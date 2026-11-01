package com.housekeeping;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SuggestionEngineTest
{
	private final SuggestionEngine engine = new SuggestionEngine();

	private static PlayerState state(Map<Skill, Integer> levels, Set<Quest> quests)
	{
		return new PlayerState(levels, quests);
	}

	private List<Suggestion> evaluate(int itemId, PlayerState state)
	{
		return engine.evaluate(Collections.singletonList(itemId), state);
	}

	@Test
	public void rawShrimpOutgrownAtHighFishingAndCooking()
	{
		PlayerState maxed = state(ImmutableMap.of(Skill.FISHING, 99, Skill.COOKING, 99), Set.of());
		List<Suggestion> result = evaluate(ItemID.RAW_SHRIMP, maxed);

		assertEquals(1, result.size());
		assertEquals(Category.OUTGROWN, result.get(0).getCategory());
		assertEquals(Confidence.HIGH, result.get(0).getConfidence());
	}

	@Test
	public void rawShrimpKeptUntilEverySkillIsMet()
	{
		PlayerState lowCooking = state(ImmutableMap.of(Skill.FISHING, 99, Skill.COOKING, 20), Set.of());
		assertTrue(evaluate(ItemID.RAW_SHRIMP, lowCooking).isEmpty());
	}

	@Test
	public void unknownItemHasNoSuggestion()
	{
		PlayerState maxed = state(ImmutableMap.of(Skill.FISHING, 99, Skill.COOKING, 99), Set.of());
		assertTrue(evaluate(ItemID.COINS, maxed).isEmpty());
	}

	@Test
	public void burntFoodIsJunk()
	{
		List<Suggestion> result = evaluate(ItemID.BURNT_SHARK, state(Map.of(), Set.of()));

		assertEquals(1, result.size());
		assertEquals(Category.JUNK, result.get(0).getCategory());
	}

	@Test
	public void bookcaseBookNeedsFinishedQuest()
	{
		assertTrue(evaluate(ItemID.BAXTORIAN_BOOK_WATERFALL_QUEST, state(Map.of(), Set.of())).isEmpty());

		Set<Quest> done = ImmutableSet.of(Quest.WATERFALL_QUEST);
		List<Suggestion> result = evaluate(ItemID.BAXTORIAN_BOOK_WATERFALL_QUEST, state(Map.of(), done));
		assertEquals(1, result.size());
		assertEquals(Category.JUNK, result.get(0).getCategory());
	}

	@Test
	public void nonQuestBookAlwaysRetrievable()
	{
		List<Suggestion> result = evaluate(ItemID.ABLENKIAN_ESCAPE, state(Map.of(), Set.of()));

		assertEquals(1, result.size());
		assertEquals(Confidence.HIGH, result.get(0).getConfidence());
	}

	@Test
	public void functionalBookIsOnlyAMaybe()
	{
		List<Suggestion> result = evaluate(ItemID.COLLECTION_LOG, state(Map.of(), Set.of()));

		assertEquals(1, result.size());
		assertEquals(Confidence.MAYBE, result.get(0).getConfidence());
	}

	@Test
	public void questItemObsoleteOnceQuestFinished()
	{
		assertTrue(evaluate(ItemID.DOOMION_AMULET, state(Map.of(), Set.of())).isEmpty());

		List<Suggestion> result = evaluate(ItemID.DOOMION_AMULET, state(Map.of(), ImmutableSet.of(Quest.UNDERGROUND_PASS)));
		assertEquals(1, result.size());
		assertEquals(Category.QUEST, result.get(0).getCategory());
	}

	@Test
	public void questItemKeptUntilEveryQuestFinished()
	{
		Set<Quest> onlyOne = ImmutableSet.of(Quest.BIOHAZARD);
		assertTrue(evaluate(ItemID.ROTTENAPPLES, state(Map.of(), onlyOne)).isEmpty());

		Set<Quest> both = ImmutableSet.of(Quest.BIOHAZARD, Quest.MOURNINGS_END_PART_I);
		assertEquals(1, evaluate(ItemID.ROTTENAPPLES, state(Map.of(), both)).size());
	}

	@Test
	public void maybeSuggestionsHiddenUnlessOptedIn()
	{
		Suggestion maybe = new Suggestion(ItemID.STEEL_AXE, Category.OUTGROWN, Confidence.MAYBE, "");
		Set<Category> all = EnumSet.allOf(Category.class);

		assertFalse(SuggestionEngine.isVisible(maybe, all, false));
		assertTrue(SuggestionEngine.isVisible(maybe, all, true));
		assertFalse(SuggestionEngine.isVisible(maybe, EnumSet.of(Category.JUNK), true));
	}
}
