package com.gielinorgoals.ui;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ItemIndexBuilderTest
{
	@Test
	public void buildsInChunksAndKeepsTheLowestIdPerName()
	{
		String[] names = {"Shark", null, "Dragon pickaxe", "shark", "Coins", null, "Dragon pickaxe"};
		ItemSearch.IndexBuilder builder = new ItemSearch.IndexBuilder(names.length, id -> names[id]);

		// Tre per varv: klart först efter tredje varvet.
		assertFalse(builder.step(3));
		assertFalse(builder.step(3));
		assertTrue(builder.step(3));

		List<ItemSearch.ItemOption> index = builder.result();
		assertEquals(List.of(new ItemSearch.ItemOption(0, "Shark"), new ItemSearch.ItemOption(2, "Dragon pickaxe"),
			new ItemSearch.ItemOption(4, "Coins")), index);
	}

	@Test
	public void emptyCacheIsDoneAtOnce()
	{
		assertTrue(new ItemSearch.IndexBuilder(0, id -> "x").step(ItemSearch.INDEX_CHUNK));
	}
}
