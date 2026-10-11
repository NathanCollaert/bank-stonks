package com.bankstonks.ui;

import com.bankstonks.Format;
import com.bankstonks.model.PortfolioRow;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.ToolTipManager;

final class SummaryCard extends Widgets.Card
{
	private static final int OVERLINE_HEIGHT = 12;
	private static final int TOTAL_HEIGHT = 24;
	private static final int HEIGHT = 2 * Theme.CARD_PAD + OVERLINE_HEIGHT + Theme.SPACE_XS + TOTAL_HEIGHT;

	private long total;
	private long invested;
	private int items;
	private Color color = Theme.TEXT_MUTED;
	private boolean showPercent = true;

	private final Rectangle totalArea = new Rectangle();
	private final Rectangle pctArea = new Rectangle();

	SummaryCard()
	{
		super(null);
		ToolTipManager.sharedInstance().registerComponent(this);
	}

	void set(List<PortfolioRow> rows, long total, Color color, boolean showPercent)
	{
		this.total = total;
		this.invested = totalPaid(rows);
		this.items = rows.size();
		this.color = color;
		this.showPercent = showPercent;
		repaint();
	}

	private static long totalPaid(List<PortfolioRow> rows)
	{
		long sum = 0;
		for (PortfolioRow row : rows)
		{
			sum += row.getAverageBuyPrice() * row.getQuantity();
		}
		return sum;
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(0, HEIGHT);
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, HEIGHT);
	}

	@Override
	public String getToolTipText(MouseEvent e)
	{
		if (totalArea.contains(e.getPoint()))
		{
			return (total > 0 ? "+" : "") + Format.exact(total) + " gp";
		}
		if (pctArea.contains(e.getPoint()))
		{
			return "Of " + Format.exact(invested) + " gp paid; worth " + Format.exact(invested + total) + " gp now";
		}
		return null;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2 = Theme.smooth(g);
		int left = Theme.CARD_PAD;
		int right = getWidth() - Theme.CARD_PAD;
		int overlineTop = Theme.CARD_PAD;
		int totalTop = overlineTop + OVERLINE_HEIGHT + Theme.SPACE_XS;
		paintOverline(g2, left, right, overlineTop);
		paintTotalAndPercent(g2, left, right, totalTop);
		g2.dispose();
	}

	private void paintOverline(Graphics2D g2, int left, int right, int top)
	{
		g2.setFont(Theme.small());
		FontMetrics small = g2.getFontMetrics();
		g2.setColor(Theme.TEXT_FAINT);
		g2.drawString("SINCE YOU BOUGHT", left, Widgets.baseline(small, top, OVERLINE_HEIGHT));
		String count = items == 1 ? "1 item" : items + " items";
		g2.drawString(count, right - small.stringWidth(count), Widgets.baseline(small, top, OVERLINE_HEIGHT));
	}

	private void paintTotalAndPercent(Graphics2D g2, int left, int right, int top)
	{
		Graphics2D hero = (Graphics2D) g2.create();
		hero.setFont(Theme.hero());
		Theme.smoothText(hero);
		FontMetrics heroMetrics = hero.getFontMetrics();
		String totalText = Format.gp(total);
		hero.setColor(color);
		int totalBaseline = Widgets.baseline(heroMetrics, top, TOTAL_HEIGHT);
		hero.drawString(totalText, left, totalBaseline);
		hero.dispose();
		totalArea.setBounds(left, top, heroMetrics.stringWidth(totalText), TOTAL_HEIGHT);
		pctArea.setBounds(0, 0, 0, 0);
		if (showPercent && invested > 0)
		{
			g2.setFont(Theme.small());
			String pct = total == 0 ? "0.0%" : String.format("%+.1f%%", total * 100.0 / invested);
			int chipLeft = Widgets.paintChip(g2, pct, right, (int) Math.round(Widgets.capCenter(heroMetrics, totalBaseline)), color);
			pctArea.setBounds(chipLeft, top, right - chipLeft, TOTAL_HEIGHT);
		}
	}
}
