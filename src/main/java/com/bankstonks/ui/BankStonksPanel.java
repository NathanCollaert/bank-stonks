package com.bankstonks.ui;

import com.bankstonks.BankStonksConfig;
import com.bankstonks.SortOrder;
import com.bankstonks.model.Lot;
import com.bankstonks.model.PortfolioRow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

public class BankStonksPanel extends PluginPanel
{
	private static final DateTimeFormatter LOT_DATE = DateTimeFormatter.ofPattern("d MMM yy", Locale.ENGLISH);
	private static final String ADD_TOGGLE_TOOLTIP = "Add a purchase by hand";
	private static final int EMPTY_STATE_ICON_SCALE = 2;

	private final ItemImages itemImages;
	private final BankStonksConfig config;
	private final PortfolioActions actions;

	private final SummaryCard summary = new SummaryCard();
	private final JTextField searchField = Widgets.textField("Search items", null, Theme.SURFACE);
	private final JButton addToggle;
	private final AddForm addForm;
	private final Collapsible addPanel;
	private final ListView listView = new ListView();
	private final JScrollPane scrollPane;

	private List<PortfolioRow> allRows = new ArrayList<>();
	/**
	 * Starts true: the plugin only updates on game ticks, and the login screen has none, so starting
	 * false would claim nothing is tracked.
	 */
	private boolean loggedOut = true;

	private final Set<Integer> expandedItems = new HashSet<>();

	public BankStonksPanel(ItemImages itemImages, BankStonksConfig config, PortfolioActions actions)
	{
		super(false);
		this.itemImages = itemImages;
		this.config = config;
		this.actions = actions;

		setLayout(new BorderLayout());
		setBackground(Theme.CANVAS);

		addForm = new AddForm(actions::addManual, config::lossColor, this::toggleAddForm);
		addPanel = new Collapsible(spacedFromToolbar(addForm), false, this::repaintAddToggle);
		addToggle = Widgets.iconButton(Icons.plus(() -> 45 * addPanel.progress()), ADD_TOGGLE_TOOLTIP, this::toggleAddForm);
		add(buildHeader(), BorderLayout.NORTH);

		scrollPane = new JScrollPane(listView, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		styleScrollPane(scrollPane);
		add(scrollPane, BorderLayout.CENTER);

		renderList();
	}

	private static JPanel spacedFromToolbar(AddForm form)
	{
		JPanel formWrap = new JPanel(new BorderLayout());
		formWrap.setOpaque(false);
		formWrap.setBorder(new EmptyBorder(Theme.GAP, 0, 0, 0));
		formWrap.add(form, BorderLayout.CENTER);
		return formWrap;
	}

	private JPanel buildHeader()
	{
		JButton[] sort = new JButton[1];
		sort[0] = Widgets.iconButton(Icons.sort(), "Sort by", () -> showSortMenu(sort[0]));

		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(Theme.CANVAS);
		header.setBorder(new EmptyBorder(Theme.SIDE, Theme.SIDE, 0, Theme.SIDE));
		stretch(header, summary);
		header.add(Box.createVerticalStrut(Theme.GAP));
		stretch(header, Widgets.hbox(Theme.SPACE_XS, buildSearchField(), sort[0], addToggle));
		stretch(header, addPanel);
		return header;
	}

	private static void styleScrollPane(JScrollPane pane)
	{
		pane.setBorder(new EmptyBorder(0, 0, 0, 0));
		pane.setBackground(Theme.CANVAS);
		pane.getViewport().setBackground(Theme.CANVAS);
		pane.getVerticalScrollBar().setPreferredSize(new Dimension(Theme.SCROLLBAR_WIDTH, 0));
		pane.getVerticalScrollBar().setUnitIncrement(16);
	}

	private static void stretch(JPanel parent, JComponent c)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		if (!(c instanceof Collapsible))
		{
			c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
		}
		parent.add(c);
	}

