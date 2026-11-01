package com.housekeeping.rules;

import com.housekeeping.Category;
import com.housekeeping.Confidence;
import com.housekeeping.PlayerState;
import com.housekeeping.Suggestion;
import com.housekeeping.data.PohStorage;
import java.util.Set;

public class PohStorageRule implements SuggestionRule
{
	@Override
	public Suggestion evaluate(int itemId, PlayerState state)
	{
		Set<PohStorage.Storage> storages = PohStorage.ITEMS.get(itemId);
		if (storages == null)
		{
			return null;
		}

		for (PohStorage.Storage storage : storages)
		{
			if (state.getPohStorage().contains(storage))
			{
				return new Suggestion(itemId, Category.POH, Confidence.HIGH,
					"Can be stored in your POH " + storage.getDisplayName());
			}
		}
		return null;
	}
}
