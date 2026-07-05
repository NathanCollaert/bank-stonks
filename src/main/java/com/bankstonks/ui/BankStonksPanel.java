package com.bankstonks.ui;

import com.bankstonks.Format;
import com.bankstonks.BankStonksConfig;
import com.bankstonks.model.PortfolioRow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Sidebar panel: manual-add form, search filter, total P/L header, the held-item list and a
 * collapsible block list.
 */
public class BankStonksPanel extends PluginPanel
{
	private final ItemManager itemManager;
	private final BankStonksConfig config;
	private final PortfolioActions actions;

	private final JLabel totalLabel = new JLabel("0", SwingConstants.CENTER);

	private final JTextField nameField = new JTextField();
	private final JTextField qtyField = new JTextField();
	private final JTextField priceField = new JTextField();
	private final JTextField heldSinceField = new JTextField();
	private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

	private final JButton addToggle = new JButton();
	private final JPanel addBody = new JPanel();
	private JPanel addFormPanel;
	private boolean addExpanded = false;

	private final IconTextField searchField = new IconTextField();

	private final JPanel listPanel = new JPanel();
	private final JLabel emptyLabel = new JLabel("<html><center>No tracked items yet.<br>Buy on the GE or add one above.</center></html>", SwingConstants.CENTER);

	private List<PortfolioRow> allRows = new ArrayList<>();

	public BankStonksPanel(ItemManager itemManager, BankStonksConfig config, PortfolioActions actions)
	{
		this.itemManager = itemManager;
		this.config = config;
		this.actions = actions;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		addStretched(buildHeader());
		add(Box.createVerticalStrut(8));
		addStretched(buildAddForm());
		add(Box.createVerticalStrut(8));
		addStretched(buildSearchField());
		add(Box.createVerticalStrut(8));

		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(listPanel);

		emptyLabel.setForeground(Color.GRAY);
		emptyLabel.setFont(FontManager.getRunescapeSmallFont());
	}

	// ---- construction helpers ------------------------------------------------

