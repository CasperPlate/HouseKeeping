package com.housekeeping;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.QuantityFormatter;

class HouseKeepingPanel extends PluginPanel
{
	private static final String IGNORED_GROUP = "Ignored";
	// Room left for the description after the icon and button columns
	private static final int TEXT_WIDTH = 105;
	private static final int BUTTON_SIZE = 20;

	private final ItemManager itemManager;
	private final Consumer<Integer> onIgnore;
	private final Consumer<Integer> onRestore;
	// Called with a category, or null for every suggestion
	private final Consumer<Category> onShowInBank;
	// Called with the item ids matching the search, or null when not searching
	private final Consumer<Set<Integer>> onSearch;
	private final Map<Category, Color> colors = new EnumMap<>(Category.class);

	// Groups the user has collapsed; only the ignored list starts collapsed
	private final Set<String> collapsed = new HashSet<>();
	private final JPanel content = new JPanel();
	private final JLabel counter = new JLabel();
	private final IconTextField searchBar = new IconTextField();
	private final JButton showInBank = new JButton("Show in bank");

	// The latest results from the plugin, kept so typing in the search bar can re-render without it
	private List<ItemSuggestion> active = List.of();
	private List<ItemSuggestion> ignored = List.of();
	private boolean hasSnapshot;
	private boolean showCounter;
	private boolean bankOpen;
	private Set<Integer> searchMatches;

