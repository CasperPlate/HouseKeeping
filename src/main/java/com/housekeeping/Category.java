package com.housekeeping;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category
{
	OUTGROWN("Outgrown skilling items"),
	JUNK("Junk / retrievable");

	private final String displayName;
}
