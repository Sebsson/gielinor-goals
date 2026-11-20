package com.gielinorgoals.tracking;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.gielinorgoals.model.CombatAchievementRequirement;
import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.QuestRequirement;
import java.util.Map;
import net.runelite.api.QuestState;
import org.junit.Test;

public class CompletionStatusTest
{
	@Test
	public void unknownStatusNeverReportsComplete()
	{
		assertFalse(CompletionStatus.UNKNOWN.isAlreadyComplete(new QuestRequirement(1, "Quest")));
		assertFalse(CompletionStatus.UNKNOWN.isAlreadyComplete(new CombatAchievementRequirement(0, "Task", "Easy")));
	}

	@Test
	public void knownStatusUsesRequirementRules()
	{
		int karamjaEasy = DiaryArea.KARAMJA.varbitFor(DiaryTier.EASY);
		CompletionStatus status = new CompletionStatus(true,
			Map.of(1, QuestState.FINISHED, 2, QuestState.IN_PROGRESS),
			Map.of(karamjaEasy, 1),
			new int[]{1 << 3});

		assertTrue(status.isAlreadyComplete(new QuestRequirement(1, "Done")));
		assertFalse(status.isAlreadyComplete(new QuestRequirement(2, "Started")));
		// Karamja easy kräver värdet 2, så 1 räknas inte som klar.
		assertFalse(status.isAlreadyComplete(new DiaryRequirement(DiaryArea.KARAMJA, DiaryTier.EASY)));
		assertTrue(status.isAlreadyComplete(new CombatAchievementRequirement(3, "Task", "Easy")));
		assertFalse(status.isAlreadyComplete(new CombatAchievementRequirement(4, "Task", "Easy")));
	}
}
