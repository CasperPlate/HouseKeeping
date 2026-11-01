package com.housekeeping;

import com.google.inject.Provides;
import com.housekeeping.data.BookcaseBooks;
import com.housekeeping.data.OutgrownItems;
import com.housekeeping.data.QuestItems;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
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
import net.runelite.api.events.WidgetLoaded;
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
	name = "House Keeping",
	description = "Suggests bank items you have outgrown, can store in your POH, or no longer need",
	tags = {"bank", "poh", "quest", "storage", "cleanup"}
)
public class HouseKeepingPlugin extends Plugin
{
	// Every quest a rule depends on; their states are read on login and whenever the bank opens
	private static final Set<Quest> TRACKED_QUESTS = Stream.concat(
			BookcaseBooks.ITEMS.values().stream().map(BookcaseBooks.Entry::getQuest).filter(Objects::nonNull),
			QuestItems.ITEMS.values().stream().flatMap(Set::stream))
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
	// Inventory item id to its canonical (unnoted) id; only the set of items matters, not quantities
	private Map<Integer, Integer> inventoryItems = Collections.emptyMap();
	private boolean questsDirty;
	private Set<Quest> finishedQuests = Collections.emptySet();
	private final Map<Skill, Integer> knownLevels = new EnumMap<>(Skill.class);

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);

		panel = new HouseKeepingPanel(itemManager, this::ignore, this::restore);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("House Keeping")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			store.load();
			questsDirty = true;
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
		finishedQuests = Collections.emptySet();
		inventoryItems = Collections.emptyMap();
		knownLevels.clear();
		store.clear();
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		store.load();
		knownLevels.clear();
		questsDirty = true;
		refreshPending = true;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			questsDirty = true;
			refreshPending = true;
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		// Quest states are read on the next tick: scripts can't run from inside this callback
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			questsDirty = true;
			refreshPending = true;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INV)
		{
			Map<Integer, Integer> carried = new HashMap<>();
			for (Item item : event.getItemContainer().getItems())
			{
				if (item.getId() > 0)
				{
					carried.computeIfAbsent(item.getId(), itemManager::canonicalize);
				}
			}
			// Quantities change constantly while skilling; only re-evaluate when the items themselves change
			if (!carried.keySet().equals(inventoryItems.keySet()))
			{
				inventoryItems = carried;
				refreshPending = true;
			}
			return;
		}

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
			int widget = entry.getParam1();
			boolean bankItem = widget == InterfaceID.Bankmain.ITEMS;
			boolean inventoryItem = widget == InterfaceID.Inventory.ITEMS || widget == InterfaceID.Bankside.ITEMS;
			// Only offer the option where a marker is actually drawn
			if (!(bankItem && config.highlightBank()) && !(inventoryItem && config.highlightInventory()))
			{
				continue;
			}

			Suggestion suggestion = visibleSuggestions.get(entry.getItemId());
			if (suggestion == null)
			{
				return;
			}

			// Purely client-side: only updates the plugin's own ignore list. The suggestion's id is
			// the unnoted one, so ignoring a noted stack ignores the item itself.
			client.getMenu().createMenuEntry(1)
				.setOption("Ignore suggestion")
				.setTarget(ColorUtil.wrapWithColorTag("House Keeping", getColor(suggestion.getCategory())))
				.setType(MenuAction.RUNELITE)
				.onClick(e -> ignore(suggestion.getItemId()));
			return;
		}
	}

	Color getColor(Category category)
	{
		switch (category)
		{
			case OUTGROWN:
				return config.outgrownColor();
			case QUEST:
				return config.questColor();
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
		if (config.showQuest())
		{
			enabled.add(Category.QUEST);
		}
		if (config.showJunk())
		{
			enabled.add(Category.JUNK);
		}

		// Inventory items get markers too, but the panel and counter only describe the bank
		Map<Integer, Integer> carried = inventoryItems;
		Set<Integer> itemIds = new LinkedHashSet<>(bank.keySet());
		itemIds.addAll(carried.values());

		Map<Integer, Suggestion> visible = new HashMap<>();
		List<ItemSuggestion> active = new ArrayList<>();
		List<ItemSuggestion> ignoredItems = new ArrayList<>();
		for (Suggestion suggestion : engine.evaluate(itemIds, buildPlayerState()))
		{
			int itemId = suggestion.getItemId();
			if (!SuggestionEngine.isVisible(suggestion, enabled, config.showMaybe()))
			{
				continue;
			}

			boolean isIgnored = ignored.contains(itemId);
			if (!isIgnored)
			{
				visible.put(itemId, suggestion);
			}

			Integer quantity = bank.get(itemId);
			if (quantity != null)
			{
				ItemSuggestion item = describe(suggestion, quantity);
				(isIgnored ? ignoredItems : active).add(item);
			}
		}

		// Noted items in the inventory are drawn with their noted id
		for (Map.Entry<Integer, Integer> entry : carried.entrySet())
		{
			Suggestion suggestion = visible.get(entry.getValue());
			if (suggestion != null)
			{
				visible.putIfAbsent(entry.getKey(), suggestion);
			}
		}
		visibleSuggestions = Collections.unmodifiableMap(visible);

		boolean hasSnapshot = !bank.isEmpty();
		boolean showCounter = config.showCounter();
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
				currentPanel.update(active, ignoredItems, hasSnapshot, showCounter, colors);
			}
		});
	}

	private ItemSuggestion describe(Suggestion suggestion, int quantity)
	{
		int itemId = suggestion.getItemId();
		ItemComposition composition = itemManager.getItemComposition(itemId);

		// Show whichever is worth more: selling on the GE or high alching
		long ge = itemManager.getItemPrice(itemId);
		long ha = composition.getHaPrice();
		long each = Math.max(ge, ha);
		String source = each <= 0 ? null : ge >= ha ? "GE" : "HA";

		return new ItemSuggestion(suggestion, composition.getName(), quantity, each * quantity, source);
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

		if (questsDirty)
		{
			questsDirty = false;
			Set<Quest> finished = EnumSet.noneOf(Quest.class);
			for (Quest quest : TRACKED_QUESTS)
			{
				if (quest.getState(client) == QuestState.FINISHED)
				{
					finished.add(quest);
				}
			}
			finishedQuests = finished;
		}

		return new PlayerState(levels, finishedQuests);
	}

	@Provides
	HouseKeepingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HouseKeepingConfig.class);
	}
}
