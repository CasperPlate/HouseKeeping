package com.housekeeping;

import com.google.common.collect.ImmutableList;
import com.housekeeping.rules.BookcaseRule;
import com.housekeeping.rules.JunkRule;
import com.housekeeping.rules.QuestItemRule;
import com.housekeeping.rules.SkillOutgrownRule;
import com.housekeeping.rules.SuggestionRule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SuggestionEngine
{
	// First matching rule wins, so an item only ever gets one suggestion
	private final List<SuggestionRule> rules = ImmutableList.of(
		new BookcaseRule(),
		new JunkRule(),
		new QuestItemRule(),
		new SkillOutgrownRule()
	);

	public List<Suggestion> evaluate(Collection<Integer> itemIds, PlayerState state)
	{
		List<Suggestion> suggestions = new ArrayList<>();
		for (int itemId : itemIds)
		{
			for (SuggestionRule rule : rules)
			{
				Suggestion suggestion = rule.evaluate(itemId, state);
				if (suggestion != null)
				{
					suggestions.add(suggestion);
					break;
				}
			}
		}
		return suggestions;
	}

	public static boolean isVisible(Suggestion suggestion, Set<Category> enabledCategories, boolean showMaybe)
	{
		return enabledCategories.contains(suggestion.getCategory())
			&& (showMaybe || suggestion.getConfidence() == Confidence.HIGH);
	}
}
