package com.gielinorgoals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * En dialog som var öppen när profilen byttes ska inte ändra det nya kontots mål.
 * Före fixen användes resultatet alltid, oavsett vilken profil som laddats under tiden.
 */
public class ProfileGuardTest
{
	@Test
	public void guardNoticesProfileSwitchAndLogout()
	{
		GoalManager manager = TestGoalManagers.inMemory("first");
		ProfileGuard guard = ProfileGuard.of(manager);
		assertTrue(guard.isCurrent());

		manager.loadProfile("second");
		assertFalse(guard.isCurrent());

		// Tillbaka på samma konto går det bra igen.
		manager.loadProfile("first");
		assertTrue(guard.isCurrent());

		manager.unload();
		assertFalse(guard.isCurrent());
	}
}
