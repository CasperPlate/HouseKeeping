package com.housekeeping.rules;

import com.housekeeping.Category;
import com.housekeeping.Confidence;
import com.housekeeping.PlayerState;
import com.housekeeping.Suggestion;
import com.housekeeping.data.QuestItems;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;

public class QuestItemRule implements SuggestionRule
{
	@Override
	public Suggestion evaluate(int itemId, PlayerState state)
	{
		Set<Quest> quests = QuestItems.ITEMS.get(itemId);
		if (quests == null || !quests.stream().allMatch(state::isFinished))
		{
			return null;
		}

		String names = quests.stream().map(Quest::getName).collect(Collectors.joining(", "));
		return new Suggestion(itemId, Category.QUEST, Confidence.HIGH,
			"No further use now that " + names + (quests.size() > 1 ? " are" : " is") + " complete");
	}
}
