package com.housekeeping.data;

import com.google.common.collect.ImmutableMap;
import com.housekeeping.Confidence;
import java.util.Map;
import lombok.Value;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Hand-curated skilling items that stop being useful once every listed skill reaches its level.
 */
public final class OutgrownItems
{
	@Value
	public static class Entry
	{
		Confidence confidence;
		Map<Skill, Integer> levels;
	}

	private static final ImmutableMap.Builder<Integer, Entry> BUILDER = ImmutableMap.builder();

	static
	{
		// Low-tier raw fish: nothing left to gain from cooking them or fishing them up again
		add(ItemID.RAW_SHRIMP, Confidence.HIGH, Skill.FISHING, 70, Skill.COOKING, 70);
		add(ItemID.RAW_ANCHOVIES, Confidence.HIGH, Skill.FISHING, 70, Skill.COOKING, 70);
		add(ItemID.RAW_SARDINE, Confidence.HIGH, Skill.FISHING, 70, Skill.COOKING, 70);
		add(ItemID.RAW_HERRING, Confidence.HIGH, Skill.FISHING, 70, Skill.COOKING, 70);

		// Low-healing cooked fish still works as food, so only a maybe
		add(ItemID.SHRIMP, Confidence.MAYBE, Skill.FISHING, 70, Skill.COOKING, 70);
		add(ItemID.ANCHOVIES, Confidence.MAYBE, Skill.FISHING, 70, Skill.COOKING, 70);

		// Rune tools unlock at 41; bronze and iron have no remaining use past that
		add(ItemID.BRONZE_AXE, Confidence.HIGH, Skill.WOODCUTTING, 41);
		add(ItemID.IRON_AXE, Confidence.HIGH, Skill.WOODCUTTING, 41);
		add(ItemID.STEEL_AXE, Confidence.MAYBE, Skill.WOODCUTTING, 41);
		add(ItemID.BLACK_AXE, Confidence.MAYBE, Skill.WOODCUTTING, 41);
		add(ItemID.MITHRIL_AXE, Confidence.MAYBE, Skill.WOODCUTTING, 41);
		add(ItemID.ADAMANT_AXE, Confidence.MAYBE, Skill.WOODCUTTING, 41);
		add(ItemID.BRONZE_PICKAXE, Confidence.HIGH, Skill.MINING, 41);
		add(ItemID.IRON_PICKAXE, Confidence.HIGH, Skill.MINING, 41);
		add(ItemID.STEEL_PICKAXE, Confidence.MAYBE, Skill.MINING, 41);
		add(ItemID.BLACK_PICKAXE, Confidence.MAYBE, Skill.MINING, 41);
		add(ItemID.MITHRIL_PICKAXE, Confidence.MAYBE, Skill.MINING, 41);
		add(ItemID.ADAMANT_PICKAXE, Confidence.MAYBE, Skill.MINING, 41);

		// Bronze-tier smithing materials; still used for the odd quest, so only a maybe
		add(ItemID.COPPER_ORE, Confidence.MAYBE, Skill.MINING, 60, Skill.SMITHING, 60);
		add(ItemID.TIN_ORE, Confidence.MAYBE, Skill.MINING, 60, Skill.SMITHING, 60);
		add(ItemID.BRONZE_BAR, Confidence.MAYBE, Skill.SMITHING, 60);
	}

	public static final Map<Integer, Entry> ITEMS = BUILDER.build();

	private OutgrownItems()
	{
	}

	private static void add(int itemId, Confidence confidence, Skill skill, int level)
	{
		BUILDER.put(itemId, new Entry(confidence, ImmutableMap.of(skill, level)));
	}

	private static void add(int itemId, Confidence confidence, Skill skill1, int level1, Skill skill2, int level2)
	{
		BUILDER.put(itemId, new Entry(confidence, ImmutableMap.of(skill1, level1, skill2, level2)));
	}
}
