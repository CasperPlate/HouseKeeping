package com.housekeeping;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category
{
	OUTGROWN("Outgrown skilling items"),
	QUEST("Obsolete quest items"),
	JUNK("Junk / retrievable");

	private final String displayName;
}