	private JTextField buildSearchField()
	{
		// FlatLaf draws the leading icon against the border, outside the field's margin.
		searchField.putClientProperty("JTextField.leadingIcon", Icons.padLeft(Icons.search(Theme.TEXT_FAINT), Theme.SPACE_XS));
		searchField.putClientProperty("JTextField.showClearButton", true);
		searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.CONTROL_SIZE));
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
		searchField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					searchField.setText("");
				}
			}
		});
		return searchField;
	}

	private void showSortMenu(JButton anchor)
	{
		JPopupMenu menu = new JPopupMenu();
		ButtonGroup group = new ButtonGroup();
		for (SortOrder order : SortOrder.values())
		{
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(order.toString(), order == config.sortOrder());
			item.addActionListener(e -> actions.setSortOrder(order));
			group.add(item);
			menu.add(item);
		}
		// Right-aligned to the button, so it opens over the sidebar rather than past its edge.
		menu.show(anchor, anchor.getWidth() - menu.getPreferredSize().width, anchor.getHeight() + Theme.SPACE_XS);
	}

	private void toggleAddForm()
	{
		setAddFormOpen(!addPanel.isExpanded(), true);
	}

	void setAddFormOpen(boolean open, boolean animate)
	{
		addPanel.setExpanded(open, animate);
		addToggle.setSelected(open);
		addToggle.setToolTipText(open ? "Close" : ADD_TOGGLE_TOOLTIP);
		if (open)
		{
			addForm.focusName();
		}
		else
		{
			releaseFocusFromHiddenAddForm();
		}
	}

	private void releaseFocusFromHiddenAddForm()
	{
		KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		Component owner = focus.getFocusOwner();
		if (owner != null && SwingUtilities.isDescendingFrom(owner, addForm))
		{
			focus.clearFocusOwner();
		}
	}

	private void repaintAddToggle()
	{
		addToggle.repaint();
	}

	/** The answer to a manual add. Thread-safe. */
	public void setStatus(String message, Color color)
	{
		SwingUtilities.invokeLater(() -> addForm.showStatus(message, color));
	}

	/** Call only after a successful add. Thread-safe. */
	public void clearManualEntry()
	{
		SwingUtilities.invokeLater(addForm::clear);
	}

	/** Thread-safe. */
	public void update(List<PortfolioRow> rows, long total)
	{
		SwingUtilities.invokeLater(() ->
		{
			loggedOut = false;
			allRows = rows;
			summary.set(rows, total, colorFor(total), config.showPercent());
			renderList();
		});
	}

	/** Thread-safe. */
	public void showLoggedOut()
	{
		SwingUtilities.invokeLater(() ->
		{
			loggedOut = true;
			allRows = new ArrayList<>();
			summary.set(allRows, 0, colorFor(0), config.showPercent());
			renderList();
		});
	}

	private void renderList()
	{
		listView.removeAll();
		List<PortfolioRow> visible = rowsMatchingSearch();
		if (loggedOut)
		{
			listView.add(emptyState("Log in to see your stonks", "Your tracked buys load once<br>you're logged in.", false), BorderLayout.CENTER);
		}
		else if (allRows.isEmpty())
		{
			listView.add(emptyState("Nothing tracked yet", "Buy something on the Grand Exchange<br>and its profit shows up here.", true), BorderLayout.CENTER);
		}
		else if (visible.isEmpty())
		{
			listView.add(noMatchLabel(), BorderLayout.NORTH);
		}
		else
		{
			listView.add(itemsCard(visible), BorderLayout.NORTH);
		}
		listView.revalidate();
		listView.repaint();
	}

	private List<PortfolioRow> rowsMatchingSearch()
	{
		String filter = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
		List<PortfolioRow> visible = new ArrayList<>();
		for (PortfolioRow row : allRows)
		{
			if (filter.isEmpty() || row.getName().toLowerCase(Locale.ROOT).contains(filter))
			{
				visible.add(row);
			}
		}
		return visible;
	}

	private JLabel noMatchLabel()
	{
		JLabel none = new JLabel("No items match \"" + searchField.getText().trim() + "\"", SwingConstants.CENTER);
		none.setFont(Theme.small());
		none.setForeground(Theme.TEXT_FAINT);
		none.setBorder(new EmptyBorder(Theme.SPACE_L, 0, 0, 0));
		return none;
	}

	private Widgets.Card itemsCard(List<PortfolioRow> visible)
	{
		Widgets.Card group = new Widgets.Card(null);
		group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
		for (int i = 0; i < visible.size(); i++)
		{
			boolean dividerAbove = i > 0;
			group.add(buildBlock(visible.get(i), dividerAbove));
		}
		return group;
	}

	private JComponent buildBlock(PortfolioRow row, boolean dividerAbove)
	{
		ItemRow itemRow = buildItemRow(row, dividerAbove);
		if (!itemRow.isCollapsible())
		{
			return itemRow;
		}

		Collapsible history = new Collapsible(buildLotList(row), expandedItems.contains(row.getItemId()), itemRow::repaint);
		history.setAlignmentX(Component.LEFT_ALIGNMENT);
		itemRow.setExpandProgress(history::progress);
		itemRow.setOnToggle(() -> toggleHistory(row, history));

		JPanel block = new JPanel();
		block.setOpaque(false);
		block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
		block.setAlignmentX(Component.LEFT_ALIGNMENT);
		block.add(itemRow);
		block.add(history);
		return block;
	}

	private ItemRow buildItemRow(PortfolioRow row, boolean dividerAbove)
	{
		AsyncBufferedImage image = itemImages.get(row.getItemId(), row.getQuantity(), row.getQuantity() > 1);
		ItemRow itemRow = new ItemRow(row, image, colorFor(row.getProfitTotal()), config.showPercent(), dividerAbove);
		image.onLoaded(() -> SwingUtilities.invokeLater(itemRow::repaint));
		itemRow.setMenu(() -> itemMenu(row));
		itemRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		return itemRow;
	}

	private JPanel buildLotList(PortfolioRow row)
	{
		JPanel lots = new JPanel();
		lots.setLayout(new BoxLayout(lots, BoxLayout.Y_AXIS));
		lots.setBackground(Theme.INSET_SURFACE);
		lots.setBorder(new EmptyBorder(Theme.SPACE_XS, 0, Theme.SPACE_XS, 0));
		for (Lot lot : row.getLots())
		{
			LotRow lotRow = new LotRow(lot, formatDate(lot.getEpochMs()), () -> lotMenu(row.getItemId(), lot));
			lotRow.setAlignmentX(Component.LEFT_ALIGNMENT);
			lots.add(lotRow);
		}
		return lots;
	}

	private void toggleHistory(PortfolioRow row, Collapsible history)
	{
		boolean open = !history.isExpanded();
		if (open)
		{
			expandedItems.add(row.getItemId());
		}
		else
		{
			expandedItems.remove(row.getItemId());
		}
		history.setExpanded(open, true);
	}

	private JPopupMenu itemMenu(PortfolioRow row)
	{
		JPopupMenu menu = new JPopupMenu();
		if (row.isLocked())
		{
			menu.add(menuItem("Unlock", Icons.unlock(), "Stop pinning this item to a single buy",
				() -> actions.unlockItem(row.getItemId())));
		}
		else
		{
			menu.add(menuItem("Lock to latest buy", Icons.lock(), "Pin this item to its most recent buy; stays shown even when you hold none",
				() -> actions.lockItem(row.getItemId())));
		}
		menu.addSeparator();
		menu.add(menuItem("Untrack", Icons.trash(), "Remove all history for this item; reappears if bought again",
			() -> untrack(row)));
		menu.add(menuItem("Block", Icons.block(), "Never show; still tracked in the background. Undo in settings.",
			() -> actions.blockItem(row.getName())));
		return menu;
	}

	/** A locked item was pinned on purpose, so untracking one asks first. */
	private void untrack(PortfolioRow row)
	{
		if (row.isLocked() && !confirmUntrackLocked(row))
		{
			return;
		}
		actions.untrackItem(row.getItemId());
	}

	private boolean confirmUntrackLocked(PortfolioRow row)
	{
		String[] options = {"Untrack", "Cancel"};
		int choice = JOptionPane.showOptionDialog(SwingUtilities.getWindowAncestor(this),
			"<html>" + escapeHtml(row.getName()) + " is locked to a buy.<br>"
				+ "Untracking deletes its whole buy history, including the locked buy.<br>"
				+ "This can't be undone.</html>",
			"Untrack locked item?", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
		return choice == 0;
	}

	private static String escapeHtml(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private JPopupMenu lotMenu(int itemId, Lot lot)
	{
		JPopupMenu menu = new JPopupMenu();
		menu.add(menuItem("Lock this buy", Icons.lock(), "Pin the item to this buy",
			() -> actions.lockLot(itemId, lot.getQuantity(), lot.getSpent(), lot.getEpochMs())));
		menu.add(menuItem("Remove this buy", Icons.trash(), "Delete this buy from the history",
			() -> actions.untrackLot(itemId, lot.getQuantity(), lot.getSpent(), lot.getEpochMs())));
		return menu;
	}

	private static JMenuItem menuItem(String text, Icon icon, String tooltip, Runnable action)
	{
		JMenuItem item = new JMenuItem(text, icon);
		item.setToolTipText(tooltip);
		item.addActionListener(e -> action.run());
		return item;
	}

	private JComponent emptyState(String title, String body, boolean withAddButton)
	{
		JPanel column = new JPanel();
		column.setOpaque(false);
		column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

		JLabel icon = new JLabel(new ImageIcon(emptyStateIcon()));
		JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
		titleLabel.setFont(Theme.body());
		titleLabel.setForeground(Theme.TEXT);
		JLabel bodyLabel = new JLabel("<html><center>" + body + "</center></html>", SwingConstants.CENTER);
		bodyLabel.setFont(Theme.small());
		bodyLabel.setForeground(Theme.TEXT_MUTED);

		for (JComponent c : new JComponent[]{icon, titleLabel, bodyLabel})
		{
			c.setAlignmentX(Component.CENTER_ALIGNMENT);
		}
		column.add(icon);
		column.add(Box.createVerticalStrut(Theme.SPACE_S));
		column.add(titleLabel);
		column.add(Box.createVerticalStrut(Theme.SPACE_XS));
		column.add(bodyLabel);
		if (withAddButton)
		{
			JButton add = Widgets.primaryButton("Add a purchase", () ->
			{
				if (!addPanel.isExpanded())
				{
					toggleAddForm();
				}
			});
			Widgets.fixSize(add, add.getPreferredSize());
			add.setAlignmentX(Component.CENTER_ALIGNMENT);
			column.add(Box.createVerticalStrut(Theme.SPACE_L));
			column.add(add);
		}
		return centredNearTop(column);
	}

	/** Near the top rather than mid-height: on a tall client the middle is far from the header. */
	private static JPanel centredNearTop(JComponent content)
	{
		JPanel holder = new JPanel(new GridBagLayout());
		holder.setOpaque(false);
		GridBagConstraints at = new GridBagConstraints();
		at.anchor = GridBagConstraints.NORTH;
		at.weightx = 1;
		at.weighty = 1;
		at.insets = new Insets(2 * Theme.SPACE_XL, 0, 0, 0);
		holder.add(content, at);
		return holder;
	}

	private static BufferedImage emptyStateIcon;

	private static BufferedImage emptyStateIcon()
	{
		if (emptyStateIcon == null)
		{
			BufferedImage src = ImageUtil.loadImageResource(BankStonksPanel.class, "/nav_button.png");
			BufferedImage big = new BufferedImage(src.getWidth() * EMPTY_STATE_ICON_SCALE, src.getHeight() * EMPTY_STATE_ICON_SCALE, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = big.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g.drawImage(src, 0, 0, big.getWidth(), big.getHeight(), null);
			g.dispose();
			emptyStateIcon = big;
		}
		return emptyStateIcon;
	}

	static String formatDate(long epochMs)
	{
		if (epochMs <= 0)
		{
			return "";
		}
		return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate().format(LOT_DATE);
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
		return Theme.TEXT_MUTED;
	}

	/**
	 * Tracks the viewport's width, so rows ellipsize instead of widening the panel, and fills its
	 * height when short, so the empty state centres.
	 */
	private final class ListView extends JPanel implements Scrollable
	{
		ListView()
		{
			super(new BorderLayout());
			setBackground(Theme.CANVAS);
		}

		/**
		 * A showing scrollbar takes the right-hand margin, so the list's edge stays in line with the
		 * header's.
		 */
		@Override
		public Insets getInsets()
		{
			boolean scrollbarShowing = scrollPane != null && scrollPane.getVerticalScrollBar().isVisible();
			return new Insets(Theme.GAP, Theme.SIDE, Theme.SIDE, scrollbarShowing ? Theme.SIDE - Theme.SCROLLBAR_WIDTH : Theme.SIDE);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return ItemRow.HEIGHT / 2;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return Math.max(ItemRow.HEIGHT, visibleRect.height - ItemRow.HEIGHT);
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return getParent() != null && getParent().getHeight() > getPreferredSize().height;
		}
	}
}
