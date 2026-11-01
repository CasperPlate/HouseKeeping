package com.housekeeping;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(HouseKeepingConfig.GROUP)
public interface HouseKeepingConfig extends Config
{
	String GROUP = "housekeeping";

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
		keyName = "showJunk",
		name = "Junk / retrievable items",
		description = "Suggest items with no remaining use, or that you can retrieve for free",
		section = suggestionsSection,
		position = 2
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
		position = 3
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
		keyName = "junkColor",
		name = "Junk colour",
		description = "Marker colour for junk and retrievable items",
		section = displaySection,
		position = 6
	)
	default Color junkColor()
	{
		return new Color(224, 85, 85);
	}
}
