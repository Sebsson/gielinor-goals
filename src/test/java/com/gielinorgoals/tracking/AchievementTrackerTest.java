package com.gielinorgoals.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.gielinorgoals.model.CombatAchievementRequirement;
import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.QuestRequirement;
import java.util.Set;
import org.junit.Test;

public class AchievementTrackerTest
{
	@Test
	public void taskBitIsReadFromCorrectVarp()
	{
		// Uppgift 37 = bit 5 i varp nummer 1.
		assertTrue(AchievementTracker.isTaskComplete(37, index -> index == 1 ? 1 << 5 : 0));
		assertFalse(AchievementTracker.isTaskComplete(37, index -> index == 0 ? 1 << 5 : 0));
		// Bit 31 (tecken-biten) ska också fungera.
		assertTrue(AchievementTracker.isTaskComplete(31, index -> Integer.MIN_VALUE));
	}

	@Test
	public void taskOutsideKnownVarpsIsUnknown()
	{
		assertNull(AchievementTracker.isTaskComplete(AchievementTracker.CA_VARPS.length * 32, index -> -1));
		assertNull(AchievementTracker.isTaskComplete(-1, index -> -1));
	}

	@Test
	public void watchedCollectsRequirementsFromWholeTree()
	{
		GoalTree tree = new GoalTree();
		Goal parent = goal(new QuestRequirement(7, "Quest"));
		tree.add(null, parent);
		tree.add(parent.getId(), goal(new DiaryRequirement(DiaryArea.FALADOR, DiaryTier.EASY)));
		tree.add(parent.getId(), goal(new CombatAchievementRequirement(99, "Task", "Hard")));
		tree.add(null, new Goal("Manual"));

		WatchedAchievements watched = WatchedAchievements.from(tree);

		assertEquals(Set.of(7), watched.getQuestIds());
		assertEquals(Set.of(DiaryArea.FALADOR.varbitFor(DiaryTier.EASY), DiaryArea.FALADOR.countVarbitFor(DiaryTier.EASY)),
			watched.getVarbits());
		assertEquals(Set.of(99), watched.getCombatAchievementIds());
		assertTrue(WatchedAchievements.from(new GoalTree()).isEmpty());
	}

	private static Goal goal(com.gielinorgoals.model.Requirement requirement)
	{
		Goal goal = new Goal("");
		goal.setRequirement(requirement);
		return goal;
	}

	@Test
	public void snapshotReportsQuestPointsOrUnknown()
	{
		AchievementTracker.Achievements achievements = new AchievementTracker.Achievements(
			java.util.Collections.emptyMap(), java.util.Collections.emptyMap(), java.util.Collections.emptyMap(), 142,
			java.util.Collections.emptyMap());

		assertEquals(142, new GameSnapshot(java.util.Collections.emptyMap(), GameSnapshot.Items.UNKNOWN, achievements)
			.getQuestPoints());
		assertEquals(-1, new GameSnapshot(java.util.Collections.emptyMap(), GameSnapshot.Items.UNKNOWN,
			AchievementTracker.Achievements.NONE).getQuestPoints());
	}
}
