package com.bankstonks.ui;

import com.bankstonks.Format;
import com.bankstonks.model.PortfolioRow;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.DoubleSupplier;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

final class ItemRow extends JComponent
{
	static final int HEIGHT = 56;

	static final int TEXT_X = Theme.CARD_PAD + Theme.SPRITE_W + Theme.SPACE_XS;

	private static final int NAME_LINE_HEIGHT = 16;
	private static final int COST_LINE_HEIGHT = 12;
	private static final int BOTTOM_LINE_HEIGHT = Widgets.CHIP_H;
	private static final int PAD_RIGHT = Theme.CARD_PAD;
	private static final Shape PADLOCK = Icons.padlockShape();
	private static final int LOCK_BADGE_INSET = 2;
	private static final int LOCK_OUTLINE_OVERHANG = 1;
	private static final BasicStroke LOCK_OUTLINE_STROKE = new BasicStroke(2f * LOCK_OUTLINE_OVERHANG, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Color LOCK_OUTLINE = new Color(12, 12, 12, 235);

	private final PortfolioRow row;
	private final BufferedImage sprite;
	private final Color plColor;
	private final boolean showPercent;
	private final boolean dividerAbove;
	private final boolean collapsible;
	private final String costLine;
	private final String ageLine;
	private final String ageLineWithoutHeld;

	private DoubleSupplier expandProgress = () -> 0;
	private Runnable onToggle;
	private java.util.function.Supplier<JPopupMenu> menuFactory;
	private boolean clickableHover;

	private final Rectangle nameArea = new Rectangle();
	private final Rectangle costArea = new Rectangle();
	private final Rectangle plArea = new Rectangle();
	private final Rectangle chipArea = new Rectangle();
	private final Rectangle ageLineArea = new Rectangle();
	private final Rectangle lockArea = new Rectangle();
	private boolean nameEllipsized;

	ItemRow(PortfolioRow row, BufferedImage sprite, Color plColor, boolean showPercent, boolean dividerAbove)
	{
		this.row = row;
		this.sprite = sprite;
		this.plColor = plColor;
		this.showPercent = showPercent;
		this.dividerAbove = dividerAbove;
		this.collapsible = row.getLots() != null && row.getLots().size() > 1;
		this.costLine = row.getQuantity() == 1
			? "paid " + Format.plain(row.getAverageBuyPrice())
			: Format.exact(row.getQuantity()) + " @ " + Format.plain(row.getAverageBuyPrice());

		String age = Format.age(row.getFirstBoughtEpochMs());
		String buys = collapsible ? row.getLots().size() + " buys" : "";
		this.ageLine = join(age.isEmpty() ? "" : "held " + age, buys);
		this.ageLineWithoutHeld = join(age, buys);

		setOpaque(false);
		ToolTipManager.sharedInstance().registerComponent(this);
		if (collapsible)
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				clickableHover = collapsible;
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				clickableHover = false;
				repaint();
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showMenu(e);
				}
				else if (SwingUtilities.isLeftMouseButton(e) && collapsible && onToggle != null)
				{
					onToggle.run();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showMenu(e);
				}
			}
		};
		addMouseListener(mouse);
	}

	/**
	 * On the sprite rather than before the name, which would cost name width on every row (most of
	 * a long-term holder's items are locked). Outlined like the item sprites rather than on a
	 * backing disc, so it reads on light and dark sprites without biting a hole in the item.
	 */
	private void paintLockBadge(Graphics2D g2, int spriteY)
	{
		int size = Icons.PADLOCK_SIZE;
		int bx = Theme.CARD_PAD + Theme.SPRITE_W - size - LOCK_BADGE_INSET;
		int by = spriteY + Theme.SPRITE_H - size - LOCK_BADGE_INSET;
		Graphics2D g = (Graphics2D) g2.create();
		g.translate(bx, by);
		g.setColor(LOCK_OUTLINE);
		g.fill(LOCK_OUTLINE_STROKE.createStrokedShape(PADLOCK));
		g.setColor(Theme.LOCK);
		g.fill(PADLOCK);
		g.dispose();
		int outlinedSize = size + 2 * LOCK_OUTLINE_OVERHANG;
		lockArea.setBounds(bx - LOCK_OUTLINE_OVERHANG, by - LOCK_OUTLINE_OVERHANG, outlinedSize, outlinedSize);
	}

	private static String join(String a, String b)
	{
		return a.isEmpty() ? b : b.isEmpty() ? a : a + " · " + b;
	}

	boolean isCollapsible()
	{
		return collapsible;
	}

	void setExpandProgress(DoubleSupplier progress)
	{
		this.expandProgress = progress;
	}

	void setOnToggle(Runnable onToggle)
	{
		this.onToggle = onToggle;
	}

	void setMenu(java.util.function.Supplier<JPopupMenu> menuFactory)
	{
		this.menuFactory = menuFactory;
	}

	private void showMenu(MouseEvent e)
	{
		if (menuFactory != null)
		{
			menuFactory.get().show(this, e.getX(), e.getY());
		}
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(Theme.SPRITE_W + 2 * Theme.CARD_PAD, HEIGHT);
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, HEIGHT);
	}

	@Override
	public String getToolTipText(MouseEvent e)
	{
		if (plArea.contains(e.getPoint()) || chipArea.contains(e.getPoint()))
		{
			return "Worth " + Format.exact(row.getCurrentPrice()) + " gp each now";
		}
		if (costArea.contains(e.getPoint()))
		{
			return "Paid " + Format.exact(row.getAverageBuyPrice()) + " gp each" + (row.getQuantity() > 1 ? " on average" : "");
		}
		if (lockArea.contains(e.getPoint()))
		{
			return "Locked to one buy";
		}
		if (nameArea.contains(e.getPoint()))
		{
			if (row.isLocked())
			{
				return row.getName() + " (locked to one buy)";
			}
			return nameEllipsized ? row.getName() : null;
		}
		if (collapsible && ageLineArea.contains(e.getPoint()))
		{
			return "Click to " + (expandProgress.getAsDouble() > 0.5 ? "hide" : "show") + " each buy";
		}
		return "Right-click to lock, untrack or block";
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		paintHoverAndDivider(g2, w, h);
		paintSpriteAndLock(g2, (h - Theme.SPRITE_H) / 2);

		boolean hasAgeLine = !ageLine.isEmpty();
		int costLineHeight = hasAgeLine ? COST_LINE_HEIGHT : BOTTOM_LINE_HEIGHT;
		int textBlockHeight = NAME_LINE_HEIGHT + costLineHeight + (hasAgeLine ? BOTTOM_LINE_HEIGHT : 0);
		int nameLineTop = (h - textBlockHeight) / 2;
		int costLineTop = nameLineTop + NAME_LINE_HEIGHT;
		int bottomLineTop = hasAgeLine ? costLineTop + COST_LINE_HEIGHT : costLineTop;
		int right = w - PAD_RIGHT;

		int profitWidth = paintProfit(g2, right, nameLineTop);
		int chipLeft = paintPercentChip(g2, right, bottomLineTop);
		int bottomLineTextRight = chipLeft - Theme.SPACE_S;
		paintName(g2, right - profitWidth - Theme.SPACE_S, nameLineTop);
		paintCost(g2, hasAgeLine ? right : bottomLineTextRight, costLineTop, costLineHeight);
		if (hasAgeLine)
		{
			paintAgeLine(g2, bottomLineTextRight, bottomLineTop);
		}
		else
		{
			ageLineArea.setBounds(0, 0, 0, 0);
		}
		g2.dispose();
	}

	private void paintHoverAndDivider(Graphics2D g2, int w, int h)
	{
		if (clickableHover)
		{
			g2.setColor(Theme.HOVER_FILL);
			g2.fillRect(0, 0, w, h);
		}
		if (dividerAbove)
		{
			g2.setColor(Theme.OUTLINE);
			g2.fillRect(Theme.CARD_PAD, 0, w - 2 * Theme.CARD_PAD, 1);
		}
	}

	private void paintSpriteAndLock(Graphics2D g2, int spriteY)
	{
		if (sprite != null)
		{
			g2.drawImage(sprite, Theme.CARD_PAD, spriteY, null);
		}
		if (row.isLocked())
		{
			paintLockBadge(g2, spriteY);
		}
		else
		{
			lockArea.setBounds(0, 0, 0, 0);
		}
	}

	/** Returns the profit text's width. */
	private int paintProfit(Graphics2D g2, int right, int top)
	{
		g2.setFont(Theme.body());
		FontMetrics body = g2.getFontMetrics();
		String pl = Format.gp(row.getProfitTotal());
		int plWidth = body.stringWidth(pl);
		g2.setColor(plColor);
		g2.drawString(pl, right - plWidth, Widgets.baseline(body, top, NAME_LINE_HEIGHT));
		plArea.setBounds(right - plWidth, top, plWidth, NAME_LINE_HEIGHT);
		return plWidth;
	}

	/** Returns the chip's left edge, or {@code right} when percentages are off. */
	private int paintPercentChip(Graphics2D g2, int right, int bottomLineTop)
	{
		g2.setFont(Theme.small());
		chipArea.setBounds(0, 0, 0, 0);
		if (!showPercent)
		{
			return right;
		}
		FontMetrics small = g2.getFontMetrics();
		int baseline = Widgets.baseline(small, bottomLineTop, BOTTOM_LINE_HEIGHT);
		int chipLeft = Widgets.paintChip(g2, percentText(), right, (int) Math.round(Widgets.capCenter(small, baseline)), plColor);
		chipArea.setBounds(chipLeft, bottomLineTop, right - chipLeft, BOTTOM_LINE_HEIGHT);
		return chipLeft;
	}

	private String percentText()
	{
		return row.getProfitTotal() == 0 ? "0.0%" : String.format("%+.1f%%", row.profitPercent());
	}

	private void paintName(Graphics2D g2, int nameRight, int top)
	{
		g2.setFont(Theme.body());
		FontMetrics body = g2.getFontMetrics();
		String name = Widgets.ellipsize(row.getName(), body, nameRight - TEXT_X);
		nameEllipsized = !name.equals(row.getName());
		g2.setColor(Theme.TEXT);
		g2.drawString(name, TEXT_X, Widgets.baseline(body, top, NAME_LINE_HEIGHT));
		nameArea.setBounds(TEXT_X, top, body.stringWidth(name), NAME_LINE_HEIGHT);
	}

	private void paintCost(Graphics2D g2, int costRight, int top, int height)
	{
		g2.setFont(Theme.small());
		FontMetrics small = g2.getFontMetrics();
		String costText = Widgets.ellipsize(costLine, small, costRight - TEXT_X);
		g2.setColor(Theme.TEXT_MUTED);
		g2.drawString(costText, TEXT_X, Widgets.baseline(small, top, height));
		costArea.setBounds(TEXT_X, top, small.stringWidth(costText), height);
	}

	private void paintAgeLine(Graphics2D g2, int textRight, int top)
	{
		g2.setFont(Theme.small());
		FontMetrics small = g2.getFontMetrics();
		int baseline = Widgets.baseline(small, top, BOTTOM_LINE_HEIGHT);
		int chevronSpace = collapsible ? Theme.SPACE_XS + Icons.CHEVRON_SIZE : 0;
		String text = fitAgeLine(small, textRight - TEXT_X - chevronSpace);
		g2.setColor(Theme.TEXT_FAINT);
		g2.drawString(text, TEXT_X, baseline);
		int width = small.stringWidth(text);
		if (collapsible)
		{
			paintChevron(g2, TEXT_X + width + Theme.SPACE_XS, Widgets.capCenter(small, baseline));
			width += chevronSpace;
		}
		ageLineArea.setBounds(TEXT_X, top, width, BOTTOM_LINE_HEIGHT);
	}

	private String fitAgeLine(FontMetrics fm, int room)
	{
		return fm.stringWidth(ageLine) <= room ? ageLine : Widgets.ellipsize(ageLineWithoutHeld, fm, room);
	}

	private void paintChevron(Graphics2D g2, int x, double centerY)
	{
		Color c = clickableHover ? Theme.TEXT_MUTED : Theme.TEXT_FAINT;
		int y = (int) Math.round(centerY - Icons.CHEVRON_SIZE / 2);
		Icons.chevron(() -> 90 * expandProgress.getAsDouble(), c).paintIcon(this, g2, x, y);
	}
}
