package com.bankstonks;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.MenuSelectionManager;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

/** Never presses Add, so a tour cannot write to the logged-in account's real portfolio. */
@Slf4j
@PluginDescriptor(name = "Bank Stonks Dev", description = "Dev only: UI tour and sidebar snapshots")
public class BankStonksDevPlugin extends Plugin
{
	private static final String TAG = "[BS-DEV]";
	private static final int STEP_DELAY = 1200;
	private static final int WATCH_MS = 2000;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ClientToolbar clientToolbar;

	private final Deque<Runnable> queue = new ArrayDeque<>();
	private final List<String> failures = new ArrayList<>();
	private int passed;
	private File outDir;
	private JComponent panel;
	private Timer stepTimer;
	private Timer watchTimer;
	private int lastHash;
	private int watchCount;

	@Override
	protected void startUp()
	{
		outDir = new File("build/devshots");
		outDir.mkdirs();
		File[] old = outDir.listFiles((d, n) -> n.endsWith(".png"));
		if (old != null)
		{
			for (File f : old)
			{
				f.delete();
			}
		}
		waitForPanel(0);
	}

	@Override
	protected void shutDown()
	{
		if (stepTimer != null)
		{
			stepTimer.stop();
		}
		if (watchTimer != null)
		{
			watchTimer.stop();
		}
	}

	private BankStonksPlugin plugin()
	{
		for (Plugin p : pluginManager.getPlugins())
		{
			if (p instanceof BankStonksPlugin)
			{
				return (BankStonksPlugin) p;
			}
		}
		return null;
	}

	private Object field(Object target, String name)
	{
		try
		{
			Field f = target.getClass().getDeclaredField(name);
			f.setAccessible(true);
			return f.get(target);
		}
		catch (ReflectiveOperationException e)
		{
			throw new IllegalStateException("no field " + name, e);
		}
	}

	private void waitForPanel(int attempt)
	{
		BankStonksPlugin p = plugin();
		JComponent found = p == null ? null : (JComponent) field(p, "panel");
		if (found != null && found.getParent() != null && SwingUtilities.getWindowAncestor(found) != null
			&& SwingUtilities.getWindowAncestor(found).isShowing())
		{
			panel = found;
			log.info("{} client UI up; starting the tour in 8s (about 15s long). Hands off the Bank Stonks panel until RESULT, please.", TAG);
			later(8000, () -> openTab((NavigationButton) field(p, "navButton")));
			return;
		}
		if (attempt > 240)
		{
			log.error("{} gave up waiting for the panel", TAG);
			return;
		}
		later(1000, () -> waitForPanel(attempt + 1));
	}

	private void openTab(NavigationButton nav)
	{
		clientToolbar.openPanel(nav);
		script();
		next();
	}

	private void script()
	{
		step(() -> check(panel.isShowing(), "panel is on screen"));
		step(() ->
		{
			snapshot("01-open");
			button("Add a purchase by hand").doClick();
		});
		step(() ->
		{
			JTextField name = field("Item name");
			check(name.isShowing() && name.getHeight() > 0, "add form opened");
			check(button("Close").isSelected(), "+ button shows the open state");
			snapshot("02-form");
			field("Quantity").setText("abc");
		});
		step(() ->
		{
			check("error".equals(field("Quantity").getClientProperty("JComponent.outline")), "bad quantity is outlined");
			check(!button("Add").isEnabled(), "Add disabled while invalid");
			snapshot("03-invalid");
			field("Item name").setText("Dragon bones");
			field("Quantity").setText("2.5k");
			field("Price each").setText("2.1k");
		});
		step(() ->
		{
			check(field("Quantity").getClientProperty("JComponent.outline") == null, "valid quantity not outlined");
			check(button("Add").isEnabled(), "Add enabled once filled in");
			snapshot("04-valid");
			for (String f : new String[]{"Item name", "Quantity", "Price each"})
			{
				field(f).setText("");
			}
			JTextField name = field("Item name");
			name.dispatchEvent(new KeyEvent(name, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));
		});
		step(() ->
		{
			check(!field("Item name").isShowing(), "Escape closed the form");
			check(button("Add a purchase by hand") != null, "+ button back to closed");
			snapshot("05-closed");
			button("Sort by").doClick();
		});
		step(() ->
		{
			check(showingPopup() != null, "sort menu opened");
			snapshot("06-sort");
			MenuSelectionManager.defaultManager().clearSelectedPath();
			JPopupMenu menu = showingPopup();
			if (menu != null)
			{
				menu.setVisible(false);
			}
			search().setText("zzz");
		});
		step(() ->
		{
			snapshot("07-nomatch");
			search().setText("");
		});
		step(() ->
		{
			check(search().getText().isEmpty() && !field("Item name").isShowing(), "panel left as found");
			snapshot("08-end");
			log.info("{} RESULT: {} passed, {} failed", TAG, passed, failures.size());
			for (String f : failures)
			{
				log.info("{}   failed: {}", TAG, f);
			}
			log.info("{} now watching: a snapshot is written whenever the panel changes", TAG);
			watch();
		});
	}

