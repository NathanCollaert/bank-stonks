package com.bankstonks.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.bankstonks.BankStonksConfig;
import com.bankstonks.SortOrder;
import com.bankstonks.model.Lot;
import com.bankstonks.model.PortfolioRow;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.laf.RuneLiteLAF;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.BeforeClass;
import org.junit.Test;

/** Writes build/panel-*.png to look at. */
public class PanelLayoutTest
{
	private static final int SIDEBAR_WIDTH = PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH;
	private static final long DAY = 86_400_000L;
	private static final int GLIDE_SETTLE_MS = 400;

	@BeforeClass
	public static void laf() throws Exception
	{
		SwingUtilities.invokeAndWait(RuneLiteLAF::setup);
	}

	@Test
	public void fullList() throws Exception
	{
		render("panel-full-900", 900, rows(), Collections.singleton(4151), false, null);
		render("panel-full-500", 500, rows(), Collections.singleton(4151), false, null);
	}

	@Test
	public void addFormOpen() throws Exception
	{
		render("panel-form", 640, rows(), Collections.emptySet(), true, null);
	}

	@Test
	public void emptyStates() throws Exception
	{
		render("panel-empty", 500, new ArrayList<>(), Collections.emptySet(), false, null);
		render("panel-nomatch", 500, rows(), Collections.emptySet(), false, "zzz");
	}

	@Test
	public void longTermHolderMostlyLockedWithBillions() throws Exception
	{
		render("panel-locked", 720, longTermHolderRows(), Collections.emptySet(), false, null);
	}

	private static List<PortfolioRow> longTermHolderRows()
	{
		long now = System.currentTimeMillis();
		long mo = 30 * DAY;
		return Arrays.asList(
			locked(1, "Inquisitor's armour set", 282_040_000, 62_040_000, now - 2 * mo),
			locked(2, "Crimson kisten", 54_000_000, 17_000_000, now - 2 * mo),
			locked(3, "Voidwaker", 39_000_000, 1_360_000, now - 2 * mo),
			locked(4, "Hallowfell", 2_200_000, 1_020_000, now - mo),
			locked(5, "Blood Moon armour set", 20_200_000, 324_100, now - 3 * mo),
			row(6, "Venator bow (uncharged)", 1, 58_670_000, 58_703_100, now, Arrays.asList(
				new Lot(1, 58_670_000, now, 0), new Lot(1, 58_100_000, now - DAY, 0)), false),
			locked(7, "Necklace of rupture", 139_000_000, -1_550_000, now),
			locked(8, "Noxious halberd", 49_580_000, -12_720_000, now - 5 * mo),
			locked(9, "Torva full helm", 234_090_000, -28_360_000, now - 5 * mo),
			locked(10, "Avernic treads", 235_540_000, -90_860_000, now - 7 * mo),
			locked(11, "Tumeken's shadow", 1_180_000_000, -398_810_000, now - 18 * mo),
			locked(12, "Scythe of vitur", 1_620_000_000, -405_180_000, now - 14 * mo));
	}

	private static PortfolioRow locked(int id, String name, long paid, long profit, long since)
	{
		return new PortfolioRow(id * 977, name, 1, paid, paid + profit, profit, profit, since, new ArrayList<>(), true);
	}

