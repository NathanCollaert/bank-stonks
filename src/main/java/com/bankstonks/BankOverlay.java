package com.bankstonks;

import com.bankstonks.model.LotValuation;
import com.bankstonks.model.TrackedItem;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;

/**
 * Shows a tooltip when hovering a tracked item in the bank: how many are in your portfolio,
 * the average price you bought them for, and the current profit/loss on that amount.
 */
@Singleton
public class BankOverlay extends WidgetItemOverlay
{
	private final Client client;
	private final PortfolioManager manager;
	private final ItemManager itemManager;
	private final TooltipManager tooltipManager;
	private final BankStonksConfig config;

	@Inject
	public BankOverlay(Client client, PortfolioManager manager, ItemManager itemManager,
		TooltipManager tooltipManager, BankStonksConfig config)
	{
		this.client = client;
		this.manager = manager;
		this.itemManager = itemManager;
		this.tooltipManager = tooltipManager;
		this.config = config;
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!config.bankOverlay() || !manager.hasAccount())
		{
			return;
		}

		// Resolve to the tracked (bought) item, matching charged/banked items back to the
		// uncharged item they were bought as.
		int boughtId = manager.boughtIdForBankItem(itemId);
		if (boughtId < 0)
		{
			return;
		}

		TrackedItem tracked = manager.getTracked(boughtId);
		if (tracked == null || tracked.totalBought() <= 0)
		{
			return;
		}

		if (PortfolioManager.isBlocked(config, itemManager.getItemComposition(boughtId).getName()))
		{
			return;
		}

		Rectangle bounds = widgetItem.getCanvasBounds();
		if (bounds == null)
		{
			return;
		}
		Point mouse = client.getMouseCanvasPosition();
		if (mouse == null || !bounds.contains(mouse.getX(), mouse.getY()))
		{
			return;
		}

		int quantity = Math.min(tracked.totalBought(), widgetItem.getQuantity());
		if (quantity <= 0)
		{
			return;
		}

		LotValuation valuation = tracked.value(quantity);
		long avg = valuation.getAveragePrice();
		int rawCurrent = itemManager.getItemPrice(boughtId);
		long current = config.applyGeTax() ? PortfolioManager.netAfterTax(rawCurrent) : rawCurrent;
		long profit = (current - avg) * quantity;

		java.awt.Color plColor = profit >= 0 ? config.profitColor() : config.lossColor();

		String heldAge = Format.age(valuation.getHeldSinceEpochMs());
		String heldLine = heldAge.isEmpty() ? "" : "Held: " + heldAge + "</br>";

		String text = quantity + " @ " + Format.plain(avg) + " ea</br>"
			+ heldLine
			+ "P/L: " + ColorUtil.wrapWithColorTag(Format.gp(profit), plColor);

		tooltipManager.add(new Tooltip(text));
	}
}
