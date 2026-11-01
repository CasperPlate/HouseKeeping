package com.housekeeping.rules;

import com.housekeeping.Category;
import com.housekeeping.PlayerState;
import com.housekeeping.Suggestion;
import com.housekeeping.data.JunkItems;

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
		return new Suggestion(itemId, Category.JUNK, entry.getConfidence(), entry.getReason());
	}
}
