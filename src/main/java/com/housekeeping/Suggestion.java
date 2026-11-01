package com.housekeeping;

import lombok.Value;

@Value
public class Suggestion
{
	int itemId;
	Category category;
	Confidence confidence;
	String reason;
}
