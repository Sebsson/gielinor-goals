package com.gielinorgoals.tracking;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SkillTrackerTest
{
	private GameState state = GameState.LOGGED_IN;
	private final Map<Skill, Integer> client = new EnumMap<>(Skill.class);
	private final SkillTracker tracker = new SkillTracker(() -> state, skill -> client.getOrDefault(skill, 0));

	@Test
	public void readsEverySkillAfterLoginAndThenOnlyReportsChanges()
	{
		client.put(Skill.COOKING, 1_000_000);
		tracker.onGameStateChanged(GameState.LOGGED_IN);

		Map<Skill, Long> first = tracker.onGameTick();
		assertNotNull(first);
		assertEquals(Long.valueOf(1_000_000L), first.get(Skill.COOKING));
		assertEquals(Long.valueOf(0L), first.get(Skill.ATTACK));
		assertFalse(first.containsKey(Skill.OVERALL));
		// Inget nytt: ingen ny ögonblicksbild, så att målen inte utvärderas i onödan.
		assertNull(tracker.onGameTick());

		tracker.onStatChanged(Skill.COOKING, 1_000_500);
		assertEquals(Long.valueOf(1_000_500L), tracker.onGameTick().get(Skill.COOKING));
		assertNull(tracker.onGameTick());
	}

	@Test
	public void fullReadWaitsUntilLoggedIn()
	{
		state = GameState.LOADING;
		tracker.onGameStateChanged(GameState.LOGGED_IN);
		client.put(Skill.MINING, 500);

		// Inget att rapportera förrän klienten verkligen är inloggad.
		assertNull(tracker.onGameTick());

		state = GameState.LOGGED_IN;
		assertEquals(Long.valueOf(500L), tracker.onGameTick().get(Skill.MINING));
	}

	@Test
	public void logoutForgetsThePreviousAccount()
	{
		tracker.onStatChanged(Skill.SLAYER, 13_034_431);
		tracker.onGameTick();

		tracker.onGameStateChanged(GameState.LOGIN_SCREEN);
		Map<Skill, Long> afterLogout = tracker.onGameTick();

		assertNotNull(afterLogout);
		assertTrue(afterLogout.isEmpty());
	}
}
