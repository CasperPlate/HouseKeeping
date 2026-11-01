package com.housekeeping.rules;

import com.housekeeping.Category;
import com.housekeeping.PlayerState;
import com.housekeeping.Suggestion;
import com.housekeeping.data.OutgrownItems;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.Skill;

public class SkillOutgrownRule implements SuggestionRule
{
	@Override
	public Suggestion evaluate(int itemId, PlayerState state)
	{
		OutgrownItems.Entry entry = OutgrownItems.ITEMS.get(itemId);
		if (entry == null)
		{
			return null;
		}

		for (Map.Entry<Skill, Integer> level : entry.getLevels().entrySet())
		{
			if (state.getLevel(level.getKey()) < level.getValue())
			{
				return null;
			}
		}

		String levels = entry.getLevels().entrySet().stream()
			.map(e -> e.getValue() + "+ " + e.getKey().getName())
			.collect(Collectors.joining(" and "));
		return new Suggestion(itemId, Category.OUTGROWN, entry.getConfidence(), "Outgrown: you have " + levels);
	}
}
