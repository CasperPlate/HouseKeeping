package com.housekeeping.data;

import com.google.common.collect.ImmutableMap;
import com.housekeeping.Confidence;
import java.util.Map;
import lombok.Value;
import net.runelite.api.gameval.ItemID;

/**
 * Hand-curated items with no remaining function.
 */
public final class JunkItems
{
	@Value
	public static class Entry
	{
		Confidence confidence;
		String reason;
	}

	private static final String BURNT = "Burnt food has no use";

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
		})
		{
			BUILDER.put(itemId, new Entry(Confidence.HIGH, BURNT));
		}

		// Witch's Potion needs burnt meat
		BUILDER.put(ItemID.BURNT_MEAT, new Entry(Confidence.MAYBE, "Burnt meat is only needed for Witch's Potion"));
	}

	public static final Map<Integer, Entry> ITEMS = BUILDER.build();

	private JunkItems()
	{
	}
}
