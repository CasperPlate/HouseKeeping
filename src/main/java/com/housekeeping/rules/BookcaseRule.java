package com.housekeeping.rules;

import com.housekeeping.Category;
import com.housekeeping.PlayerState;
import com.housekeeping.Suggestion;
import com.housekeeping.data.BookcaseBooks;
import net.runelite.api.Quest;

public class BookcaseRule implements SuggestionRule
{
	@Override
	public Suggestion evaluate(int itemId, PlayerState state)
	{
		BookcaseBooks.Entry entry = BookcaseBooks.ITEMS.get(itemId);
		if (entry == null)
		{
			return null;
		}

		Quest quest = entry.getQuest();
		if (quest != null && !state.isFinished(quest))
		{
			return null;
		}

		String reason = quest != null
			? quest.getName() + " is complete: drop it and take it back from a POH bookcase when needed"
			: "A POH bookcase keeps this book: drop it and take it back when needed";
		return new Suggestion(itemId, Category.JUNK, entry.getConfidence(), reason);
	}
}
