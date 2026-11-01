package com.housekeeping;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ItemSearchTest
{
	private static int score(String query, String name)
	{
		return ItemSearch.score(query, name, "");
	}

	@Test
	public void ignoresCaseSpacesAndPunctuation()
	{
		assertEquals(100, score("FIRE CAPE", "Fire cape"));
		assertEquals(100, score("firecape", "Fire cape"));
		assertEquals(100, score("adamant helm h5", "Adamant helm (h5)"));
		assertTrue(score("gianne", "Gianne's cook book") > ItemSearch.NO_MATCH);
	}

	@Test
	public void matchesPartOfLongerNames()
	{
		assertEquals(95, score("spottier", "Spottier cape"));
		assertEquals(90, score("cape", "Spottier cape"));
	}

	@Test
	public void matchesWordsInAnyOrder()
	{
		assertEquals(80, score("cape fire", "Fire cape"));
	}

	@Test
	public void matchesInitials()
	{
		assertEquals(70, score("bdm", "Blue dragon mask"));
	}

	@Test
	public void toleratesTypos()
	{
		assertTrue(score("spotier cape", "Spottier cape") > ItemSearch.NO_MATCH);
		assertTrue(score("baguete", "Stale baguette") > ItemSearch.NO_MATCH);
		assertTrue(score("dragn mask", "Blue dragon mask") > ItemSearch.NO_MATCH);
		// Short words must match exactly, or "axe" would also find "ace"
		assertEquals(ItemSearch.NO_MATCH, score("axe", "Ace of spades"));
	}

	@Test
	public void closerMatchesRankHigher()
	{
		assertTrue(score("fire cape", "Fire cape") > score("fire cap", "Fire cape"));
		assertTrue(score("cape", "Fire cape") > score("cape", "Bronze axe"));
		assertTrue(score("baguette", "Stale baguette") > score("baguete", "Stale baguette"));
	}

	@Test
	public void fallsBackToTheReason()
	{
		assertEquals(50, ItemSearch.score("watchtower", "Old robe", "No further use now that Watchtower is complete"));
		assertEquals(50, ItemSearch.score("toy box", "Bunny ears", "Can be stored in your POH toy box"));
		// A name match always outranks a reason match
		assertTrue(score("cape", "Fire cape") > ItemSearch.score("cape rack", "Bunny ears", "...cape rack"));
	}

	@Test
	public void typoMatchesStayBelowExactOnes()
	{
		// "cane" is one letter off "cape", so it only counts as a typo match the panel can hide
		int typo = score("cape", "Rune cane");
		assertTrue(typo > ItemSearch.NO_MATCH && typo <= ItemSearch.TYPO_MATCH_MAX);
		assertTrue(score("cape", "Fire cape") > ItemSearch.TYPO_MATCH_MAX);
		assertTrue(score("bdm", "Blue dragon mask") > ItemSearch.TYPO_MATCH_MAX);
		assertTrue(ItemSearch.score("cape rack", "Bunny ears", "...cape rack") > ItemSearch.TYPO_MATCH_MAX);
		assertTrue(score("spotier cape", "Spottier cape") <= ItemSearch.TYPO_MATCH_MAX);
	}

	@Test
	public void emptyQueryMatchesNothing()
	{
		assertEquals(ItemSearch.NO_MATCH, score("   ", "Fire cape"));
	}

	@Test
	public void substringDistanceFindsBestWindow()
	{
		assertEquals(0, ItemSearch.substringDistance("cape", "spottiercape"));
		assertEquals(1, ItemSearch.substringDistance("spotier", "spottiercape"));
	}
}
