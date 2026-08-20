package com.bankstonks;

import com.bankstonks.model.PortfolioRow;
import com.bankstonks.ui.BankStonksPanel;
import com.bankstonks.ui.PortfolioActions;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
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
	/** The bank interface widget group id. */
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
	 * Set by any event that could change the panel; the actual rebuild happens at most once per
	 * game tick and only while the sidebar is open. This coalesces the flood of inventory/bank
	 * changes during normal play into a single refresh, and skips work entirely when the panel is
	 * closed. (The bank overlay reads live data on render and is unaffected.)
	 */
	private volatile boolean refreshPending;

	/**
	 * When true the next rebuild is unconditional (something other than inventory changed: an edit,
	 * a config change, the price timer, opening the panel). Inventory changes leave this false, so
	 * they only rebuild when the tracked items' held quantities actually changed.
	 */
	private volatile boolean forceRebuild;

	/** The tracked items' held quantities at the last rebuild, to detect real changes. */
	private Map<Integer, Integer> lastHeldSignature = new HashMap<>();

	@Provides
	BankStonksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankStonksConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		panel = new BankStonksPanel(itemManager, config, this);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/nav_button.png");
		navButton = NavigationButton.builder()
			.tooltip("Bank Stonks")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(bankOverlay);

		// Rebuild once whenever the sidebar becomes visible (e.g. the user opens the tab), so it
		// shows current data without us having to rebuild it while it was hidden.
		panel.addHierarchyListener(e ->
		{
			if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing())
			{
				requestForceRefresh();
			}
		});

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

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGING_IN || state == GameState.HOPPING)
		{
			// The client is about to replay all existing GE offers; don't re-record them.
			buyTracker.setLoginSync(true);
		}
		if (state == GameState.LOGGED_IN)
		{
			// Load the account immediately (the bank overlay needs it), but leave the panel
			// rebuild to the coalesced tick handler.
			manager.setAccount(client.getAccountHash());
			requestForceRefresh();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// The login/hop offer replay all fires before the first tick, so the sync window is over.
		buyTracker.setLoginSync(false);

		// Coalesce all the refresh requests since the last tick, and only bother while the sidebar
		// is open. An inventory-only request rebuilds only if a tracked item's held quantity has
		// actually changed; edits / config / the price timer force it regardless.
		if (refreshPending && panel != null && panel.isShowing())
		{
			refreshPending = false;
			Map<Integer, Integer> signature = manager.trackedHeldSignature(liveHeldQuantities());
			if (forceRebuild || !signature.equals(lastHeldSignature))
			{
				forceRebuild = false;
				lastHeldSignature = signature;
				refresh();
			}
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		// The bank opening can change what counts as held; queue a refresh.
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
			requestRefresh();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// Only react to actual setting changes. Our own portfolio saves land in the same config
		// group under the data_ key; those must not force a rebuild or every bank change and GE buy
		// (which call save()) would rebuild the whole panel.
		if (BankStonksConfig.GROUP.equals(event.getGroup()) && !event.getKey().startsWith(PortfolioManager.DATA_KEY_PREFIX))
		{
			requestForceRefresh();
		}
	}

	@Schedule(period = 5, unit = ChronoUnit.MINUTES)
	public void scheduledRefresh()
	{
		// Picks up price changes while the panel sits open with nothing else happening.
		requestForceRefresh();
	}

	/**
	 * Marks the panel as needing a rebuild from an inventory change; the tick only rebuilds if a
	 * tracked item's held quantity actually changed.
	 */
	private void requestRefresh()
	{
		refreshPending = true;
	}

	/** Requests an unconditional rebuild (used for edits, config, prices, opening the panel). */
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
				panel.update(Collections.emptyList(), 0L);
				return;
			}

			List<PortfolioRow> rows = manager.buildRows(itemManager, config, liveHeldQuantities());
			panel.update(rows, manager.getLastTotalProfit());
		});
	}

	/**
	 * Live item quantities from the inventory, worn equipment and the Dizana's quiver (client
	 * thread). The quiver holds equipped ammunition in its own container, separate from the worn
	 * slots, so it must be counted too or quivered ammo would drop out of the list.
	 */
	private Map<Integer, Integer> liveHeldQuantities()
	{
		Map<Integer, Integer> out = new HashMap<>();
		addContainer(out, InventoryID.INV);
		addContainer(out, InventoryID.WORN);
		addQuiver(out);
		return out;
	}

	/**
	 * Adds the ammunition stored in a Dizana's quiver. The quiver keeps its ammo in varplayers
	 * rather than an item container, so it is not covered by {@link #addContainer}.
	 */
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
			// Canonicalize so a noted (or placeholder) item counts as the real item it was bought as.
			out.merge(itemManager.canonicalize(item.getId()), item.getQuantity(), Integer::sum);
		}
	}

	// ---- PortfolioActions (invoked from the panel on the EDT) ----------------

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
				panel.setStatus("No item found for \"" + query + "\".", Color.RED);
				return;
			}

			String name = itemManager.getItemComposition(itemId).getName();
			// Adding an item you want tracked implicitly unblocks it.
			configManager.setConfiguration(BankStonksConfig.GROUP, BankStonksConfig.KEY_BLOCK_LIST, BlockLists.remove(config.blockList(), name));
			manager.recordBuy(itemId, quantity, priceEach * quantity, heldSinceEpochMs);
			manager.save();
			panel.setStatus("Added " + quantity + " x " + name + ".", Color.LIGHT_GRAY);
			panel.clearManualEntry();
			refresh();
		});
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

	/** Resolves a user-typed name to an item id, preferring an exact (case-insensitive) match. */
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
