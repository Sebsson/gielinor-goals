package com.gielinorgoals.ui;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ItemSearchTest
{
	private static final List<ItemSearch.ItemOption> INDEX = List.of(
		new ItemSearch.ItemOption(1, "Dragon pickaxe (or)"),
		new ItemSearch.ItemOption(2, "Pickaxe handle"),
		new ItemSearch.ItemOption(3, "Dragon pickaxe"),
		new ItemSearch.ItemOption(4, "Shark"));

	@Test
	public void exactThenPrefixThenContains()
	{
		List<String> names = names(ItemSearch.filter(INDEX, "dragon pickaxe"));
		assertEquals(List.of("Dragon pickaxe", "Dragon pickaxe (or)"), names);

		assertEquals(List.of("Pickaxe handle", "Dragon pickaxe (or)", "Dragon pickaxe"),
			names(ItemSearch.filter(INDEX, "pickaxe")));
	}

	@Test
	public void resultsAreCapped()
	{
		List<ItemSearch.ItemOption> big = new ArrayList<>();
		for (int i = 0; i < 200; i++)
		{
			big.add(new ItemSearch.ItemOption(i, "Rune " + i));
		}
		assertEquals(ItemSearch.MAX_RESULTS, ItemSearch.filter(big, "rune").size());
	}

	private static List<String> names(List<ItemSearch.ItemOption> options)
	{
		List<String> names = new ArrayList<>();
		options.forEach(option -> names.add(option.getName()));
		return names;
	}
}
