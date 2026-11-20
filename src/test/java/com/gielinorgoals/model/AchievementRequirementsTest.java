package com.gielinorgoals.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Map;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

public class AchievementRequirementsTest
{
	@Test
	public void questIsCompleteOnlyWhenFinished()
	{
		QuestRequirement requirement = new QuestRequirement(42, "Dragon Slayer I");

		assertEquals("Not started", requirement.progress(quest(42, QuestState.NOT_STARTED)).getLabel());
		GoalProgress inProgress = requirement.progress(quest(42, QuestState.IN_PROGRESS));
		assertFalse(inProgress.isComplete());
		assertEquals(0.5, inProgress.fraction(), 0.0);
		assertTrue(requirement.progress(quest(42, QuestState.FINISHED)).isComplete());
		assertNull(requirement.progress(GameData.EMPTY));
		assertEquals("Complete Dragon Slayer I", requirement.describe());
	}

	@Test
	public void regularDiaryTierIsCompleteAtOne()
	{
		DiaryRequirement requirement = new DiaryRequirement(DiaryArea.VARROCK, DiaryTier.HARD);
		assertEquals(VarbitID.VARROCK_DIARY_HARD_COMPLETE, requirement.varbit());

		assertFalse(requirement.progress(varbit(requirement.varbit(), 0)).isComplete());
		assertTrue(requirement.progress(varbit(requirement.varbit(), 1)).isComplete());
		assertEquals("Varrock Hard diary", requirement.describe());
	}

	@Test
	public void karamjaLowTiersNeedTwoButEliteNeedsOne()
	{
		DiaryRequirement easy = new DiaryRequirement(DiaryArea.KARAMJA, DiaryTier.EASY);
		assertEquals(VarbitID.ATJUN_EASY_DONE, easy.varbit());
		assertFalse(easy.progress(varbit(easy.varbit(), 1)).isComplete());
		assertTrue(easy.progress(varbit(easy.varbit(), 2)).isComplete());

		DiaryRequirement elite = new DiaryRequirement(DiaryArea.KARAMJA, DiaryTier.ELITE);
		assertEquals(VarbitID.KARAMJA_DIARY_ELITE_COMPLETE, elite.varbit());
		assertTrue(elite.progress(varbit(elite.varbit(), 1)).isComplete());
	}

	@Test
	public void diaryShowsTaskCountWhenKnown()
	{
		DiaryRequirement requirement = new DiaryRequirement(DiaryArea.VARROCK, DiaryTier.EASY);
		GameData data = varbits(Map.of(requirement.varbit(), 0, requirement.countVarbit(), 7));

		GoalProgress progress = requirement.progress(data);

		assertEquals("7/14 tasks", progress.getLabel());
		assertEquals(0.5, progress.fraction(), 0.0);
		assertFalse(progress.isComplete());
	}

	@Test
	public void completedDiaryShowsFullCount()
	{
		DiaryRequirement requirement = new DiaryRequirement(DiaryArea.ARDOUGNE, DiaryTier.ELITE);
		GoalProgress progress = requirement.progress(varbits(Map.of(requirement.varbit(), 1, requirement.countVarbit(), 3)));

		assertTrue(progress.isComplete());
		assertEquals("8/8 tasks", progress.getLabel());
	}

	@Test
	public void journalTitleMapsToArea()
	{
		assertEquals(DiaryArea.VARROCK, DiaryArea.fromJournalTitle("Varrock Area Tasks"));
		assertEquals(DiaryArea.KOUREND, DiaryArea.fromJournalTitle("<col=7f0000>Kourend & Kebos Tasks</col>"));
		assertEquals(DiaryArea.LUMBRIDGE, DiaryArea.fromJournalTitle("Lumbridge & Draynor Tasks"));
		assertNull(DiaryArea.fromJournalTitle("Cook's Assistant"));
	}

	@Test
	public void unreadDiaryVarbitGivesNoProgress()
	{
		assertNull(new DiaryRequirement(DiaryArea.DESERT, DiaryTier.EASY).progress(GameData.EMPTY));
	}

	@Test
	public void combatAchievementFollowsCompletionFlag()
	{
		CombatAchievementRequirement requirement = new CombatAchievementRequirement(17, "Noxious Foe", "Easy");

		assertFalse(requirement.progress(ca(17, false)).isComplete());
		assertTrue(requirement.progress(ca(17, true)).isComplete());
		assertNull(requirement.progress(GameData.EMPTY));
		assertEquals("CA (Easy): Noxious Foe", requirement.describe());
	}

	private static GameData quest(int id, QuestState state)
	{
		return new GameData()
		{
			@Override
			public long getXp(Skill skill)
			{
				return -1;
			}

			@Override
			public QuestState getQuestState(int questId)
			{
				return questId == id ? state : null;
			}
		};
	}

	private static GameData varbit(int id, int value)
	{
		return varbits(Map.of(id, value));
	}

	private static GameData varbits(Map<Integer, Integer> values)
	{
		return new GameData()
		{
			@Override
			public long getXp(Skill skill)
			{
				return -1;
			}

			@Override
			public int getVarbit(int varbitId)
			{
				return values.getOrDefault(varbitId, -1);
			}
		};
	}

	private static GameData ca(int id, boolean complete)
	{
		return new GameData()
		{
			@Override
			public long getXp(Skill skill)
			{
				return -1;
			}

			@Override
			public Boolean isCombatAchievementComplete(int taskId)
			{
				return taskId == id ? complete : null;
			}
		};
	}
}