	@Test
	public void startsLoggedOut() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			BankStonksPanel panel = newPanel();
			boolean found = false;
			for (javax.swing.JLabel l : all(panel, javax.swing.JLabel.class))
			{
				found |= "Log in to see your stonks".equals(l.getText());
			}
			assertTrue("a new panel shows the logged-out state", found);
			check(panel, 900, "panel-loggedout");
		});
	}

	@Test
	public void foldsGlideOpenAndShut() throws Exception
	{
		BankStonksPanel[] holder = new BankStonksPanel[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = newPanel());
		BankStonksPanel panel = holder[0];
		List<PortfolioRow> rows = rows();
		panel.update(rows, 0);
		SwingUtilities.invokeAndWait(() ->
		{
			panel.setSize(SIDEBAR_WIDTH, 700);
			layout(panel);
			panel.setAddFormOpen(true, true);
			clickWhipRow(panel);
		});
		Thread.sleep(GLIDE_SETTLE_MS);
		SwingUtilities.invokeAndWait(() ->
		{
			List<Collapsible> folds = all(panel, Collapsible.class);
			assertEquals(2, folds.size());
			for (Collapsible fold : folds)
			{
				assertTrue("open", fold.isExpanded());
				assertEquals("fully open", fold.getComponent(0).getPreferredSize().height, fold.getPreferredSize().height);
			}
			clickWhipRow(panel);
		});
		Thread.sleep(GLIDE_SETTLE_MS);
		SwingUtilities.invokeAndWait(() ->
		{
			Collapsible history = all(panel, Collapsible.class).get(1);
			assertEquals("shut", 0, history.getPreferredSize().height);
			assertTrue("content hidden when shut", !history.getComponent(0).isVisible());
		});
	}

	private static void clickWhipRow(BankStonksPanel panel)
	{
		ItemRow whip = all(panel, ItemRow.class).get(0);
		whip.dispatchEvent(new java.awt.event.MouseEvent(whip, java.awt.event.MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(), java.awt.event.InputEvent.BUTTON1_DOWN_MASK, 10, 10, 1, false, java.awt.event.MouseEvent.BUTTON1));
	}

	private static BankStonksPanel newPanel()
	{
		return new BankStonksPanel(PanelLayoutTest::placeholderSprite, new BankStonksConfig()
		{
		}, new NoActions());
	}

	private static void render(String name, int height, List<PortfolioRow> rows, Set<Integer> expanded, boolean form, String search) throws Exception
	{
		BankStonksPanel[] holder = new BankStonksPanel[1];
		SwingUtilities.invokeAndWait(() ->
		{
			BankStonksPanel panel = newPanel();
			try
			{
				Field f = BankStonksPanel.class.getDeclaredField("expandedItems");
				f.setAccessible(true);
				@SuppressWarnings("unchecked")
				Set<Integer> set = (Set<Integer>) f.get(panel);
				set.addAll(expanded);
				if (form)
				{
					panel.setAddFormOpen(true, false);
				}
			}
			catch (ReflectiveOperationException e)
			{
				throw new RuntimeException(e);
			}
			holder[0] = panel;
		});
		long total = rows.stream().mapToLong(PortfolioRow::getProfitTotal).sum();
		holder[0].update(rows, total);
		// Queued after update()'s invokeLater, so it sees the rows.
		SwingUtilities.invokeAndWait(() ->
		{
			if (search != null)
			{
				setSearch(holder[0], search);
			}
			check(holder[0].getWrappedPanel(), height, name);
		});
	}

	private static void setSearch(BankStonksPanel panel, String text)
	{
		try
		{
			Field f = BankStonksPanel.class.getDeclaredField("searchField");
			f.setAccessible(true);
			((javax.swing.JTextField) f.get(panel)).setText(text);
		}
		catch (ReflectiveOperationException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static List<PortfolioRow> rows()
	{
		long now = System.currentTimeMillis();
		List<Lot> whipLots = Arrays.asList(
			new Lot(1, 1_520_000, now - 20 * DAY, 0),
			new Lot(1, 1_480_000, now - 200 * DAY, 0),
			new Lot(1, 2_100_000, now - 800 * DAY, 0));
		return Arrays.asList(
			row(4151, "Abyssal whip", 3, 1_700_000, 1_850_000, now - 800 * DAY, whipLots, false),
			row(12924, "Toxic blowpipe (empty)", 1, 5_200_000, 4_300_000, now - 400 * DAY, lots(1, 5_200_000, now - 400 * DAY), true),
			row(536, "Dragon bones", 25_000, 2_150, 2_310, now - 45 * DAY, lots(25_000, 2_150L * 25_000, now - 45 * DAY), false),
			row(22326, "Justiciar faceguard", 1, 18_400_000, 16_950_000, now - 3 * DAY, lots(1, 18_400_000, now - 3 * DAY), false),
			row(12002, "Occult necklace", 2, 780_000, 802_000, now - 1200 * DAY, lots(2, 1_560_000, now - 1200 * DAY), false),
			row(6685, "Saradomin brew(4)", 1_200, 9_100, 8_640, now, lots(1_200, 9_100L * 1_200, now), false),
			row(11785, "Armadyl crossbow", 1, 31_500_000, 32_100_000, 0, lots(1, 31_500_000, 0), false),
			row(19547, "Necklace of anguish (or) ornament kit applied", 1, 14_000_000, 14_000_000, now - 90 * DAY, lots(1, 14_000_000, now - 90 * DAY), false),
			row(12934, "Zulrah's scales", 184_000, 160, 151, now - 600 * DAY, lots(184_000, 160L * 184_000, now - 600 * DAY), false));
	}

	private static List<Lot> lots(int qty, long spent, long epochMs)
	{
		return Collections.singletonList(new Lot(qty, spent, epochMs, 0));
	}

	private static PortfolioRow row(int id, String name, int qty, long avg, long current, long since, List<Lot> lots, boolean locked)
	{
		long each = current - avg;
		return new PortfolioRow(id, name, qty, avg, current, each, each * qty, since, lots, locked);
	}

	private static AsyncBufferedImage placeholderSprite(int itemId, int quantity, boolean stackable)
	{
		AsyncBufferedImage img = new AsyncBufferedImage(null, 36, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Color c = Color.getHSBColor((itemId * 0.137f) % 1f, 0.45f, 0.75f);
		g.setColor(Color.BLACK);
		g.fillRoundRect(7, 5, 24, 24, 8, 8);
		g.setColor(c);
		g.fillRoundRect(8, 6, 22, 22, 8, 8);
		if (stackable)
		{
			g.setFont(FontManager.getRunescapeSmallFont());
			String text = quantity >= 100_000 ? quantity / 1000 + "K" : Integer.toString(quantity);
			g.setColor(Color.BLACK);
			g.drawString(text, 1, 10);
			g.setColor(quantity >= 100_000 ? Color.WHITE : Color.YELLOW);
			g.drawString(text, 0, 9);
		}
		g.dispose();
		// Never marked loaded: that would hand listeners to the client thread, which doesn't exist here.
		return img;
	}

	private static void check(JComponent panel, int height, String name)
	{
		panel.setSize(SIDEBAR_WIDTH, height);
		for (int pass = 0; pass < 3; pass++)
		{
			layout(panel);
		}
		assertNothingOverflowsOrIsSqueezed(panel, name);
		assertContentOnOneAlignmentColumn(panel, name);
		paint(panel, name);
	}

	private static void assertNothingOverflowsOrIsSqueezed(JComponent panel, String name)
	{
		for (JComponent c : all(panel, JComponent.class))
		{
			if (!isVisibleChain(c) || c.getWidth() == 0)
			{
				continue;
			}
			Container frame = visibleFrame(c, panel);
			Rectangle r = SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), frame);
			assertTrue(name + ": inside the visible width: " + c.getClass().getName() + " " + r + " in " + frame.getWidth(),
				r.x >= 0 && r.x + r.width <= frame.getWidth());

			if (isFixedSize(c))
			{
				assertEquals(name + ": not squeezed: " + c.getClass().getName(), c.getPreferredSize(), c.getSize());
			}
		}
	}

	private static Container visibleFrame(JComponent c, JComponent panel)
	{
		Container viewport = SwingUtilities.getAncestorOfClass(JViewport.class, c);
		return viewport != null ? viewport : panel;
	}

	private static boolean isFixedSize(JComponent c)
	{
		return c.isMinimumSizeSet() && c.isMaximumSizeSet() && c.getMinimumSize().equals(c.getMaximumSize());
	}

	private static void assertContentOnOneAlignmentColumn(JComponent panel, String name)
	{
		SummaryCard summary = all(panel, SummaryCard.class).get(0);
		int headerRight = SwingUtilities.convertPoint(summary, summary.getWidth(), 0, panel).x;
		assertEquals(name + ": summary on the column", Theme.SIDE, SwingUtilities.convertPoint(summary, 0, 0, panel).x);
		assertEquals(name + ": header right margin", panel.getWidth() - Theme.SIDE, headerRight);
		for (ItemRow row : all(panel, ItemRow.class))
		{
			assertEquals(name + ": sprite on the column", Theme.CONTENT_COLUMN_X, SwingUtilities.convertPoint(row, 0, 0, panel).x + Theme.CARD_PAD);
			assertEquals(name + ": list right edge", headerRight, SwingUtilities.convertPoint(row, row.getWidth(), 0, panel).x);
		}
	}

	/** At 2x, so the PNG is sharp enough to zoom into. */
	private static void paint(JComponent panel, String name)
	{
		int scale = 2;
		BufferedImage img = new BufferedImage(panel.getWidth() * scale, panel.getHeight() * scale, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.scale(scale, scale);
		RepaintManager rm = RepaintManager.currentManager(panel);
		boolean buffered = rm.isDoubleBufferingEnabled();
		rm.setDoubleBufferingEnabled(false);
		try
		{
			panel.paint(g);
			assertCardCornersMaskedWithCanvas(panel, img, scale, name);
			new File("build").mkdirs();
			ImageIO.write(img, "png", new File("build/" + name + ".png"));
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			rm.setDoubleBufferingEnabled(buffered);
			g.dispose();
		}
	}

	private static void assertCardCornersMaskedWithCanvas(JComponent panel, BufferedImage img, int scale, String name)
	{
		for (Widgets.Card card : all(panel, Widgets.Card.class))
		{
			if (isVisibleChain(card) && card.getHeight() > 0)
			{
				java.awt.Point p = SwingUtilities.convertPoint(card, 0, 0, panel);
				assertEquals(name + ": card corner shows the canvas", Theme.CANVAS.getRGB(), img.getRGB(p.x * scale, p.y * scale));
			}
		}
	}

	/** Container.validate needs a native peer; offscreen, run the layout managers by hand. */
	private static void layout(Component c)
	{
		if (c instanceof Container)
		{
			((Container) c).doLayout();
			for (Component child : ((Container) c).getComponents())
			{
				layout(child);
			}
		}
	}

	private static boolean isVisibleChain(Component c)
	{
		for (Component p = c; p != null; p = p.getParent())
		{
			if (!p.isVisible())
			{
				return false;
			}
		}
		return true;
	}

	private static <T> List<T> all(Container root, Class<T> type)
	{
		List<T> out = new ArrayList<>();
		for (Component c : root.getComponents())
		{
			if (type.isInstance(c))
			{
				out.add(type.cast(c));
			}
			if (c instanceof Container)
			{
				out.addAll(all((Container) c, type));
			}
		}
		return out;
	}

	private static final class NoActions implements PortfolioActions
	{
		@Override
		public void addManual(String query, int quantity, long priceEach, long heldSinceEpochMs)
		{
		}

		@Override
		public void untrackItem(int itemId)
		{
		}

		@Override
		public void untrackLot(int itemId, int quantity, long spent, long epochMs)
		{
		}

		@Override
		public void blockItem(String itemName)
		{
		}

		@Override
		public void lockItem(int itemId)
		{
		}

		@Override
		public void lockLot(int itemId, int quantity, long spent, long epochMs)
		{
		}

		@Override
		public void unlockItem(int itemId)
		{
		}

		@Override
		public void setSortOrder(SortOrder order)
		{
		}
	}
}