	/** Adds a component to the vertical layout stretched to the full panel width. */
	private void addStretched(JComponent c)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
		add(c);
	}

	private JPanel buildHeader()
	{
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JLabel title = new JLabel("Bank Stonks profit / loss", SwingConstants.CENTER);
		title.setFont(FontManager.getRunescapeSmallFont());
		title.setForeground(Color.LIGHT_GRAY);

		totalLabel.setFont(FontManager.getRunescapeBoldFont());
		totalLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

		header.add(title, BorderLayout.NORTH);
		header.add(totalLabel, BorderLayout.CENTER);
		return header;
	}

	private JPanel buildAddForm()
	{
		JPanel form = new JPanel();
		form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
		form.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		addFormPanel = form;

		addToggle.setFocusPainted(false);
		addToggle.setHorizontalAlignment(SwingConstants.LEFT);
		addToggle.setFont(FontManager.getRunescapeSmallFont());
		addToggle.setForeground(Color.LIGHT_GRAY);
		addToggle.setContentAreaFilled(false);
		addToggle.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		addToggle.setText(addExpanded ? "[-] Add item manually" : "[+] Add item manually");
		addToggle.addActionListener(e -> toggleAddForm());

		styleField(nameField);
		styleField(qtyField);
		styleField(priceField);
		styleField(heldSinceField);

		nameField.setToolTipText("Item name");
		JPanel nameLabeled = labeled("Item", nameField);

		JPanel qtyPrice = new JPanel(new GridLayout(1, 2, 4, 0));
		qtyPrice.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		qtyField.setToolTipText("Quantity");
		priceField.setToolTipText("Price each (e.g. 1.5m, 250k, 1200)");
		qtyPrice.add(labeled("Qty", qtyField));
		qtyPrice.add(labeled("Price ea", priceField));

		heldSinceField.setToolTipText("Optional. When you bought it, e.g. 2023-03-15, Jan 2024, or 2y / 6mo ago. Blank = now.");
		JPanel heldSince = labeled("Held since (optional)", heldSinceField);

		JButton addBtn = new JButton("Add");
		addBtn.setFocusPainted(false);
		addBtn.addActionListener(e -> onAdd());

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(Color.GRAY);

		addBody.setLayout(new BoxLayout(addBody, BoxLayout.Y_AXIS));
		addBody.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		addBody.setVisible(addExpanded);

		stretch(addToggle);
		stretch(nameLabeled);
		stretch(qtyPrice);
		stretch(heldSince);
		stretch(addBtn);
		stretch(statusLabel);

		addBody.add(Box.createVerticalStrut(6));
		addBody.add(nameLabeled);
		addBody.add(Box.createVerticalStrut(4));
		addBody.add(qtyPrice);
		addBody.add(Box.createVerticalStrut(4));
		addBody.add(heldSince);
		addBody.add(Box.createVerticalStrut(4));
		addBody.add(addBtn);
		addBody.add(statusLabel);
		addBody.setAlignmentX(Component.LEFT_ALIGNMENT);
		addBody.setMaximumSize(new Dimension(Integer.MAX_VALUE, addBody.getPreferredSize().height));

		form.add(addToggle);
		form.add(addBody);
		return form;
	}

	private void styleField(JTextField field)
	{
		field.setBackground(ColorScheme.DARK_GRAY_COLOR);
		field.setForeground(Color.WHITE);
		field.setCaretColor(Color.WHITE);
		field.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			BorderFactory.createEmptyBorder(2, 4, 2, 4)));
	}

	private void toggleAddForm()
	{
		addExpanded = !addExpanded;
		addBody.setVisible(addExpanded);
		addToggle.setText(addExpanded ? "[-] Add item manually" : "[+] Add item manually");
		addFormPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, addFormPanel.getPreferredSize().height));
		revalidate();
		repaint();
	}

	private static void stretch(JComponent c)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
	}

	private JPanel labeled(String text, Component field)
	{
		JPanel p = new JPanel(new BorderLayout(0, 2));
		p.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(Color.GRAY);
		p.add(l, BorderLayout.NORTH);
		p.add(field, BorderLayout.CENTER);
		return p;
	}

	private IconTextField buildSearchField()
	{
		searchField.setIcon(IconTextField.Icon.SEARCH);
		searchField.setPreferredSize(new Dimension(100, 30));
		searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchField.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				renderList();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				renderList();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				renderList();
			}
		});
		searchField.addClearListener(this::renderList);
		return searchField;
	}

	// ---- actions -------------------------------------------------------------

	private void onAdd()
	{
		String name = nameField.getText().trim();
		Integer qty = parseAmountInt(qtyField.getText());
		Long price = parseAmount(priceField.getText());

		if (name.isEmpty() || qty == null || qty <= 0 || price == null || price <= 0)
		{
			setStatus("Enter an item, quantity and price.", config.lossColor());
			return;
		}

		Long heldSince = parseHeldSince(heldSinceField.getText());
		if (heldSince == null)
		{
			setStatus("Couldn't read 'held since'. Try 2023-03-15 or 2y.", config.lossColor());
			return;
		}

		// Fields are cleared only on success, via clearManualEntry() from the plugin.
		actions.addManual(name, qty, price, heldSince);
	}

	/** Shows a transient status message under the add form. Thread-safe. */
	public void setStatus(String message, Color color)
	{
		SwingUtilities.invokeLater(() ->
		{
			statusLabel.setText(message == null || message.isEmpty() ? " " : message);
			statusLabel.setForeground(color);
		});
	}

	/** Clears the manual-add fields. Called only after a successful add. Thread-safe. */
	public void clearManualEntry()
	{
		SwingUtilities.invokeLater(() ->
		{
			nameField.setText("");
			qtyField.setText("");
			priceField.setText("");
			heldSinceField.setText("");
		});
	}

	// ---- rendering -----------------------------------------------------------

	/** Rebuilds the panel from the given rows. Thread-safe. */
	public void update(List<PortfolioRow> rows, long total)
	{
		SwingUtilities.invokeLater(() ->
		{
			allRows = rows;

			totalLabel.setText(Format.gp(total));
			totalLabel.setForeground(colorFor(total));

			renderList();
		});
	}

	private void renderList()
	{
		listPanel.removeAll();

		String filter = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
		List<PortfolioRow> visible = new ArrayList<>();
		for (PortfolioRow row : allRows)
		{
			if (filter.isEmpty() || row.getName().toLowerCase(Locale.ROOT).contains(filter))
			{
				visible.add(row);
			}
		}

		if (allRows.isEmpty())
		{
			listPanel.add(emptyLabel);
		}
		else if (visible.isEmpty())
		{
			JLabel none = new JLabel("No matches.", SwingConstants.CENTER);
			none.setForeground(Color.GRAY);
			none.setFont(FontManager.getRunescapeSmallFont());
			listPanel.add(none);
		}
		else
		{
			for (PortfolioRow row : visible)
			{
				listPanel.add(buildRow(row));
				listPanel.add(Box.createVerticalStrut(4));
			}
		}

		listPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, listPanel.getPreferredSize().height));
		listPanel.revalidate();
		listPanel.repaint();
	}

	private JPanel buildRow(PortfolioRow row)
	{
		JPanel panel = new JPanel(new BorderLayout(6, 0));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 4));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(32, 32));
		AsyncBufferedImage image = itemManager.getImage(row.getItemId(), row.getQuantity(), row.getQuantity() > 1);
		image.addTo(icon);
		panel.add(icon, BorderLayout.WEST);

		String age = Format.age(row.getFirstBoughtEpochMs());
		String heldText = age.isEmpty() ? "" : "held " + age;
		JPanel middle = new JPanel(new GridLayout(heldText.isEmpty() ? 2 : 3, 1));
		middle.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JLabel name = new JLabel(row.getName());
		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(Color.WHITE);
		JLabel detail = new JLabel(row.getQuantity() + " @ " + Format.plain(row.getAverageBuyPrice()));
		detail.setFont(FontManager.getRunescapeSmallFont());
		detail.setForeground(Color.GRAY);
		middle.add(name);
		middle.add(detail);
		if (!heldText.isEmpty())
		{
			JLabel held = new JLabel(heldText);
			held.setFont(FontManager.getRunescapeSmallFont());
			held.setForeground(Color.GRAY.darker());
			middle.add(held);
		}
		panel.add(middle, BorderLayout.CENTER);

		JPanel plInfo = new JPanel(new GridLayout(config.showPercent() ? 2 : 1, 1));
		plInfo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JLabel pl = new JLabel(Format.gp(row.getProfitTotal()), SwingConstants.RIGHT);
		pl.setFont(FontManager.getRunescapeSmallFont());
		pl.setForeground(colorFor(row.getProfitTotal()));
		plInfo.add(pl);
		if (config.showPercent())
		{
			JLabel pct = new JLabel(String.format("%+.1f%%", row.profitPercent()), SwingConstants.RIGHT);
			pct.setFont(FontManager.getRunescapeSmallFont());
			pct.setForeground(colorFor(row.getProfitTotal()));
			plInfo.add(pct);
		}
		panel.add(plInfo, BorderLayout.EAST);

		// Right-click a row for untrack (delete data; reappears if rebought) or block (never
		// show; still tracked in the background).
		JPopupMenu popup = new JPopupMenu();
		JMenuItem untrack = new JMenuItem("Untrack " + row.getName());
		untrack.setToolTipText("Remove now; reappears with fresh data if bought again");
		untrack.addActionListener(e -> actions.untrackItem(row.getItemId()));
		JMenuItem block = new JMenuItem("Block " + row.getName());
		block.setToolTipText("Never show; still tracked in the background. Undo in settings.");
		block.addActionListener(e -> actions.blockItem(row.getName()));
		popup.add(untrack);
		popup.add(block);
		panel.setComponentPopupMenu(popup);
		inheritPopup(panel);

		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
		return panel;
	}

	/** Lets a row's child components trigger the row's right-click menu too. */
	private static void inheritPopup(java.awt.Container container)
	{
		for (java.awt.Component child : container.getComponents())
		{
			if (child instanceof JComponent)
			{
				((JComponent) child).setInheritsPopupMenu(true);
			}
			if (child instanceof java.awt.Container)
			{
				inheritPopup((java.awt.Container) child);
			}
		}
	}

	private Color colorFor(long value)
	{
		if (value > 0)
		{
			return config.profitColor();
		}
		if (value < 0)
		{
			return config.lossColor();
		}
		return Color.LIGHT_GRAY;
	}

	/** Parses "1.5m", "250k", "1,200", "999" into a long. Returns null if unparseable. */
	static Long parseAmount(String raw)
	{
		if (raw == null)
		{
			return null;
		}
		String s = raw.trim().toLowerCase(Locale.ROOT).replace(",", "").replace(" ", "");
		if (s.isEmpty())
		{
			return null;
		}
		double mult = 1;
		char last = s.charAt(s.length() - 1);
		if (last == 'k')
		{
			mult = 1_000;
			s = s.substring(0, s.length() - 1);
		}
		else if (last == 'm')
		{
			mult = 1_000_000;
			s = s.substring(0, s.length() - 1);
		}
		else if (last == 'b')
		{
			mult = 1_000_000_000;
			s = s.substring(0, s.length() - 1);
		}
		try
		{
			return (long) (Double.parseDouble(s) * mult);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	static Integer parseAmountInt(String raw)
	{
		Long v = parseAmount(raw);
		if (v == null || v > Integer.MAX_VALUE)
		{
			return null;
		}
		return v.intValue();
	}

	private static final Pattern REL_TOKEN =
		Pattern.compile("(\\d+)\\s*(years?|yrs?|y|months?|mons?|mo|weeks?|wks?|w|days?|d)");

	private static final String[] DATE_FORMATS =
		{"yyyy-MM-dd", "yyyy/MM/dd", "d-M-yyyy", "d/M/yyyy", "d MMM yyyy", "d MMMM yyyy"};

	private static final String[] YEAR_MONTH_FORMATS =
		{"yyyy-MM", "MMM yyyy", "MMMM yyyy", "MMM-yyyy"};

	/**
	 * Parses the optional "held since" input into an epoch-millis timestamp.
	 *
	 * <ul>
	 *   <li>blank → 0 (meaning "use now")</li>
	 *   <li>relative age: "2y", "6mo", "2y 3mo", "30d ago"</li>
	 *   <li>absolute date: "2023-03-15", "Jan 2024", "2022"</li>
	 * </ul>
	 *
	 * @return epoch millis, 0 for blank, or {@code null} if the value can't be understood.
	 */
	static Long parseHeldSince(String raw)
	{
		if (raw == null)
		{
			return 0L;
		}
		String s = raw.trim().toLowerCase(Locale.ROOT);
		if (s.isEmpty())
		{
			return 0L;
		}
		Long relative = parseRelativeAge(s);
		if (relative != null)
		{
			return relative;
		}
		return parseAbsoluteDate(s);
	}

	private static Long parseRelativeAge(String s)
	{
		String cleaned = s.replace("ago", "").replace(",", " ").trim();
		if (cleaned.isEmpty())
		{
			return null;
		}
		Matcher m = REL_TOKEN.matcher(cleaned);
		long totalDays = 0;
		int idx = 0;
		boolean any = false;
		while (m.find())
		{
			if (!cleaned.substring(idx, m.start()).trim().isEmpty())
			{
				return null;
			}
			totalDays += Long.parseLong(m.group(1)) * unitDays(m.group(2));
			idx = m.end();
			any = true;
		}
		if (!any || !cleaned.substring(idx).trim().isEmpty())
		{
			return null;
		}
		return System.currentTimeMillis() - totalDays * 86_400_000L;
	}

	private static long unitDays(String unit)
	{
		if (unit.startsWith("y"))
		{
			return 365;
		}
		if (unit.startsWith("mo"))
		{
			return 30;
		}
		if (unit.startsWith("w"))
		{
			return 7;
		}
		return 1;
	}

	private static Long parseAbsoluteDate(String s)
	{
		for (String fmt : DATE_FORMATS)
		{
			try
			{
				return toEpochMs(LocalDate.parse(s, caseInsensitive(fmt)));
			}
			catch (Exception ignored)
			{
				// try next format
			}
		}
		for (String fmt : YEAR_MONTH_FORMATS)
		{
			try
			{
				return toEpochMs(YearMonth.parse(s, caseInsensitive(fmt)).atDay(1));
			}
			catch (Exception ignored)
			{
				// try next format
			}
		}
		if (s.matches("\\d{4}"))
		{
			return toEpochMs(LocalDate.of(Integer.parseInt(s), 1, 1));
		}
		return null;
	}

	private static DateTimeFormatter caseInsensitive(String pattern)
	{
		return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.ENGLISH);
	}

	private static long toEpochMs(LocalDate date)
	{
		return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
}
