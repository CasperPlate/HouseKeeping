package com.housekeeping.rules;

import com.housekeeping.Category;
import com.housekeeping.Confidence;
import com.housekeeping.PlayerState;
import com.housekeeping.Suggestion;
import com.housekeeping.data.JunkItems;
import net.runelite.api.Quest;

public class JunkRule implements SuggestionRule
{
	@Override
	public Suggestion evaluate(int itemId, PlayerState state)
	{
		JunkItems.Entry entry = JunkItems.ITEMS.get(itemId);
		if (entry == null)
		{
			return null;
		}

		Quest quest = entry.getNeededFor();
		if (quest != null && state.isFinished(quest))
		{
			return new Suggestion(itemId, Category.JUNK, Confidence.HIGH,
				"No further use now that " + quest.getName() + " is complete");
		}
		return new Suggestion(itemId, Category.JUNK, entry.getConfidence(), entry.getReason());
	}
}
