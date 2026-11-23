package com.gielinorgoals;

import com.gielinorgoals.model.DropSource;
import com.gielinorgoals.model.DryMeter;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.template.TemplateNode;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Buggar från kvalitetspasset i milstolpe 10. Varje test misslyckades före sin fix.
 */
public class QualityBugsTest
{
	@Test
	public void goalDoneThroughDoneWhenCannotBeUncheckedAndKeepsItsDate()
	{
		GoalManager manager = torsoTemplate();
		Goal torso = find(manager, "Torso");
		Goal points = find(manager, "Attacker points");
		manager.setCompleted(torso.getId(), true);
		assertTrue(points.isCompleted());
		Long completedAt = points.getCompletedAt();
		boolean approx = points.isCompletedAtApprox();
		long revision = manager.getRevision();

		// Förut gick avbockningen igenom, och utvärderingen bockade av målet igen direkt med
		// dagens datum som ungefärligt.
		manager.setCompleted(points.getId(), false);

		assertTrue(points.isCompleted());
		assertEquals(completedAt, points.getCompletedAt());
		assertEquals(approx, points.isCompletedAtApprox());
		// Inget ändrades, så inget sparades.
		assertEquals(revision, manager.getRevision());
	}

	@Test
	public void goalsDoneBecauseYouTickedALaterGoalGetAnExactDate()
	{
		GoalManager manager = torsoTemplate();
		Goal torso = find(manager, "Torso");
		Goal points = find(manager, "Attacker points");

		// Förut markerades de som ungefärliga, som om de varit klara redan innan.
		manager.setCompleted(torso.getId(), true);

		assertTrue(points.isCompleted());
		assertFalse(points.isCompletedAtApprox());
		assertTrue(manager.isDoneByOtherGoal(points));
		assertFalse(manager.isDoneByOtherGoal(torso));
	}

	// En template där "Attacker points" blir klart när "Torso" är klart.
	private static GoalManager torsoTemplate()
	{
		GoalManager manager = TestGoalManagers.inMemory("profile");
		TemplateNode root = TemplateNode.aggregate("Fighter torso");
		TemplateNode torso = new TemplateNode("Torso", null, false);
		TemplateNode points = new TemplateNode("Attacker points", null, false);
		points.setDoneWhen(List.of("group:torso"));
		root.add(torso).add(points);
		manager.insertTemplate(root);
		return manager;
	}

	@Test
	public void killCountFoundAtLoginIsShownAsApproximate()
	{
		Goal goal = new Goal("");
		goal.setRequirement(new ItemRequirement(13576, "Dragon warhammer", 1, false));
		goal.setDropSource(DropSource.npc("Lizardman shaman", 3000));
		goal.markCompleted(true, 1L);
		goal.setObtainedAtKc(1240L);
		assertEquals("Got it at 1,240 kc", DryMeter.completedText(goal));

		// Klarad offline och upptäckt vid inloggning: KC är räknarens värde då, inte vid dropen.
		goal.markCompleted(true, 1L, true);
		goal.setObtainedAtKc(1240L);
		assertEquals("Got it at about 1,240 kc", DryMeter.completedText(goal));
	}

	private static Goal find(GoalManager manager, String title)
	{
		for (Goal goal : manager.getTree().flatten())
		{
			if (title.equals(goal.getTitle()))
			{
				return goal;
			}
		}
		throw new AssertionError(title);
	}
}
