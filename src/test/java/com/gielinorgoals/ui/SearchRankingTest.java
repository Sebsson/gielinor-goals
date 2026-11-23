package com.gielinorgoals.ui;

import java.util.List;
import java.util.function.Function;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SearchRankingTest
{
	private static final List<String> ITEMS = List.of("Rune pouch", "Divine rune pouch", "Pouch", "Rune platebody",
		"Small pouch");

	@Test
	public void exactThenPrefixThenContainsIgnoringCase()
	{
		assertEquals(List.of("Pouch", "Rune pouch", "Divine rune pouch", "Small pouch"), rank("POUCH", 10));
		assertEquals(List.of("Rune pouch", "Rune platebody", "Divine rune pouch"), rank(" rune ", 10));
	}

	@Test
	public void resultsAreCappedAndEmptyQueryListsTheStart()
	{
		assertEquals(List.of("Pouch", "Rune pouch"), rank("pouch", 2));
		assertEquals(List.of("Rune pouch", "Divine rune pouch"), rank("", 2));
		assertEquals(List.of(), rank("dragon", 10));
	}

	private static List<String> rank(String query, int max)
	{
		return SearchRanking.rank(ITEMS, Function.identity(), query, max);
	}
}
