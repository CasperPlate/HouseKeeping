package com.housekeeping.rules;

import com.housekeeping.PlayerState;
import com.housekeeping.Suggestion;
import javax.annotation.Nullable;

public interface SuggestionRule
{
	/**
	 * @return a suggestion for the item, or null if this rule has nothing to say about it
	 */
	@Nullable
	Suggestion evaluate(int itemId, PlayerState state);
}
