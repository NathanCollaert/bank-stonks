package com.bankstonks.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class AddForm extends Widgets.Card
{
	/** Answered later through {@link #showStatus} and, on success, {@link #clear}. */
	interface Submit
	{
		void add(String name, int quantity, long priceEach, long heldSinceEpochMs);
	}

	private static final String HINT = "Amounts take k, m and b, e.g. 1.5m";
	private static final long MS_PER_DAY = 86_400_000L;

	private final JTextField nameField = field("Item name", "The item's name; the closest match is used");
	private final JTextField qtyField = field("Quantity", "How many you bought");
	private final JTextField priceField = field("Price each", "Price paid per item, e.g. 1.5m, 250k or 1200");
	private final JTextField heldField = field("Held since (optional)",
		"When you bought it, e.g. 2023-03-15, Jan 2024, or 2y / 6mo ago. Blank = now.");
	private final JLabel status = new JLabel(HINT);
	private final JButton addButton;
	private final Submit submit;
	/** Read each time, so a config change applies straight away. */
	private final Supplier<Color> errorColor;
	private boolean pending;

	AddForm(Submit submit, Supplier<Color> errorColor, Runnable onEscape)
	{
		super(null);
		this.submit = submit;
		this.errorColor = errorColor;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(Theme.CARD_PAD, Theme.CARD_PAD, Theme.CARD_PAD, Theme.CARD_PAD));

		JPanel qtyPrice = new JPanel(new GridLayout(1, 2, Theme.SPACE_XS, 0));
		qtyPrice.setOpaque(false);
		qtyPrice.add(qtyField);
		qtyPrice.add(priceField);

		status.setFont(Theme.small());
		status.setForeground(Theme.TEXT_FAINT);

		addButton = Widgets.primaryButton("Add", this::onAdd);
		addButton.setEnabled(false);

		JLabel title = Widgets.sectionLabel("ADD A PURCHASE");
		stack(title, Theme.SPACE_XS, nameField, Theme.SPACE_XS, qtyPrice, Theme.SPACE_XS, heldField,
			Theme.SPACE_S, status, Theme.SPACE_S, addButton);

		DocumentListener changed = new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				validateLive();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				validateLive();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				validateLive();
			}
		};
		KeyAdapter keys = new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER && addButton.isEnabled())
				{
					onAdd();
				}
				else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					onEscape.run();
				}
			}
		};
		for (JTextField f : allFields())
		{
			f.getDocument().addDocumentListener(changed);
			f.addKeyListener(keys);
		}
	}

	private JTextField[] allFields()
	{
		return new JTextField[]{nameField, qtyField, priceField, heldField};
	}

	private void stack(Object... componentsAndGaps)
	{
		for (Object item : componentsAndGaps)
		{
			if (item instanceof Integer)
			{
				add(Box.createVerticalStrut((Integer) item));
				continue;
			}
			JComponent c = (JComponent) item;
			c.setAlignmentX(Component.LEFT_ALIGNMENT);
			c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
			add(c);
		}
	}

	private static JTextField field(String placeholder, String tooltip)
	{
		return Widgets.textField(placeholder, tooltip, Theme.CANVAS);
	}

	void focusName()
	{
		nameField.requestFocusInWindow();
	}

	boolean hasFocusedField()
	{
		for (JTextField f : allFields())
		{
			if (f.isFocusOwner())
			{
				return true;
			}
		}
		return false;
	}

	/** Leaves "held since" to {@link #onAdd}: a half-typed date is never valid. */
	private void validateLive()
	{
		if (pending)
		{
			return;
		}
		boolean qtyBad = !isBlank(qtyField) && !validQty();
		boolean priceBad = !isBlank(priceField) && !validPrice();
		mark(qtyField, qtyBad);
		mark(priceField, priceBad);
		mark(heldField, false);
		mark(nameField, false);

		if (qtyBad)
		{
			setStatus("Quantity should be a number, e.g. 500", errorColor.get());
		}
		else if (priceBad)
		{
			setStatus("Price should be an amount, e.g. 250k", errorColor.get());
		}
		else
		{
			setStatus(HINT, Theme.TEXT_FAINT);
		}

		addButton.setEnabled(!qtyBad && !priceBad && requiredFieldsFilled());
	}

	private boolean requiredFieldsFilled()
	{
		return !isBlank(nameField) && !isBlank(qtyField) && !isBlank(priceField);
	}

	private static boolean isBlank(JTextField field)
	{
		return field.getText().trim().isEmpty();
	}

	private boolean validQty()
	{
		Integer qty = parseAmountInt(qtyField.getText());
		return qty != null && qty > 0;
	}

	private boolean validPrice()
	{
		Long price = parseAmount(priceField.getText());
		return price != null && price > 0;
	}

	private void onAdd()
	{
		if (pending || !addButton.isEnabled())
		{
			return;
		}
		Long heldSince = parseHeldSince(heldField.getText());
		if (heldSince == null)
		{
			mark(heldField, true);
			setStatus("Held since: try 2023-03-15 or 2y", errorColor.get());
			heldField.requestFocusInWindow();
			return;
		}

		pending = true;
		addButton.setEnabled(false);
		setStatus("Adding…", Theme.TEXT_FAINT);
		// Not cleared here: the plugin calls clear() only on success.
		submit.add(nameField.getText().trim(), parseAmountInt(qtyField.getText()), parseAmount(priceField.getText()), heldSince);
	}

	void showStatus(String message, Color color)
	{
		pending = false;
		validateLive();
		setStatus(message == null || message.isEmpty() ? HINT : message, color);
	}

	/** Keeps the success message, which emptying the fields would otherwise replace. */
	void clear()
	{
		String message = status.getText();
		Color color = status.getForeground();
		for (JTextField f : allFields())
		{
			f.setText("");
		}
		setStatus(message, color);
		focusName();
	}

	private void setStatus(String message, Color color)
	{
		status.setText(message);
		status.setForeground(color);
	}

	private static void mark(JTextField field, boolean error)
	{
		field.putClientProperty("JComponent.outline", error ? "error" : null);
	}

	/** Takes "1.5m", "250k", "1,200" or "999"; null if unreadable. */
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

	private static final Pattern RELATIVE_AGE_TOKEN =
		Pattern.compile("(\\d+)\\s*(years?|yrs?|y|months?|mons?|mo|weeks?|wks?|w|days?|d)");

	private static final String[] DATE_FORMATS =
		{"yyyy-MM-dd", "yyyy/MM/dd", "d-M-yyyy", "d/M/yyyy", "d MMM yyyy", "d MMMM yyyy"};

	private static final String[] YEAR_MONTH_FORMATS =
		{"yyyy-MM", "MMM yyyy", "MMMM yyyy", "MMM-yyyy"};

	/**
	 * Takes an age ("2y", "6mo", "2y 3mo", "30d ago") or a date ("2023-03-15", "Jan 2024", "2022").
	 *
	 * @return epoch millis, 0 for blank (meaning now), or {@code null} if unreadable
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
		Matcher m = RELATIVE_AGE_TOKEN.matcher(cleaned);
		long totalDays = 0;
		int parsedUpTo = 0;
		boolean foundToken = false;
		while (m.find())
		{
			if (!cleaned.substring(parsedUpTo, m.start()).trim().isEmpty())
			{
				return null;
			}
			totalDays += Long.parseLong(m.group(1)) * unitDays(m.group(2));
			parsedUpTo = m.end();
			foundToken = true;
		}
		if (!foundToken || !cleaned.substring(parsedUpTo).trim().isEmpty())
		{
			return null;
		}
		return System.currentTimeMillis() - totalDays * MS_PER_DAY;
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
