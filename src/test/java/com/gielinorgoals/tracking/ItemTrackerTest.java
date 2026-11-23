package com.gielinorgoals.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.List;
import java.util.Map;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class ItemTrackerTest
{
	private static final int SHARK = 385;
	private static final int SHARK_NOTED = 386;
	private static final int AIR_RUNE = ItemID.AIRRUNE;
	private static final int BLOOD_RUNE = ItemID.BLOODRUNE;

	@Test
	public void countMergesNotedAndSkipsEmptySlots()
	{
		Item[] items = {
			new Item(SHARK, 5),
			new Item(SHARK_NOTED, 100),
			new Item(-1, 0),
			// Placeholder i banken: id finns men antal 0.
			new Item(995, 0),
			null,
		};

		Map<Integer, Long> counts = ItemTracker.count(items, id -> id == SHARK_NOTED ? SHARK : id);

		assertEquals(Long.valueOf(105L), counts.get(SHARK));
		assertFalse(counts.containsKey(995));
		assertEquals(1, counts.size());
	}

	@Test
	public void sumAddsAllSources()
	{
		Map<Integer, Long> totals = ItemTracker.sum(List.of(
			Map.of(SHARK, 10L),
			Map.of(SHARK, 5L, 995, 1000L),
			Map.of()));

		assertEquals(Long.valueOf(15L), totals.get(SHARK));
		assertEquals(Long.valueOf(1000L), totals.get(995));
	}

	@Test
	public void runePouchSlotsBecomeRunes()
	{
		// Typ 1 = luftruna, 2 = blodruna i testets uppslag; fack 3 är tomt, fack 4 har antal 0.
		Map<Integer, Integer> lookup = Map.of(1, AIR_RUNE, 2, BLOOD_RUNE);
		Map<Integer, Long> contents = ItemTracker.runePouchContents(
			new int[]{2, 1, 0, 1, 2, 0},
			new int[]{4000, 100, 50, 0, 16000, 0},
			type -> lookup.getOrDefault(type, -1));

		assertEquals(Long.valueOf(20000L), contents.get(BLOOD_RUNE));
		assertEquals(Long.valueOf(100L), contents.get(AIR_RUNE));
		assertEquals(2, contents.size());
	}

	@Test
	public void unknownRuneTypeIsSkipped()
	{
		Map<Integer, Long> contents = ItemTracker.runePouchContents(
			new int[]{99}, new int[]{10}, type -> -1);

		assertTrue(contents.isEmpty());
	}

	@Test
	public void snapshotReportsUnknownItemsAsMinusOne()
	{
		GameSnapshot unknown = new GameSnapshot(Map.of(), GameSnapshot.Items.UNKNOWN, AchievementTracker.Achievements.NONE);
		GameSnapshot known = new GameSnapshot(Map.of(), new GameSnapshot.Items(Map.of(SHARK, 3L), 42L),
			AchievementTracker.Achievements.NONE);

		assertEquals(-1, unknown.getItemCount(SHARK));
		assertEquals(3, known.getItemCount(SHARK));
		assertEquals(0, known.getItemCount(995));
		assertEquals(Long.valueOf(42L), known.getBankSeenAt());
	}
}
