package com.housekeeping;

import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(HouseKeepingConfig.GROUP)
public interface HouseKeepingConfig extends Config
{
	String GROUP = "housekeeping";

	@Getter
	@RequiredArgsConstructor
	enum TreasureChest
	{
		NOT_BUILT("Not built"),
		OAK("Oak"),
		TEAK("Teak"),
		MAHOGANY("Mahogany");

		private final String name;

		@Override
		public String toString()
		{
			return name;
		}
	}

	@ConfigSection(
		name = "Suggestions",
		description = "Which kinds of suggestions to show",
		position = 0
	)
	String suggestionsSection = "suggestions";

	@ConfigSection(
		name = "Display",
		description = "How suggestions are shown",
		position = 1
	)
	String displaySection = "display";

	@ConfigSection(
		name = "Player-owned house",
		description = "The costume room storage you have built, used for 'Store in your POH' suggestions",
		position = 2
	)
	String pohSection = "poh";

	@ConfigItem(
		keyName = "showOutgrown",
		name = "Outgrown skilling items",
		description = "Suggest low-tier skilling items your levels have made obsolete",
		section = suggestionsSection,
		position = 0
	)
	default boolean showOutgrown()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showQuest",
		name = "Obsolete quest items",
		description = "Suggest quest items that have no use left once their quests are complete",
		section = suggestionsSection,
		position = 1
	)
	default boolean showQuest()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPoh",
		name = "Store in your POH",
		description = "Suggest items that fit in the costume room storage you have built",
		section = suggestionsSection,
		position = 2
	)
	default boolean showPoh()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showJunk",
		name = "Junk / retrievable items",
		description = "Suggest items with no remaining use, or that you can retrieve for free",
		section = suggestionsSection,
		position = 3
	)
	default boolean showJunk()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMaybe",
		name = "Include 'maybe' suggestions",
		description = "Also show items that are usually unneeded but still have niche uses, marked with a ?",
		section = suggestionsSection,
		position = 4
	)
	default boolean showMaybe()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightBank",
		name = "Mark items in bank",
		description = "Draw a coloured dot on suggested items in the bank",
		section = displaySection,
		position = 0
	)
	default boolean highlightBank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightInventory",
		name = "Mark items in inventory",
		description = "Draw a coloured dot on suggested items in your inventory",
		section = displaySection,
		position = 1
	)
	default boolean highlightInventory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ignoreMenuOption",
		name = "Ignore menu option",
		description = "Add an 'Ignore suggestion' right-click option to marked items in the bank and inventory",
		section = displaySection,
		position = 2
	)
	default boolean ignoreMenuOption()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCounter",
		name = "Show slots counter",
		description = "Show how many bank slots and how much value the suggestions add up to at the top of the panel",
		section = displaySection,
		position = 3
	)
	default boolean showCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "outgrownColor",
		name = "Outgrown colour",
		description = "Marker colour for outgrown skilling items",
		section = displaySection,
		position = 4
	)
	default Color outgrownColor()
	{
		return new Color(63, 169, 245);
	}

	@ConfigItem(
		keyName = "questColor",
		name = "Quest item colour",
		description = "Marker colour for obsolete quest items",
		section = displaySection,
		position = 5
	)
	default Color questColor()
	{
		return new Color(181, 140, 255);
	}

	@ConfigItem(
		keyName = "pohColor",
		name = "POH storage colour",
		description = "Marker colour for items that can be stored in your POH",
		section = displaySection,
		position = 6
	)
	default Color pohColor()
	{
		return new Color(55, 192, 122);
	}

	@ConfigItem(
		keyName = "junkColor",
		name = "Junk colour",
		description = "Marker colour for junk and retrievable items",
		section = displaySection,
		position = 7
	)
	default Color junkColor()
	{
		return new Color(224, 85, 85);
	}

	@ConfigItem(
		keyName = "treasureChest",
		name = "Treasure chest",
		description = "Your treasure chest. Oak holds beginner and easy clue rewards, teak adds medium, mahogany holds all",
		section = pohSection,
		position = 0
	)
	default TreasureChest treasureChest()
	{
		return TreasureChest.NOT_BUILT;
	}

	@ConfigItem(
		keyName = "armourCase",
		name = "Armour case",
		description = "Tick if you have built an armour case in your costume room",
		section = pohSection,
		position = 1
	)
	default boolean armourCase()
	{
		return false;
	}

	@ConfigItem(
		keyName = "capeRack",
		name = "Cape rack",
		description = "Tick if you have built a cape rack in your costume room",
		section = pohSection,
		position = 2
	)
	default boolean capeRack()
	{
		return false;
	}

	@ConfigItem(
		keyName = "magicWardrobe",
		name = "Magic wardrobe",
		description = "Tick if you have built a magic wardrobe in your costume room",
		section = pohSection,
		position = 3
	)
	default boolean magicWardrobe()
	{
		return false;
	}

	@ConfigItem(
		keyName = "fancyDressBox",
		name = "Fancy dress box",
		description = "Tick if you have built a fancy dress box in your costume room",
		section = pohSection,
		position = 4
	)
	default boolean fancyDressBox()
	{
		return false;
	}

	@ConfigItem(
		keyName = "toyBox",
		name = "Toy box",
		description = "Tick if you have built a toy box in your costume room",
		section = pohSection,
		position = 5
	)
	default boolean toyBox()
	{
		return false;
	}
}
