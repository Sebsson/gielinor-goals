package com.gielinorgoals.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemVariationMapping;
import org.junit.Test;

public class ItemRequirementTest
{
	@Test
	public void countsAgainstQuantity()
	{
		ItemRequirement requirement = new ItemRequirement(ItemID.SHARK, "Shark", 100, false);

		GoalProgress partial = requirement.progress(items(ItemID.SHARK, 37));
		assertFalse(partial.isComplete());
		assertEquals("37/100", partial.getLabel());

		assertTrue(requirement.progress(items(ItemID.SHARK, 100)).isComplete());
	}

	@Test
	public void missingItemCountsAsZero()
	{
		GoalProgress progress = new ItemRequirement(ItemID.SHARK, "Shark", 1, false).progress(items(ItemID.COINS, 5));
		assertEquals(0, progress.getCurrent());
		assertFalse(progress.isComplete());
	}

	@Test
	public void unknownItemDataGivesNoProgress()
	{
		assertNull(new ItemRequirement(ItemID.SHARK, "Shark", 1, false).progress(GameData.EMPTY));
	}

	@Test
	public void variantsAreCountedOnlyWhenEnabled()
	{
		// Hitta ett föremål som faktiskt har varianter i klientens tabell.
		int base = ItemVariationMapping.map(ItemID.AMULET_OF_GLORY_6);
		int variant = ItemVariationMapping.getVariations(base).stream()
			.filter(id -> id != ItemID.AMULET_OF_GLORY_6)
			.findFirst()
			.orElseThrow(AssertionError::new);

		GameData data = items(variant, 1);
		assertFalse(new ItemRequirement(ItemID.AMULET_OF_GLORY_6, "Glory", 1, false).progress(data).isComplete());
		assertTrue(new ItemRequirement(ItemID.AMULET_OF_GLORY_6, "Glory", 1, true).progress(data).isComplete());
	}

	@Test
	public void describeIncludesQuantityAboveOne()
	{
		assertEquals("Fire cape", new ItemRequirement(ItemID.TZHAAR_CAPE_FIRE, "Fire cape", 1, false).describe());
		assertEquals("1,000 x Shark", new ItemRequirement(ItemID.SHARK, "Shark", 1000, false).describe());
	}

	@Test(expected = IllegalArgumentException.class)
	public void zeroQuantityIsRejected()
	{
		new ItemRequirement(ItemID.SHARK, "Shark", 0, false);
	}

	private static GameData items(int itemId, long count)
	{
		Map<Integer, Long> map = new HashMap<>();
		map.put(itemId, count);
		return new GameData()
		{
			@Override
			public long getXp(net.runelite.api.Skill skill)
			{
				return -1;
			}

			@Override
			public long getItemCount(int id)
			{
				return map.getOrDefault(id, 0L);
			}
		};
	}
}
