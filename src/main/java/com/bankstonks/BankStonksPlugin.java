package com.bankstonks;

import com.bankstonks.model.PortfolioRow;
import com.bankstonks.ui.BankStonksPanel;
import com.bankstonks.ui.PortfolioActions;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.item.ItemPrice;

@Slf4j
@PluginDescriptor(
	name = "Bank Stonks",
	description = "Tracks GE buys and shows live profit/loss vs the wiki price for what you hold",
	tags = {"grand", "exchange", "ge", "profit", "loss", "portfolio", "stonks", "flip", "price", "bank"}
)
public class BankStonksPlugin extends Plugin implements PortfolioActions
{
	private static final int BANK_GROUP_ID = 12;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ItemManager itemManager;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private BankStonksConfig config;
	@Inject
	private ConfigManager configManager;
	@Inject
	private PortfolioManager manager;
	@Inject
	private GrandExchangeBuyTracker buyTracker;
	@Inject
	private BankTracker bankTracker;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private BankOverlay bankOverlay;

	private BankStonksPanel panel;
	private NavigationButton navButton;

	/**
	 * Rebuilds wait for the next game tick and an open sidebar, so the flood of container changes
	 * during play costs one rebuild, or none while the panel is closed.
	 */
	private volatile boolean refreshPending;

	private volatile boolean forceRebuild;

	private Map<Integer, Integer> heldSignatureAtLastRebuild = new HashMap<>();

	@Provides
	BankStonksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankStonksConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		panel = new BankStonksPanel(itemManager::getImage, config, this);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/nav_button.png");
		navButton = NavigationButton.builder()
			.tooltip("Bank Stonks")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(bankOverlay);

		panel.addHierarchyListener(this::refreshWhenShown);

