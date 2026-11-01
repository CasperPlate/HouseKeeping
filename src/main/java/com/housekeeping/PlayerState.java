package com.housekeeping;

import com.housekeeping.data.PohStorage;
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
	/**
	 * Costume room storage the player has told us they built.
	 */
	Set<PohStorage.Storage> pohStorage;

	public int getLevel(Skill skill)
	{
		return levels.getOrDefault(skill, 1);
	}

	public boolean isFinished(Quest quest)
	{
		return finishedQuests.contains(quest);
	}
}
