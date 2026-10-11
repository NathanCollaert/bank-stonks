package com.bankstonks.ui;

import com.bankstonks.Format;
import com.bankstonks.model.Lot;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

final class LotRow extends JComponent
{
	static final int HEIGHT = 20;

	private final Lot lot;
	private final String date;
	private final Supplier<JPopupMenu> menuFactory;
	private final Rectangle priceArea = new Rectangle();

	LotRow(Lot lot, String date, Supplier<JPopupMenu> menuFactory)
	{
		this.lot = lot;
		this.date = date;
		this.menuFactory = menuFactory;
		setOpaque(false);
		ToolTipManager.sharedInstance().registerComponent(this);
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				popup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				popup(e);
			}

			private void popup(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					menuFactory.get().show(LotRow.this, e.getX(), e.getY());
				}
			}
		});
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(ItemRow.TEXT_X + Theme.CARD_PAD, HEIGHT);
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, HEIGHT);
	}

	@Override
	public String getToolTipText(MouseEvent e)
	{
		if (priceArea.contains(e.getPoint()))
		{
			return "Paid " + Format.exact(lot.unitPrice()) + " gp each";
		}
		return "Right-click to lock or remove this buy";
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		g2.setFont(Theme.small());
		FontMetrics fm = g2.getFontMetrics();
		int baseline = Widgets.baseline(fm, 0, h);
		int right = w - Theme.CARD_PAD;

		int dateWidth = fm.stringWidth(date);
		g2.setColor(Theme.TEXT_FAINT);
		g2.drawString(date, right - dateWidth, baseline);

		String price = Widgets.ellipsize(Format.exact(lot.getQuantity()) + " @ " + Format.plain(lot.unitPrice()), fm,
			right - dateWidth - Theme.SPACE_S - ItemRow.TEXT_X);
		g2.setColor(Theme.TEXT_MUTED);
		g2.drawString(price, ItemRow.TEXT_X, baseline);
		priceArea.setBounds(ItemRow.TEXT_X, 0, fm.stringWidth(price), h);
		g2.dispose();
	}
}
