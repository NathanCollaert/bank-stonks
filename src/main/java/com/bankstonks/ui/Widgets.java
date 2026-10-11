package com.bankstonks.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/** Painted by hand, so the controls match each other and ignore the look and feel. */
final class Widgets
{
	static final int CHIP_H = 14;
	private static final int CHIP_PAD = 5;
	private static final int CHIP_TINT_ALPHA = 38;
	private static final int ICON_BUTTON_ARC = 8;
	private static final String CAPITALS_AND_DIGITS = "0123456789H";

	/** Not FlowLayout: it pads both ends too, which knocks the edges off the alignment column. */
	static JPanel hbox(int gap, java.awt.Component... items)
	{
		JPanel row = new JPanel();
		row.setOpaque(false);
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		for (int i = 0; i < items.length; i++)
		{
			if (i > 0)
			{
				row.add(Box.createHorizontalStrut(gap));
			}
			if (items[i] instanceof JComponent)
			{
				((JComponent) items[i]).setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
			}
			row.add(items[i]);
		}
		return row;
	}

	static JButton iconButton(Icon icon, String tooltip, Runnable action)
	{
		JButton b = new JButton(icon)
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2 = Theme.smooth(g);
				if (getModel().isPressed())
				{
					g2.setColor(Theme.PRESS_FILL);
					g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), ICON_BUTTON_ARC, ICON_BUTTON_ARC));
				}
				else if (getModel().isRollover() || isSelected())
				{
					g2.setColor(Theme.HOVER_FILL);
					g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), ICON_BUTTON_ARC, ICON_BUTTON_ARC));
				}
				Icon i = getIcon();
				i.paintIcon(this, g2, (getWidth() - i.getIconWidth()) / 2, (getHeight() - i.getIconHeight()) / 2);
				g2.dispose();
			}
		};
		plain(b);
		b.setToolTipText(tooltip);
		fixSize(b, new Dimension(Theme.CONTROL_SIZE, Theme.CONTROL_SIZE));
		b.addActionListener(e -> action.run());
		return b;
	}

	static JButton primaryButton(String text, Runnable action)
	{
		JButton b = new JButton(text)
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2 = Theme.smooth(g);
				g2.setColor(!isEnabled() ? Theme.OUTLINE
					: getModel().isPressed() ? Theme.ACCENT
					: getModel().isRollover() ? Theme.ACCENT_HOVER : Theme.ACCENT);
				g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
				g2.setFont(getFont());
				g2.setColor(isEnabled() ? Theme.TEXT_ON_ACCENT : Theme.TEXT_FAINT);
				FontMetrics fm = g2.getFontMetrics();
				g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, baseline(fm, 0, getHeight()));
				g2.dispose();
			}
		};
		plain(b);
		b.setFont(Theme.small());
		b.setPreferredSize(new Dimension(b.getFontMetrics(b.getFont()).stringWidth(text) + 2 * Theme.SPACE_M, Theme.CONTROL_SIZE));
		b.addActionListener(e -> action.run());
		return b;
	}

	/** Keeps FlatLaf's own border, so {@code JComponent.outline = "error"} still works. */
	static JTextField textField(String placeholder, String tooltip, Color background)
	{
		JTextField f = new JTextField();
		f.putClientProperty("JTextField.placeholderText", placeholder);
		f.putClientProperty("FlatLaf.style", "arc: 8; focusWidth: 0; innerFocusWidth: 0; margin: 0,6,0,6;"
			+ " placeholderForeground: " + hex(Theme.TEXT_FAINT));
		f.setBackground(background);
		f.setForeground(Theme.TEXT);
		f.setCaretColor(Theme.TEXT);
		f.setFont(Theme.small());
		f.setToolTipText(tooltip);
		f.setPreferredSize(new Dimension(0, Theme.CONTROL_SIZE));
		return f;
	}

	private static String hex(Color c)
	{
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}

	static JLabel sectionLabel(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(Theme.small());
		l.setForeground(Theme.TEXT_FAINT);
		return l;
	}

	/**
	 * Masks its corners after the children paint, so a hover fill in the first or last row follows
	 * the rounded edge.
	 */
	static class Card extends JPanel
	{
		Card(LayoutManager layout)
		{
			super(layout);
			setBackground(Theme.SURFACE);
		}

		/** A child's repaint must go through this paint, or it draws over the masked corners. */
		@Override
		public boolean isPaintingOrigin()
		{
			return true;
		}

		@Override
		public void paint(Graphics g)
		{
			super.paint(g);
			Graphics2D g2 = Theme.smooth(g);
			maskCorners(g2);
			paintOutline(g2);
			g2.dispose();
		}

		private void maskCorners(Graphics2D g2)
		{
			int w = getWidth();
			int h = getHeight();
			Shape round = new RoundRectangle2D.Double(0, 0, w, h, Theme.CARD_ARC, Theme.CARD_ARC);
			Area corners = new Area(new Rectangle2D.Double(0, 0, w, h));
			corners.subtract(new Area(round));
			g2.setColor(nearestOpaqueBackground());
			g2.fill(corners);
		}

		private void paintOutline(Graphics2D g2)
		{
			g2.setColor(Theme.OUTLINE);
			g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, Theme.CARD_ARC, Theme.CARD_ARC));
		}

		private Color nearestOpaqueBackground()
		{
			for (java.awt.Container p = getParent(); p != null; p = p.getParent())
			{
				if (p.isOpaque())
				{
					return p.getBackground();
				}
			}
			return Theme.CANVAS;
		}
	}

	/** Right-aligned at {@code right}; returns the chip's left edge. */
	static int paintChip(Graphics2D g2, String text, int right, int centerY, Color color)
	{
		FontMetrics fm = g2.getFontMetrics();
		int w = chipWidth(fm, text);
		int x = right - w;
		int y = centerY - CHIP_H / 2;
		g2.setColor(Theme.withAlpha(color, CHIP_TINT_ALPHA));
		g2.fill(new RoundRectangle2D.Double(x, y, w, CHIP_H, CHIP_H, CHIP_H));
		g2.setColor(color);
		g2.drawString(text, x + CHIP_PAD, baseline(fm, y, CHIP_H));
		return x;
	}

	static int chipWidth(FontMetrics fm, String text)
	{
		return fm.stringWidth(text) + 2 * CHIP_PAD;
	}

	/**
	 * Centres the capitals and digits in the box. Measures the real glyph bounds: the RuneScape
	 * fonts report an ascent that doesn't match their glyphs.
	 */
	static int baseline(FontMetrics fm, int top, int height)
	{
		Rectangle2D cap = capBounds(fm);
		return top + (int) Math.round((height - cap.getHeight()) / 2 - cap.getY());
	}

	static double capCenter(FontMetrics fm, int baseline)
	{
		Rectangle2D cap = capBounds(fm);
		return baseline + cap.getY() + cap.getHeight() / 2;
	}

	private static final java.util.Map<java.awt.Font, Rectangle2D> CAP_BOUNDS = new java.util.HashMap<>();

	/** Relative to the baseline, so y is negative. */
	private static Rectangle2D capBounds(FontMetrics fm)
	{
		return CAP_BOUNDS.computeIfAbsent(fm.getFont(),
			f -> f.createGlyphVector(fm.getFontRenderContext(), CAPITALS_AND_DIGITS).getVisualBounds());
	}

	static String ellipsize(String text, FontMetrics fm, int width)
	{
		if (fm.stringWidth(text) <= width)
		{
			return text;
		}
		String dots = "…";
		int end = text.length();
		while (end > 0 && fm.stringWidth(text.substring(0, end) + dots) > width)
		{
			end--;
		}
		return text.substring(0, end).trim() + dots;
	}

	static void fixSize(JComponent c, Dimension size)
	{
		c.setPreferredSize(size);
		c.setMinimumSize(size);
		c.setMaximumSize(size);
	}

	static void plain(JButton b)
	{
		b.setContentAreaFilled(false);
		b.setBorderPainted(false);
		b.setFocusPainted(false);
		b.setOpaque(false);
		b.setRolloverEnabled(true);
		b.setBorder(new EmptyBorder(0, 0, 0, 0));
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	private Widgets()
	{
	}
}