	HouseKeepingPanel(ItemManager itemManager, Consumer<Integer> onIgnore, Consumer<Integer> onRestore,
		Consumer<Category> onShowInBank, Consumer<Set<Integer>> onSearch)
	{
		this.itemManager = itemManager;
		this.onIgnore = onIgnore;
		this.onRestore = onRestore;
		this.onShowInBank = onShowInBank;
		this.onSearch = onSearch;
		collapsed.add(IGNORED_GROUP);

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));

		JLabel title = new JLabel(HouseKeepingPlugin.NAME);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);

		counter.setFont(FontManager.getRunescapeSmallFont());
		counter.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PANEL_WIDTH - 20, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.setToolTipText("Search by item name or reason; typos and initials are fine");
		searchBar.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				render();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				render();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				render();
			}
		});
		searchBar.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					searchBar.setText("");
				}
			}
		});
		searchBar.addClearListener(this::render);

		showInBank.setFocusPainted(false);
		showInBank.addActionListener(e -> onShowInBank.accept(null));
		setBankOpen(false);

		JPanel controls = new JPanel(new BorderLayout(0, 6));
		controls.add(searchBar, BorderLayout.NORTH);
		controls.add(showInBank, BorderLayout.SOUTH);

		JPanel header = new JPanel(new BorderLayout(0, 6));
		header.setBorder(new EmptyBorder(0, 0, 8, 0));
		header.add(title, BorderLayout.NORTH);
		header.add(counter, BorderLayout.CENTER);
		header.add(controls, BorderLayout.SOUTH);
		add(header, BorderLayout.NORTH);

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		add(content, BorderLayout.CENTER);

		update(List.of(), List.of(), false, false, Map.of());
	}

	/**
	 * Must be called on the Swing thread.
	 */
	void update(List<ItemSuggestion> active, List<ItemSuggestion> ignored, boolean hasSnapshot, boolean showCounter,
		Map<Category, Color> categoryColors)
	{
		this.active = active;
		this.ignored = ignored;
		this.hasSnapshot = hasSnapshot;
		this.showCounter = showCounter;
		colors.clear();
		colors.putAll(categoryColors);
		render();
	}

	/**
	 * The bank tab can only be shown while the bank is open. Must be called on the Swing thread.
	 */
	void setBankOpen(boolean open)
	{
		bankOpen = open;
		showInBank.setEnabled(open);
		render();
	}

	private void render()
	{
		content.removeAll();

		// Every suggested item occupies exactly one bank slot; the counter ignores the search
		long value = active.stream().mapToLong(ItemSuggestion::getValue).sum();
		counter.setText(active.size() + (active.size() == 1 ? " slot" : " slots") + " could be freed"
			+ (value > 0 ? " (~" + QuantityFormatter.quantityToStackSize(value) + " gp)" : ""));
		counter.setVisible(showCounter && hasSnapshot && !active.isEmpty());

		String query = searchBar.getText().trim();
		boolean searching = !query.isEmpty();
		List<ItemSuggestion> shownActive = active;
		List<ItemSuggestion> shownIgnored = ignored;
		if (searching)
		{
			Map<ItemSuggestion, Integer> scores = new HashMap<>();
			score(active, query, scores);
			score(ignored, query, scores);
			int cutoff = ItemSearch.cutoff(scores.values().stream().mapToInt(Integer::intValue).max().orElse(ItemSearch.NO_MATCH));
			scores.values().removeIf(score -> score <= cutoff);
			shownActive = ranked(active, scores);
			shownIgnored = ranked(ignored, scores);
		}

		// The bank tabs show the same items as the panel; ignored items are never in them anyway
		Set<Integer> matchingIds = searching
			? shownActive.stream().map(i -> i.getSuggestion().getItemId()).collect(Collectors.toSet())
			: null;
		if (!Objects.equals(matchingIds, searchMatches))
		{
			searchMatches = matchingIds;
			onSearch.accept(matchingIds);
		}
		String shown = searching ? "matching" : "suggested";
		showInBank.setToolTipText(bankOpen
			? "Show only the " + shown + " items in your bank; click again to go back"
			: "Open your bank to show the " + shown + " items there");

		if (!hasSnapshot)
		{
			content.add(message("Open your bank once so House Keeping can look through it."));
		}
		else if (searching)
		{
			int matches = shownActive.size() + shownIgnored.size();
			content.add(message(matches == 0
				? "No suggestions match \"" + query + "\"."
				: matches + (matches == 1 ? " match" : " matches") + " for \"" + query + "\""));
		}
		else if (active.isEmpty())
		{
			content.add(message("Nothing to clean up. Your bank looks tidy!"));
		}

		for (Category category : Category.values())
		{
			List<ItemSuggestion> items = filter(shownActive, category);
			if (!items.isEmpty())
			{
				addGroup(category.getDisplayName(), colors.get(category), items, category, searching);
			}
		}

		if (!shownIgnored.isEmpty())
		{
			addGroup(IGNORED_GROUP, ColorScheme.MEDIUM_GRAY_COLOR, shownIgnored, null, searching);
		}

		content.revalidate();
		content.repaint();
	}

	private static List<ItemSuggestion> filter(List<ItemSuggestion> items, Category category)
	{
		return items.stream()
			.filter(i -> i.getSuggestion().getCategory() == category)
			.collect(Collectors.toList());
	}

	private static void score(List<ItemSuggestion> items, String query, Map<ItemSuggestion, Integer> scores)
	{
		for (ItemSuggestion item : items)
		{
			int score = ItemSearch.score(query, item.getName(), item.getSuggestion().getReason());
			if (score != ItemSearch.NO_MATCH)
			{
				scores.put(item, score);
			}
		}
	}

	/**
	 * The scored items, best match first.
	 */
	private static List<ItemSuggestion> ranked(List<ItemSuggestion> items, Map<ItemSuggestion, Integer> scores)
	{
		return items.stream()
			.filter(scores::containsKey)
			.sorted(Comparator.comparing(scores::get).reversed())
			.collect(Collectors.toList());
	}

	/**
	 * @param category the group's category, or null for the ignored list
	 */
	private void addGroup(String name, Color color, List<ItemSuggestion> items, Category category, boolean searching)
	{
		boolean ignoredGroup = category == null;
		JPanel rows = buildRows(items, ignoredGroup);
		// Search results are always shown; collapsed groups come back once the search is cleared
		rows.setVisible(searching || !collapsed.contains(name));

		JLabel title = new JLabel(groupTitle(name, items.size(), rows.isVisible()));
		title.setForeground(Color.WHITE);
		title.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		title.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				rows.setVisible(!rows.isVisible());
				if (!searching)
				{
					if (rows.isVisible())
					{
						collapsed.remove(name);
					}
					else
					{
						collapsed.add(name);
					}
				}
				title.setText(groupTitle(name, items.size(), rows.isVisible()));
				content.revalidate();
			}
		});

		JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, color),
			new EmptyBorder(5, 6, 5, 6)));
		header.setAlignmentX(LEFT_ALIGNMENT);
		header.add(title, BorderLayout.CENTER);
		if (!ignoredGroup)
		{
			header.add(showLink(category), BorderLayout.EAST);
		}
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));

		content.add(header);
		content.add(rows);

		JPanel spacer = new JPanel();
		spacer.setOpaque(false);
		spacer.setPreferredSize(new Dimension(0, 6));
		spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
		content.add(spacer);
	}

	private JLabel showLink(Category category)
	{
		JLabel link = new JLabel("show");
		link.setFont(FontManager.getRunescapeSmallFont());
		link.setForeground(bankOpen ? ColorScheme.BRAND_ORANGE : ColorScheme.MEDIUM_GRAY_COLOR);
		link.setToolTipText(bankOpen
			? "Show only these items in your bank; click again to go back"
			: "Open your bank to show these items there");
		if (bankOpen)
		{
			link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			link.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					onShowInBank.accept(category);
				}
			});
		}
		return link;
	}

	private static String groupTitle(String name, int count, boolean open)
	{
		return (open ? "\u25BE " : "\u25B8 ") + name + " (" + count + ")";
	}

	private JPanel buildRows(List<ItemSuggestion> items, boolean ignoredGroup)
	{
		JPanel rows = new JPanel();
		rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
		rows.setAlignmentX(LEFT_ALIGNMENT);
		for (ItemSuggestion item : items)
		{
			rows.add(buildRow(item, ignoredGroup));
		}
		return rows;
	}

	private JPanel buildRow(ItemSuggestion item, boolean ignoredGroup)
	{
		Suggestion suggestion = item.getSuggestion();
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setBorder(new EmptyBorder(4, 4, 4, 4));
		row.setAlignmentX(LEFT_ALIGNMENT);

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(34, 32));
		itemManager.getImage(suggestion.getItemId(), item.getQuantity(), item.getQuantity() > 1).addTo(icon);
		row.add(icon, BorderLayout.WEST);

		String maybe = suggestion.getConfidence() == Confidence.MAYBE ? " ?" : "";
		JLabel text = new JLabel("<html><body style='width:" + TEXT_WIDTH + "px'>"
			+ "<span style='color:white'>" + item.getName() + maybe + "</span>"
			+ " <span style='color:#a5a5a5'>x" + QuantityFormatter.quantityToStackSize(item.getQuantity())
			+ (item.getValueSource() != null ? ", " + item.getValueSource() + " " + QuantityFormatter.quantityToStackSize(item.getValue()) : "")
			+ "</span><br>"
			+ "<span style='color:#a5a5a5'>" + suggestion.getReason() + "</span></body></html>");
		text.setFont(FontManager.getRunescapeSmallFont());
		row.add(text, BorderLayout.CENTER);

		JButton action = new JButton(ignoredGroup ? "↺" : "✕");
		action.setToolTipText(ignoredGroup ? "Restore this suggestion" : "Ignore this suggestion");
		action.setFocusPainted(false);
		action.setBorder(new EmptyBorder(0, 0, 0, 0));
		action.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
		action.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		action.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		action.addActionListener(e -> (ignoredGroup ? onRestore : onIgnore).accept(suggestion.getItemId()));

		// Own column beside the text, pinned to the top so it never overlaps the description
		JPanel actionColumn = new JPanel(new BorderLayout());
		actionColumn.setOpaque(false);
		actionColumn.add(action, BorderLayout.NORTH);
		row.add(actionColumn, BorderLayout.EAST);

		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setOpaque(false);
		wrapper.setBorder(new EmptyBorder(1, 0, 0, 0));
		wrapper.setAlignmentX(LEFT_ALIGNMENT);
		wrapper.add(row);
		wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 1));
		return wrapper;
	}

	private static JLabel message(String text)
	{
		JLabel label = new JLabel("<html><body style='width:" + (TEXT_WIDTH + 60) + "px'>" + text + "</body></html>");
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setBorder(new EmptyBorder(0, 0, 8, 0));
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}
}
