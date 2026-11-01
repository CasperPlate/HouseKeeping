package com.housekeeping.data;

import com.google.common.collect.ImmutableMap;
import com.housekeeping.Confidence;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;

/**
 * Hand-curated items with no remaining function. Besides burnt food, these are items the OSRS Wiki
 * describes as having no use, checked by hand to leave out anything that still has one.
 */
public final class JunkItems
{
	@Value
	public static class Entry
	{
		Confidence confidence;
		String reason;
		/**
		 * The quest that still needs the item, if any. Until it's finished the item is only a 'maybe'.
		 */
		@Nullable
		Quest neededFor;
	}

	private static final String BURNT = "Burnt food has no use";
	private static final String NO_USE = "Has no use, according to the OSRS Wiki";

	private static final ImmutableMap.Builder<Integer, Entry> BUILDER = ImmutableMap.builder();

	static
	{
		for (int itemId : new int[]{
			ItemID.BURNT_SHRIMP, ItemID.BURNT_SWORDFISH, ItemID.BURNT_LOBSTER, ItemID.BURNT_SHARK,
			ItemID.BURNT_MANTARAY, ItemID.BURNT_SEATURTLE, ItemID.BURNT_MONKFISH, ItemID.BURNT_DARK_CRAB,
			ItemID.BURNT_ANGLERFISH, ItemID.BURNT_EEL, ItemID.BURNT_CAVE_EEL, ItemID.BURNT_SNAIL,
			ItemID.BURNT_CHICKEN, ItemID.BURNT_BREAD, ItemID.BURNT_PITTA_BREAD, ItemID.BURNT_CAKE,
			ItemID.BURNT_STEW, ItemID.BURNT_CURRY, ItemID.BURNT_PIZZA, ItemID.BURNT_PIE,
			ItemID.BURNT_GNOMEBOWL, ItemID.BURNT_CRUNCHIES, ItemID.BURNT_BATTA, ItemID.BURNT_OOMLIE,
			ItemID.BURNT_UW_OOMLIE,
			// Burnt fish share one name in game but have an item per fish
			ItemID.BURNTFISH1, ItemID.BURNTFISH2, ItemID.BURNTFISH3, ItemID.BURNTFISH4, ItemID.BURNTFISH5,
			ItemID.BURNT_ANTELOPE, ItemID.BURNT_BLUEFIN, ItemID.BURNT_CRAB_MEAT, ItemID.BURNT_FOXMEAT,
			ItemID.BURNT_GIANT_KRILL, ItemID.BURNT_HADDOCK, ItemID.BURNT_HALIBUT, ItemID.BURNT_JUMBO_SQUID,
			ItemID.BURNT_KEBBIT, ItemID.BURNT_LARGEBEAST, ItemID.BURNT_MARLIN, ItemID.BURNT_RAINBOW_CRAB_MEAT,
			ItemID.BURNT_SWORDTIP_SQUID, ItemID.BURNT_YELLOWFIN, ItemID.SPIT_BURNED_BEAST_MEAT,
			ItemID.SPIT_BURNED_BIRD_MEAT, ItemID.SPIT_BURNED_RABBIT_MEAT, ItemID._100_JUBBLY_MEAT_BURNED,
			ItemID.TBWT_BURNT_KARAMBWAN, ItemID.HUNTING_RAW_FISH_SPECIAL_BURNT, ItemID.TBW_SPIDER_ON_STICK_BURNT,
			ItemID.TBW_SPIDER_ON_SHAFT_BURNT, ItemID.POTATO_BURNT, ItemID.SWEETCORN_BURNT,
			ItemID.BOWL_EGG_BURNT, ItemID.BOWL_MUSHROOM_BURNT, ItemID.BOWL_ONION_BURNT,
			ItemID.RUINED_CATFISH, ItemID.RUINED_CAVEFISH, ItemID.RUINED_CHOMPY, ItemID.RUINED_GUPPY,
			ItemID.RUINED_TETRA,
		})
		{
			BUILDER.put(itemId, new Entry(Confidence.HIGH, BURNT, null));
		}

		neededFor(ItemID.BURNT_MEAT, Quest.WITCHS_POTION);

		for (int itemId : new int[]{
			ItemID.FLIER_PRIF, // Iwan's flyer
			ItemID.NEWSLETTER_SAWDUST,
			ItemID.DUST,
			ItemID.OYSTEREMPTY,
			ItemID.DEADMAN_BROKEN_CHARM,
			ItemID.BROKEN_ROD,
			ItemID.DIGSITEVASE,
			ItemID.PERFECT_RUBY_NECKLACE,
			ItemID.PERFECT_RUBY_RING,
			ItemID.IRON_SICKLE,
			ItemID.GOBLIN_ARMOUR_GREEN,
			ItemID.GOBLIN_ARMOUR_PINK,
			ItemID.GOBLIN_ARMOUR_RED,
			ItemID.TEMPLETREK_SWAMPTREE_BRANCH,
			ItemID.LEAFLET_DROPPER_FLYER, // Al Kharid flyer
			ItemID.FLIER, // Flyer for Bob's Brilliant Axes
			ItemID.RAG_BOTTLE_EMPTY, // Empty wine bottle
			ItemID.OLD_BOOT,
		})
		{
			BUILDER.put(itemId, new Entry(Confidence.HIGH, NO_USE, null));
		}

		// Heals a single hitpoint, so technically still food
		BUILDER.put(ItemID.XMAS19_GINGERBREAD_GNOME, new Entry(Confidence.MAYBE, "Only heals 1 hitpoint", null));

		// Useless once a quest no longer needs them
		neededFor(ItemID.BURGH_RUBBLE_BUCKET_1, Quest.IN_AID_OF_THE_MYREQUE);
		neededFor(ItemID.BURGH_RUBBLE_BUCKET_2, Quest.IN_AID_OF_THE_MYREQUE);
		neededFor(ItemID.BURGH_RUBBLE_BUCKET_3, Quest.IN_AID_OF_THE_MYREQUE);
		neededFor(ItemID.GROUND_BAT_BONES, Quest.WATCHTOWER);
		neededFor(ItemID.BOWL_EMPTY_FILLIMAN, Quest.NATURE_SPIRIT); // Washing bowl
		neededFor(ItemID.CRUSHED_GEMSTONE, Quest.FAIRYTALE_I__GROWING_PAINS);
		neededFor(ItemID.SCRYING_ORB_EMPTY, Quest.ENTER_THE_ABYSS);
		neededFor(ItemID.SCRYING_ORB_FULL, Quest.ENTER_THE_ABYSS);

		// The Wise Old Man's Recycling Centre removes these once their quest is complete
		neededFor(ItemID.BURGH_ROD_COMMAND_DUST, Quest.IN_AID_OF_THE_MYREQUE); // Rod dust
		neededFor(ItemID.MDAUGHTER_BROKEN_STICK, Quest.MOUNTAIN_DAUGHTER); // Broken pole
		neededFor(ItemID.MDAUGHTER_STICK, Quest.MOUNTAIN_DAUGHTER); // Pole
		neededFor(ItemID.MM_EYE_OF_GNOME, Quest.MONKEY_MADNESS_I);
		neededFor(ItemID.SMASHED_GLASS, Quest.LEGENDS_QUEST);
		neededFor(ItemID.TBWT_BANANA_IN_KARAMJA_RUM, Quest.TAI_BWO_WANNAI_TRIO); // Karamjan rum (banana)
	}

	public static final Map<Integer, Entry> ITEMS = BUILDER.build();

	private JunkItems()
	{
	}

	private static void neededFor(int itemId, Quest quest)
	{
		BUILDER.put(itemId, new Entry(Confidence.MAYBE, "Only needed for " + quest.getName(), quest));
	}
}
