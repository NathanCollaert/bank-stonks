package com.bankstonks.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.BiConsumer;
import java.util.function.DoubleSupplier;
import javax.swing.AbstractButton;
import javax.swing.Icon;

/** Drawn in code, so they stay crisp and need no resources. */
final class Icons
{
	static final int CHEVRON_SIZE = 8;
	static final int PADLOCK_SIZE = 10;

	private static final int ICON_SIZE = 16;
	private static final BasicStroke STROKE = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	/** A plus at 0 degrees, a cross at 45. */
	static Icon plus(DoubleSupplier rotationDegrees)
	{
		return new Vector(ICON_SIZE, null, (g, c) ->
		{
			g.rotate(Math.toRadians(rotationDegrees.getAsDouble()), 8, 8);
			g.draw(new Line2D.Double(8, 3.5, 8, 12.5));
			g.draw(new Line2D.Double(3.5, 8, 12.5, 8));
		});
	}

	static Icon search(Color color)
	{
		return new Vector(ICON_SIZE, color, (g, c) ->
		{
			g.draw(new Ellipse2D.Double(3, 3, 7.5, 7.5));
			g.draw(new Line2D.Double(9.4, 9.4, 13, 13));
		});
	}

	static Icon padLeft(Icon icon, int padding)
	{
		return new Icon()
		{
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y)
			{
				icon.paintIcon(c, g, x + padding, y);
			}

			@Override
			public int getIconWidth()
			{
				return icon.getIconWidth() + padding;
			}

			@Override
			public int getIconHeight()
			{
				return icon.getIconHeight();
			}
		};
	}

	static Icon sort()
	{
		return new Vector(ICON_SIZE, null, (g, c) ->
		{
			g.draw(new Line2D.Double(3, 4.5, 13, 4.5));
			g.draw(new Line2D.Double(3, 8, 10, 8));
			g.draw(new Line2D.Double(3, 11.5, 7, 11.5));
		});
	}

	/** Points right at 0 degrees, down at 90. */
	static Icon chevron(DoubleSupplier rotationDegrees, Color color)
	{
		return new Vector(CHEVRON_SIZE, color, (g, c) ->
		{
			g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.rotate(Math.toRadians(rotationDegrees.getAsDouble()), 4, 4);
			g.draw(path(2.75, 1.5, 5.25, 4, 2.75, 6.5));
		});
	}

	/** {@link #PADLOCK_SIZE} square, as one shape so it can be outlined. */
	static Area padlockShape()
	{
		BasicStroke stroke = new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		Area lock = new Area(stroke.createStrokedShape(new Arc2D.Double(2.4, 0.8, 5.2, 6, 0, 180, Arc2D.OPEN)));
		lock.add(new Area(stroke.createStrokedShape(new Line2D.Double(2.4, 3.8, 2.4, 4.6))));
		lock.add(new Area(stroke.createStrokedShape(new Line2D.Double(7.6, 3.8, 7.6, 4.6))));
		lock.add(new Area(new RoundRectangle2D.Double(1, 4.4, 8, 5.4, 2, 2)));
		lock.subtract(new Area(new Ellipse2D.Double(4.1, 6.1, 1.8, 1.8)));
		return lock;
	}

	static Icon lock()
	{
		return new Vector(ICON_SIZE, null, (g, c) ->
		{
			g.draw(new RoundRectangle2D.Double(3.5, 7.5, 9, 6.5, 2, 2));
			g.draw(new Line2D.Double(5.5, 7.5, 5.5, 5));
			g.draw(new Arc2D.Double(5.5, 2.5, 5, 5, 0, 180, Arc2D.OPEN));
			g.draw(new Line2D.Double(10.5, 5, 10.5, 7.5));
		});
	}

	static Icon unlock()
	{
		return new Vector(ICON_SIZE, null, (g, c) ->
		{
			g.draw(new RoundRectangle2D.Double(3.5, 7.5, 9, 6.5, 2, 2));
			g.draw(new Line2D.Double(5.5, 7.5, 5.5, 5));
			g.draw(new Arc2D.Double(5.5, 2.5, 5, 5, 0, 180, Arc2D.OPEN));
		});
	}

	static Icon trash()
	{
		return new Vector(ICON_SIZE, null, (g, c) ->
		{
			g.draw(new Line2D.Double(2.5, 4, 13.5, 4));
			g.draw(path(6, 4, 6, 2.5, 10, 2.5, 10, 4));
			g.draw(path(4, 4, 4.8, 13.5, 11.2, 13.5, 12, 4));
			g.draw(new Line2D.Double(6.8, 6.5, 6.8, 11));
			g.draw(new Line2D.Double(9.2, 6.5, 9.2, 11));
		});
	}

	static Icon block()
	{
		return new Vector(ICON_SIZE, null, (g, c) ->
		{
			g.draw(new Ellipse2D.Double(2.5, 2.5, 11, 11));
			g.draw(new Line2D.Double(4.4, 11.6, 11.6, 4.4));
		});
	}

	static Color colorFor(Component c)
	{
		if (c == null)
		{
			return Theme.TEXT_MUTED;
		}
		if (!c.isEnabled())
		{
			return Theme.TEXT_FAINT;
		}
		if (c instanceof AbstractButton)
		{
			AbstractButton b = (AbstractButton) c;
			if (b.isSelected())
			{
				return Theme.ACCENT;
			}
			if (b.getModel().isRollover() || b.getModel().isArmed())
			{
				return Theme.TEXT;
			}
		}
		return Theme.TEXT_MUTED;
	}

	private static Path2D path(double... xy)
	{
		Path2D p = new Path2D.Double();
		p.moveTo(xy[0], xy[1]);
		for (int i = 2; i < xy.length; i += 2)
		{
			p.lineTo(xy[i], xy[i + 1]);
		}
		return p;
	}

	private static final class Vector implements Icon
	{
		private final int size;
		/** Null to follow the component's state. */
		private final Color fixedColor;
		private final BiConsumer<Graphics2D, Component> painter;

		Vector(int size, Color fixedColor, BiConsumer<Graphics2D, Component> painter)
		{
			this.size = size;
			this.fixedColor = fixedColor;
			this.painter = painter;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			Graphics2D g2 = Theme.smooth(g);
			g2.translate(x, y);
			g2.setColor(fixedColor != null ? fixedColor : colorFor(c));
			g2.setStroke(STROKE);
			painter.accept(g2, c);
			g2.dispose();
		}

		@Override
		public int getIconWidth()
		{
			return size;
		}

		@Override
		public int getIconHeight()
		{
			return size;
		}
	}

	private Icons()
	{
	}
}
