package com.stockpile;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Setter;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * A small draggable overlay showing the total portfolio profit/loss, rendered only while the
 * bank interface is open.
 */
@Singleton
public class BankTotalOverlay extends OverlayPanel
{
	private final PortfolioManager manager;
	private final StockpileConfig config;

	@Setter
	private boolean bankOpen;

	@Inject
	public BankTotalOverlay(PortfolioManager manager, StockpileConfig config)
	{
		this.manager = manager;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!bankOpen || !config.bankTotalOverlay() || !manager.hasAccount())
		{
			return null;
		}

		long total = manager.getLastTotalProfit();

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Portfolio P/L")
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Total:")
			.right(Format.gp(total))
			.rightColor(total >= 0 ? config.profitColor() : config.lossColor())
			.build());

		return super.render(graphics);
	}
}
