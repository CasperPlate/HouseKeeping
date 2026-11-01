package com.housekeeping;

import lombok.Value;

/**
 * A suggestion resolved against the bank snapshot, ready for display.
 */
@Value
class ItemSuggestion
{
	Suggestion suggestion;
	String name;
	int quantity;
}
