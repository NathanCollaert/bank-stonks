package com.geportfolio;

import com.geportfolio.model.PortfolioRow;
import com.geportfolio.ui.GePortfolioPanel;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.ItemContainerChanged;
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
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "GE Portfolio Tracker",
	description = "Tracks GE buys and shows live profit/loss vs the wiki price for what you hold",
	tags = {"grand", "exchange", "ge", "profit", "loss", "portfolio", "flip", "price", "bank"}
)
public class GePortfolioPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ItemManager itemManager;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private GePortfolioConfig config;
	@Inject
	private PortfolioManager manager;
	@Inject
	private GrandExchangeBuyTracker buyTracker;
	@Inject
	private BankTracker bankTracker;

	private GePortfolioPanel panel;
	private NavigationButton navButton;

	@Provides
	GePortfolioConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GePortfolioConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new GePortfolioPanel(itemManager, config);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/nav_button.png");
		navButton = NavigationButton.builder()
			.tooltip("GE Portfolio")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		refresh();
	}

	@Override
	protected void shutDown()
	{
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
		if (bankTracker.handle(event))
		{
			refresh();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (GePortfolioConfig.GROUP.equals(event.getGroup()))
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

			List<PortfolioRow> rows = manager.buildRows(itemManager, config.hideEmpty());
			long total = 0L;
			for (PortfolioRow row : rows)
			{
				total += row.getProfitTotal();
			}
			panel.update(rows, total);
		});
	}
}
