package com.gielinorgoals.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.util.List;
import java.util.Map;
import net.runelite.api.Item;
import org.junit.Test;

public class ItemTrackerTest
{
	private static final int SHARK = 385;
	private static final int SHARK_NOTED = 386;

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
