package com.housekeeping;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
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
	private final Map<Category, Color> colors = new EnumMap<>(Category.class);

	// Groups the user has collapsed; only the ignored list starts collapsed
	private final Set<String> collapsed = new HashSet<>();
	private final JPanel content = new JPanel();
	private final JLabel counter = new JLabel();

	HouseKeepingPanel(ItemManager itemManager, Consumer<Integer> onIgnore, Consumer<Integer> onRestore)
	{
		this.itemManager = itemManager;
		this.onIgnore = onIgnore;
		this.onRestore = onRestore;
		collapsed.add(IGNORED_GROUP);

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));

		JLabel title = new JLabel("House Keeping");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);

		counter.setFont(FontManager.getRunescapeSmallFont());
		counter.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel header = new JPanel(new BorderLayout());
		header.setBorder(new EmptyBorder(0, 0, 8, 0));
		header.add(title, BorderLayout.NORTH);
		header.add(counter, BorderLayout.SOUTH);
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
		colors.clear();
		colors.putAll(categoryColors);
		content.removeAll();

		// Every suggested item occupies exactly one bank slot
		long value = active.stream().mapToLong(ItemSuggestion::getValue).sum();
		counter.setText(active.size() + (active.size() == 1 ? " slot" : " slots") + " could be freed"
			+ (value > 0 ? " (~" + QuantityFormatter.quantityToStackSize(value) + " gp)" : ""));
		counter.setVisible(showCounter && hasSnapshot && !active.isEmpty());

		if (!hasSnapshot)
		{
			content.add(message("Open your bank once so House Keeping can look through it."));
		}
		else if (active.isEmpty())
		{
			content.add(message("Nothing to clean up. Your bank looks tidy!"));
		}

		for (Category category : Category.values())
		{
			List<ItemSuggestion> items = filter(active, category);
			if (!items.isEmpty())
			{
				addGroup(category.getDisplayName(), colors.get(category), items, false);
			}
		}

		if (!ignored.isEmpty())
		{
			addGroup(IGNORED_GROUP, ColorScheme.MEDIUM_GRAY_COLOR, ignored, true);
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

	private void addGroup(String name, Color color, List<ItemSuggestion> items, boolean ignoredGroup)
	{
		JPanel rows = buildRows(items, ignoredGroup);
		rows.setVisible(!collapsed.contains(name));

		JLabel header = new JLabel(groupTitle(name, items.size()));
		header.setForeground(Color.WHITE);
		header.setOpaque(true);
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, color),
			new EmptyBorder(5, 6, 5, 6)));
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		header.setAlignmentX(LEFT_ALIGNMENT);
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));
		header.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (!collapsed.remove(name))
				{
					collapsed.add(name);
				}
				header.setText(groupTitle(name, items.size()));
				rows.setVisible(!collapsed.contains(name));
				content.revalidate();
			}
		});

		content.add(header);
		content.add(rows);

		JPanel spacer = new JPanel();
		spacer.setOpaque(false);
		spacer.setPreferredSize(new Dimension(0, 6));
		spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
		content.add(spacer);
	}

	private String groupTitle(String name, int count)
	{
		return (collapsed.contains(name) ? "▸ " : "▾ ") + name + " (" + count + ")";
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
