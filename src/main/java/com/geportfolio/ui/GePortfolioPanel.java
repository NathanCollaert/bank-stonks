package com.geportfolio.ui;

import com.geportfolio.GePortfolioConfig;
import com.geportfolio.model.PortfolioRow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Sidebar panel: a total P/L header plus one row per held, tracked item.
 */
public class GePortfolioPanel extends PluginPanel
{
	private static final Color PROFIT = new Color(76, 175, 80);
	private static final Color LOSS = new Color(244, 67, 54);

	private final ItemManager itemManager;
	private final GePortfolioConfig config;

	private final JLabel totalLabel = new JLabel("0", SwingConstants.CENTER);
	private final JPanel listPanel = new JPanel();
	private final JLabel emptyLabel = new JLabel("<html><center>No tracked items yet.<br>Buy something on the GE.</center></html>", SwingConstants.CENTER);

	public GePortfolioPanel(ItemManager itemManager, GePortfolioConfig config)
	{
		this.itemManager = itemManager;
		this.config = config;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		// Header
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JLabel title = new JLabel("Portfolio profit / loss", SwingConstants.CENTER);
		title.setFont(FontManager.getRunescapeSmallFont());
		title.setForeground(Color.LIGHT_GRAY);

		totalLabel.setFont(FontManager.getRunescapeBoldFont());
		totalLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

		header.add(title, BorderLayout.NORTH);
		header.add(totalLabel, BorderLayout.CENTER);
		add(header, BorderLayout.NORTH);

		// List
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

		emptyLabel.setForeground(Color.GRAY);
		emptyLabel.setFont(FontManager.getRunescapeSmallFont());

		add(listPanel, BorderLayout.CENTER);
	}

	/** Rebuilds the panel from the given rows. Safe to call from any thread. */
	public void update(List<PortfolioRow> rows, long total)
	{
		SwingUtilities.invokeLater(() ->
		{
			listPanel.removeAll();

			totalLabel.setText(formatGp(total));
			totalLabel.setForeground(colorFor(total));

			if (rows.isEmpty())
			{
				listPanel.add(emptyLabel);
			}
			else
			{
				for (PortfolioRow row : rows)
				{
					listPanel.add(buildRow(row));
					listPanel.add(javax.swing.Box.createVerticalStrut(4));
				}
			}

			listPanel.revalidate();
			listPanel.repaint();
		});
	}

	private JPanel buildRow(PortfolioRow row)
	{
		JPanel panel = new JPanel(new BorderLayout(6, 0));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

		// Icon
		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(32, 32));
		AsyncBufferedImage image = itemManager.getImage(row.getItemId(), row.getQuantity(), row.getQuantity() > 1);
		image.addTo(icon);
		panel.add(icon, BorderLayout.WEST);

		// Middle: name + qty @ avg
		JPanel middle = new JPanel(new GridLayout(2, 1));
		middle.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel name = new JLabel(row.getName());
		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(Color.WHITE);

		JLabel detail = new JLabel(row.getQuantity() + " @ " + formatPlain(row.getAverageBuyPrice()));
		detail.setFont(FontManager.getRunescapeSmallFont());
		detail.setForeground(Color.GRAY);

		middle.add(name);
		middle.add(detail);
		panel.add(middle, BorderLayout.CENTER);

		// Right: P/L (+ optional %)
		JPanel right = new JPanel(new GridLayout(config.showPercent() ? 2 : 1, 1));
		right.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel pl = new JLabel(formatGp(row.getProfitTotal()), SwingConstants.RIGHT);
		pl.setFont(FontManager.getRunescapeSmallFont());
		pl.setForeground(colorFor(row.getProfitTotal()));
		right.add(pl);

		if (config.showPercent())
		{
			JLabel pct = new JLabel(formatPercent(row.profitPercent()), SwingConstants.RIGHT);
			pct.setFont(FontManager.getRunescapeSmallFont());
			pct.setForeground(colorFor(row.getProfitTotal()));
			right.add(pct);
		}

		panel.add(right, BorderLayout.EAST);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
		return panel;
	}

	private static Color colorFor(long value)
	{
		if (value > 0)
		{
			return PROFIT;
		}
		if (value < 0)
		{
			return LOSS;
		}
		return Color.LIGHT_GRAY;
	}

	private static String formatPercent(double pct)
	{
		return String.format("%+.1f%%", pct);
	}

	/** Compact signed gp, e.g. +1.23M, -450K, +999. */
	static String formatGp(long value)
	{
		long abs = Math.abs(value);
		String sign = value > 0 ? "+" : (value < 0 ? "-" : "");
		if (abs >= 1_000_000_000L)
		{
			return sign + String.format("%.2fB", abs / 1_000_000_000.0);
		}
		if (abs >= 1_000_000L)
		{
			return sign + String.format("%.2fM", abs / 1_000_000.0);
		}
		if (abs >= 1_000L)
		{
			return sign + String.format("%.1fK", abs / 1_000.0);
		}
		return sign + abs;
	}

	/** Unsigned compact gp for the average-price detail line. */
	static String formatPlain(long value)
	{
		if (value >= 1_000_000L)
		{
			return String.format("%.2fM", value / 1_000_000.0);
		}
		if (value >= 1_000L)
		{
			return String.format("%.1fK", value / 1_000.0);
		}
		return Long.toString(value);
	}
}