		requestForceRefresh();
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(bankOverlay);
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
	}

	private void refreshWhenShown(HierarchyEvent event)
	{
		boolean showingChanged = (event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0;
		if (showingChanged && panel.isShowing())
		{
			requestForceRefresh();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		boolean offersAboutToReplay = state == GameState.LOGGING_IN || state == GameState.HOPPING;
		if (offersAboutToReplay)
		{
			buyTracker.setLoginSync(true);
		}
		if (state == GameState.LOGGED_IN)
		{
			// Now rather than at the next rebuild: the bank overlay needs the account loaded.
			manager.setAccount(client.getAccountHash());
			requestForceRefresh();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// The login/hop offer replay all arrives before the first tick.
		buyTracker.setLoginSync(false);

		if (refreshPending && panel != null && panel.isShowing())
		{
			refreshPending = false;
			rebuildIfForcedOrHoldingsChanged();
		}
	}

	private void rebuildIfForcedOrHoldingsChanged()
	{
		Map<Integer, Integer> signature = manager.trackedHeldSignature(liveHeldQuantities());
		if (forceRebuild || !signature.equals(heldSignatureAtLastRebuild))
		{
			forceRebuild = false;
			heldSignatureAtLastRebuild = signature;
			refresh();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == BANK_GROUP_ID)
		{
			requestForceRefresh();
		}
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		if (buyTracker.handle(event))
		{
			requestForceRefresh();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		boolean bankChanged = bankTracker.handle(event);
		int id = event.getContainerId();
		if (bankChanged || id == InventoryID.INV || id == InventoryID.WORN)
		{
			requestRefreshIfHoldingsChanged();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (isSettingChange(event))
		{
			requestForceRefresh();
		}
	}

	private static boolean isSettingChange(ConfigChanged event)
	{
		return BankStonksConfig.GROUP.equals(event.getGroup()) && !isPortfolioSave(event);
	}

	private static boolean isPortfolioSave(ConfigChanged event)
	{
		return event.getKey().startsWith(PortfolioManager.DATA_KEY_PREFIX);
	}

	@Schedule(period = 5, unit = ChronoUnit.MINUTES)
	public void refreshPrices()
	{
		requestForceRefresh();
	}

	private void requestRefreshIfHoldingsChanged()
	{
		refreshPending = true;
	}

	private void requestForceRefresh()
	{
		forceRebuild = true;
		refreshPending = true;
	}

	private void refresh()
	{
		if (panel == null)
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				manager.setAccount(client.getAccountHash());
			}

			if (!manager.hasAccount())
			{
				panel.showLoggedOut();
				return;
			}

			List<PortfolioRow> rows = manager.buildRows(itemManager, config, liveHeldQuantities());
			panel.update(rows, manager.getLastTotalProfit());
		});
	}

	/** Client thread only. */
	private Map<Integer, Integer> liveHeldQuantities()
	{
		Map<Integer, Integer> out = new HashMap<>();
		addContainer(out, InventoryID.INV);
		addContainer(out, InventoryID.WORN);
		addQuiver(out);
		return out;
	}

	/** The quiver keeps its ammo in varplayers, not in an item container. */
	private void addQuiver(Map<Integer, Integer> out)
	{
		int ammoId = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO);
		int ammoQty = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT);
		if (ammoId > 0 && ammoQty > 0)
		{
			out.merge(itemManager.canonicalize(ammoId), ammoQty, Integer::sum);
		}
	}

	private void addContainer(Map<Integer, Integer> out, int containerId)
	{
		ItemContainer container = client.getItemContainer(containerId);
		if (container == null)
		{
			return;
		}
		for (Item item : container.getItems())
		{
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			int unnotedId = itemManager.canonicalize(item.getId());
			out.merge(unnotedId, item.getQuantity(), Integer::sum);
		}
	}

	@Override
	public void addManual(String query, int quantity, long priceEach, long heldSinceEpochMs)
	{
		clientThread.invokeLater(() ->
		{
			if (!manager.hasAccount())
			{
				panel.setStatus("Log in first.", Color.GRAY);
				return;
			}

			int itemId = resolveItemId(query);
			if (itemId <= 0)
			{
				panel.setStatus("No item found for \"" + query + "\".", config.lossColor());
				return;
			}

			String name = itemManager.getItemComposition(itemId).getName();
			unblock(name);
			manager.recordBuy(itemId, quantity, priceEach * quantity, heldSinceEpochMs);
			manager.save();
			panel.setStatus("Added " + quantity + " x " + name + ".", Color.LIGHT_GRAY);
			panel.clearManualEntry();
			refresh();
		});
	}

	private void unblock(String itemName)
	{
		configManager.setConfiguration(BankStonksConfig.GROUP, BankStonksConfig.KEY_BLOCK_LIST, BlockLists.remove(config.blockList(), itemName));
	}

	@Override
	public void untrackItem(int itemId)
	{
		clientThread.invokeLater(() ->
		{
			manager.removeItem(itemId);
			manager.save();
			refresh();
		});
	}

	@Override
	public void untrackLot(int itemId, int quantity, long spent, long epochMs)
	{
		clientThread.invokeLater(() ->
		{
			manager.removeLot(itemId, quantity, spent, epochMs);
			manager.save();
			refresh();
		});
	}

	@Override
	public void blockItem(String itemName)
	{
		configManager.setConfiguration(BankStonksConfig.GROUP, BankStonksConfig.KEY_BLOCK_LIST, BlockLists.add(config.blockList(), itemName));
		refresh();
	}

	@Override
	public void lockItem(int itemId)
	{
		clientThread.invokeLater(() ->
		{
			manager.lockItem(itemId);
			manager.save();
			refresh();
		});
	}

	@Override
	public void lockLot(int itemId, int quantity, long spent, long epochMs)
	{
		clientThread.invokeLater(() ->
		{
			manager.lockLot(itemId, quantity, spent, epochMs);
			manager.save();
			refresh();
		});
	}

	@Override
	public void unlockItem(int itemId)
	{
		clientThread.invokeLater(() ->
		{
			manager.unlockItem(itemId);
			manager.save();
			refresh();
		});
	}

	@Override
	public void setSortOrder(SortOrder order)
	{
		configManager.setConfiguration(BankStonksConfig.GROUP, BankStonksConfig.KEY_SORT_ORDER, order);
	}

	private int resolveItemId(String query)
	{
		List<ItemPrice> results = itemManager.search(query);
		if (results == null || results.isEmpty())
		{
			return -1;
		}
		for (ItemPrice result : results)
		{
			if (result.getName().equalsIgnoreCase(query))
			{
				return result.getId();
			}
		}
		return results.get(0).getId();
	}
}