	private void watch()
	{
		watchTimer = new Timer(WATCH_MS, e ->
		{
			if (!panel.isShowing())
			{
				return;
			}
			BufferedImage img = render(1);
			int hash = Arrays.hashCode(((DataBufferInt) img.getRaster().getDataBuffer()).getData());
			if (hash != lastHash)
			{
				lastHash = hash;
				snapshot(String.format("w-%03d", ++watchCount));
			}
		});
		watchTimer.start();
	}

	private void step(Runnable r)
	{
		queue.add(() ->
		{
			try
			{
				r.run();
			}
			catch (RuntimeException e)
			{
				failures.add("step threw " + e);
				log.warn("{} FAIL step threw", TAG, e);
			}
		});
	}

	private void next()
	{
		Runnable r = queue.poll();
		if (r == null)
		{
			return;
		}
		r.run();
		later(STEP_DELAY, this::next);
	}

	private void later(int ms, Runnable r)
	{
		stepTimer = new Timer(ms, e -> r.run());
		stepTimer.setRepeats(false);
		stepTimer.start();
	}

	private void check(boolean ok, String what)
	{
		if (ok)
		{
			passed++;
			log.info("{} PASS {}", TAG, what);
		}
		else
		{
			failures.add(what);
			log.info("{} FAIL {}", TAG, what);
		}
	}

	private AbstractButton button(String label)
	{
		for (AbstractButton b : find(panel, AbstractButton.class))
		{
			if (b.isShowing() && (label.equals(b.getText()) || label.equals(b.getToolTipText())))
			{
				return b;
			}
		}
		throw new IllegalStateException("no showing button '" + label + "'");
	}

	private JTextField field(String placeholder)
	{
		for (JTextField f : find(panel, JTextField.class))
		{
			if (placeholder.equals(f.getClientProperty("JTextField.placeholderText")))
			{
				return f;
			}
		}
		throw new IllegalStateException("no field '" + placeholder + "'");
	}

	private JTextField search()
	{
		return field("Search items");
	}

	private static JPopupMenu showingPopup()
	{
		for (Window w : Window.getWindows())
		{
			if (w.isShowing())
			{
				for (JPopupMenu m : find(w, JPopupMenu.class))
				{
					if (m.isShowing())
					{
						return m;
					}
				}
			}
		}
		return null;
	}

	private static <T> List<T> find(Container root, Class<T> type)
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
				out.addAll(find((Container) c, type));
			}
		}
		return out;
	}

	private BufferedImage render(int scale)
	{
		BufferedImage img = new BufferedImage(panel.getWidth() * scale, panel.getHeight() * scale, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.scale(scale, scale);
		RepaintManager rm = RepaintManager.currentManager(panel);
		boolean buffered = rm.isDoubleBufferingEnabled();
		rm.setDoubleBufferingEnabled(false);
		try
		{
			panel.paint(g);
			paintShowingPopupOver(g);
		}
		finally
		{
			rm.setDoubleBufferingEnabled(buffered);
			g.dispose();
		}
		return img;
	}

	/** Popups are separate windows, so {@code panel.paint} leaves them out. */
	private void paintShowingPopupOver(Graphics2D g)
	{
		JPopupMenu menu = showingPopup();
		if (menu == null)
		{
			return;
		}
		Point m = menu.getLocationOnScreen();
		Point p = panel.getLocationOnScreen();
		Graphics2D pg = (Graphics2D) g.create();
		pg.translate(m.x - p.x, m.y - p.y);
		menu.paint(pg);
		pg.dispose();
	}

	private void snapshot(String name)
	{
		try
		{
			ImageIO.write(render(2), "png", new File(outDir, name + ".png"));
			log.info("{} snapshot {}", TAG, name);
		}
		catch (Exception e)
		{
			log.warn("{} snapshot {} failed", TAG, name, e);
		}
	}
}
