package com.bankstonks.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

final class Theme
{
	static final Color CANVAS = ColorScheme.DARKER_GRAY_COLOR;
	static final Color SURFACE = new Color(46, 46, 46);
	static final Color SURFACE_HOVER = new Color(55, 55, 55);
	static final Color INSET_SURFACE = new Color(38, 38, 38);
	static final Color OUTLINE = new Color(60, 60, 60);

	static final Color TEXT = Color.WHITE;
	static final Color TEXT_MUTED = ColorScheme.LIGHT_GRAY_COLOR;
	static final Color TEXT_FAINT = new Color(120, 120, 120);

	static final Color ACCENT = ColorScheme.BRAND_ORANGE;
	static final Color ACCENT_HOVER = new Color(240, 158, 30);
	static final Color TEXT_ON_ACCENT = new Color(28, 28, 28);
	static final Color LOCK = new Color(230, 185, 60);

	// Translucent, so hover and press work on any surface.
	static final Color HOVER_FILL = new Color(255, 255, 255, 20);
	static final Color PRESS_FILL = new Color(255, 255, 255, 36);

	// Every gap, padding and margin comes from this 4px scale, and padding inside a box is never
	// larger than the space around it.
	static final int SPACE_XS = 4;
	static final int SPACE_S = 8;
	static final int SPACE_M = 12;
	static final int SPACE_L = 16;
	static final int SPACE_XL = 24;

	static final int SIDE = SPACE_S;
	static final int GAP = SPACE_S;
	static final int CONTENT_COLUMN_X = SPACE_L;
	static final int CARD_PAD = CONTENT_COLUMN_X - SIDE;

	static final int CONTROL_SIZE = 24;
	static final int CARD_ARC = 10;
	// Takes the list's right-hand margin while it shows.
	static final int SCROLLBAR_WIDTH = SIDE;

	static final int SPRITE_W = 36;
	static final int SPRITE_H = 32;

	static Font title()
	{
		return FontManager.getRunescapeBoldFont();
	}

	/** Off the font's pixel grid: paint it with {@link #smoothText} or its strokes come out uneven. */
	static Font hero()
	{
		return HERO;
	}

	private static final Font HERO = FontManager.getRunescapeBoldFont().deriveFont(24f);

	static Font body()
	{
		return FontManager.getRunescapeFont();
	}

	static Font small()
	{
		return FontManager.getRunescapeSmallFont();
	}

	/** Antialiased copy of {@code g}; dispose it when done. */
	static Graphics2D smooth(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		return g2;
	}

	/** For fonts drawn off their pixel grid, where whole-pixel advances space the digits unevenly. */
	static void smoothText(Graphics2D g2)
	{
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
	}

	static Color withAlpha(Color c, int alpha)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	private Theme()
	{
	}
}
