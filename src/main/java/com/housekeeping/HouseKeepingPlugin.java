package com.housekeeping;

import com.google.inject.Provides;
import com.housekeeping.data.BookcaseBooks;
import com.housekeeping.data.OutgrownItems;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "HouseKeeping",
	description = "Suggests bank items you have outgrown, can store in your POH, or no longer need",
	tags = {"bank", "poh", "quest", "storage", "cleanup"}
)
public class HouseKeepingPlugin extends Plugin
{
	private static final Set<Quest> BOOKCASE_QUESTS = BookcaseBooks.ITEMS.values().stream()
		.map(BookcaseBooks.Entry::getQuest)
		.filter(Objects::nonNull)
		.collect(Collectors.toCollection(() -> EnumSet.noneOf(Quest.class)));

	@Inject
	private Client client;

	@Inject
	private HouseKeepingConfig config;

	@Inject
	private ProfileStore store;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HouseKeepingOverlay overlay;

	@Inject
	private ClientToolbar clientToolbar;

	private final SuggestionEngine engine = new SuggestionEngine();

	private HouseKeepingPanel panel;
	private NavigationButton navButton;

	/**
	 * Non-ignored suggestions passing the config filters, keyed by item id. Read every frame by the overlay.
	 */
	@Getter
	private volatile Map<Integer, Suggestion> visibleSuggestions = Collections.emptyMap();

	private volatile boolean refreshPending;
	private final Map<Skill, Integer> knownLevels = new EnumMap<>(Skill.class);

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);

		panel = new HouseKeepingPanel(itemManager, this::ignore, this::restore);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("HouseKeeping")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			store.load();
			refreshPending = true;
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
		visibleSuggestions = Collections.emptyMap();
		knownLevels.clear();
		store.clear();
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		store.load();
		knownLevels.clear();
		refreshPending = true;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			refreshPending = true;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.BANK)
		{
			return;
		}

		Map<Integer, Integer> items = new LinkedHashMap<>();
		for (Item item : event.getItemContainer().getItems())
		{
			// Placeholders have quantity 0; fillers are just empty slots
			if (item.getId() <= 0 || item.getQuantity() <= 0 || item.getId() == ItemID.BANK_FILLER)
			{
				continue;
			}
			items.merge(itemManager.canonicalize(item.getId()), item.getQuantity(), Integer::sum);
		}
		store.setBank(items);
		refreshPending = true;
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		// Fires on every xp drop, so only refresh when a real level actually changes
		Integer previous = knownLevels.put(event.getSkill(), event.getLevel());
		if (previous == null || previous != event.getLevel())
		{
			refreshPending = true;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (HouseKeepingConfig.GROUP.equals(event.getGroup()))
		{
			refreshPending = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (refreshPending)
		{
			refreshPending = false;
			refresh();
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (!config.ignoreMenuOption())
		{
			return;
		}

		for (MenuEntry entry : event.getMenuEntries())
		{
			if (entry.getParam1() != InterfaceID.Bankmain.ITEMS)
			{
				continue;
			}

			int itemId = entry.getItemId();
			Suggestion suggestion = visibleSuggestions.get(itemId);
			if (suggestion == null)
			{
				return;
			}

			// Purely client-side: only updates the plugin's own ignore list
			client.getMenu().createMenuEntry(1)
				.setOption("Ignore suggestion")
				.setTarget(ColorUtil.wrapWithColorTag("HouseKeeping", getColor(suggestion.getCategory())))
				.setType(MenuAction.RUNELITE)
				.onClick(e -> ignore(itemId));
			return;
		}
	}

	Color getColor(Category category)
	{
		switch (category)
		{
			case OUTGROWN:
				return config.outgrownColor();
			case JUNK:
			default:
				return config.junkColor();
		}
	}

	private void ignore(int itemId)
	{
		store.setIgnored(itemId, true);
		refreshPending = true;
	}

	private void restore(int itemId)
	{
		store.setIgnored(itemId, false);
		refreshPending = true;
	}

	private void refresh()
	{
		Map<Integer, Integer> bank = store.getBank();
		Set<Integer> ignored = store.getIgnored();

		Set<Category> enabled = EnumSet.noneOf(Category.class);
		if (config.showOutgrown())
		{
			enabled.add(Category.OUTGROWN);
		}
		if (config.showJunk())
		{
			enabled.add(Category.JUNK);
		}

		Map<Integer, Suggestion> visible = new LinkedHashMap<>();
		List<ItemSuggestion> active = new ArrayList<>();
		List<ItemSuggestion> ignoredItems = new ArrayList<>();
		for (Suggestion suggestion : engine.evaluate(bank.keySet(), buildPlayerState()))
		{
			if (!SuggestionEngine.isVisible(suggestion, enabled, config.showMaybe()))
			{
				continue;
			}

			int itemId = suggestion.getItemId();
			ItemSuggestion item = new ItemSuggestion(suggestion, itemManager.getItemComposition(itemId).getName(), bank.get(itemId));
			if (ignored.contains(itemId))
			{
				ignoredItems.add(item);
			}
			else
			{
				visible.put(itemId, suggestion);
				active.add(item);
			}
		}
		visibleSuggestions = Collections.unmodifiableMap(visible);

		boolean hasSnapshot = !bank.isEmpty();
		Map<Category, Color> colors = new EnumMap<>(Category.class);
		for (Category category : Category.values())
		{
			colors.put(category, getColor(category));
		}
		HouseKeepingPanel currentPanel = panel;
		SwingUtilities.invokeLater(() ->
		{
			if (currentPanel != null)
			{
				currentPanel.update(active, ignoredItems, hasSnapshot, colors);
			}
		});
	}

	private PlayerState buildPlayerState()
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (OutgrownItems.Entry entry : OutgrownItems.ITEMS.values())
		{
			for (Skill skill : entry.getLevels().keySet())
			{
				levels.computeIfAbsent(skill, client::getRealSkillLevel);
			}
		}

		Set<Quest> finished = new HashSet<>();
		for (Quest quest : BOOKCASE_QUESTS)
		{
			if (quest.getState(client) == QuestState.FINISHED)
			{
				finished.add(quest);
			}
		}

		return new PlayerState(levels, finished);
	}

	@Provides
	HouseKeepingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HouseKeepingConfig.class);
	}
}
