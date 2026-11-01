package com.housekeeping;

import java.util.Map;
import java.util.Set;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Snapshot of the account facts the suggestion rules depend on. Built on the client thread,
 * then read freely so the rules stay independent of {@link net.runelite.api.Client}.
 */
@Value
public class PlayerState
{
	Map<Skill, Integer> levels;
	Set<Quest> finishedQuests;

	public int getLevel(Skill skill)
	{
		return levels.getOrDefault(skill, 1);
	}

	public boolean isFinished(Quest quest)
	{
		return finishedQuests.contains(quest);
	}
}
