package com.bankstonks;

import com.bankstonks.model.PortfolioRow;
import com.bankstonks.ui.BankStonksPanel;
import com.bankstonks.ui.PortfolioActions;
import com.google.inject.Provides;
import java.awt.Color;
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
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InventoryID;
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
	@Inject
	private BankTotalOverlay bankTotalOverlay;

	private BankStonksPanel panel;
	private NavigationButton navButton;

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
		overlayManager.add(bankTotalOverlay);

		refresh();
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(bankOverlay);
		overlayManager.remove(bankTotalOverlay);
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			manager.setAccount(client.getAccountHash());
			refresh();
		}
		else
		{
			bankTotalOverlay.setBankOpen(false);
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == BANK_GROUP_ID)
		{
			bankTotalOverlay.setBankOpen(true);
			refresh();
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == BANK_GROUP_ID)
		{
			bankTotalOverlay.setBankOpen(false);
		}
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		if (buyTracker.handle(event))
		{
			refresh();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		boolean bankChanged = bankTracker.handle(event);
		int id = event.getContainerId();
		if (bankChanged || id == InventoryID.INV || id == InventoryID.WORN)
		{
			refresh();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (BankStonksConfig.GROUP.equals(event.getGroup()))
		{
			refresh();
		}
	}

	@Schedule(period = 30, unit = ChronoUnit.SECONDS)
	public void scheduledRefresh()
	{
		refresh();
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

	/** Live item quantities from the inventory and worn-equipment containers (client thread). */
	private Map<Integer, Integer> liveHeldQuantities()
	{
		Map<Integer, Integer> out = new HashMap<>();
		addContainer(out, InventoryID.INV);
		addContainer(out, InventoryID.WORN);
		return out;
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
			out.merge(item.getId(), item.getQuantity(), Integer::sum);
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
	public void blockItem(String itemName)
	{
		configManager.setConfiguration(BankStonksConfig.GROUP, BankStonksConfig.KEY_BLOCK_LIST, BlockLists.add(config.blockList(), itemName));
		refresh();
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
