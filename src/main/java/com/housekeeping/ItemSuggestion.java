package com.housekeeping;

import javax.annotation.Nullable;
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
	/**
	 * Value of the whole stack, using the higher of the GE price and high alch value.
	 */
	long value;
	/**
	 * "GE" or "HA", naming where {@link #value} came from; null when the item is worthless.
	 */
	@Nullable
	String valueSource;
}
