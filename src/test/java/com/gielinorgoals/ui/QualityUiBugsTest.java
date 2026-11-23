package com.gielinorgoals.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Buggar från kvalitetspasset i milstolpe 10 som gällde UI-lagret. Logiken bröts ut ur
 * kartfönstret respektive CA-katalogen för att kunna testas; före fixen fanns den inne i
 * klasserna och betedde sig som beskrivet i kommentarerna.
 */
public class QualityUiBugsTest
{
	@Test
	public void switchingProfileDoesNotMakeEveryCompletedGoalPulse()
	{
		CompletionWatcher watcher = new CompletionWatcher();
		assertTrue(watcher.update("first", Set.of("a")).isEmpty());
		assertEquals(Set.of("b"), watcher.update("first", Set.of("a", "b")));

		// Förut jämfördes det nya kontots klara mål med det gamla kontots, så alla pulserade.
		assertTrue(watcher.update("second", Set.of("x", "y", "z")).isEmpty());
		assertEquals(Set.of("w"), watcher.update("second", Set.of("x", "y", "z", "w")));

		watcher.reset();
		assertTrue(watcher.update("second", Set.of("x", "y", "z", "w", "v")).isEmpty());
	}

	@Test
	public void emptyCombatAchievementListIsReadAgainNextTime()
	{
		List<List<CombatAchievementCatalog.Task>> answers = new ArrayList<>();
		answers.add(List.of());
		answers.add(List.of(new CombatAchievementCatalog.Task(17, "Noxious Foe", "Kill an Aberrant Spectre", "Easy")));
		int[] calls = {0};
		CombatAchievementCatalog catalog = new CombatAchievementCatalog(() -> answers.get(Math.min(calls[0]++, 1)));

		// Förut sparades den tomma listan, och sökningen förblev tom tills klienten startades om.
		assertTrue(catalog.tasks().isEmpty());
		assertEquals(1, catalog.tasks().size());
		assertEquals(1, catalog.tasks().size());
		assertEquals(2, calls[0]);
	}
}
