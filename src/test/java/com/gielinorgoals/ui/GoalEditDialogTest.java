package com.gielinorgoals.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.tracking.CompletionStatus;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import org.junit.Test;

public class GoalEditDialogTest
{
	@Test
	public void parsesPlainAndAbbreviatedXp()
	{
		assertEquals(13_034_431L, GoalEditDialog.parseXp("13034431"));
		assertEquals(13_034_431L, GoalEditDialog.parseXp("13,034,431"));
		assertEquals(13_000_000L, GoalEditDialog.parseXp("13m"));
		assertEquals(500_000L, GoalEditDialog.parseXp("500K"));
		assertEquals(200_000_000L, GoalEditDialog.parseXp("200m"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsXpAboveMax()
	{
		GoalEditDialog.parseXp("201m");
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsGarbageXp()
	{
		GoalEditDialog.parseXp("lots");
	}

	@Test
	public void parsesItemQuantity()
	{
		assertEquals(1L, GoalEditDialog.parseQuantity("1"));
		assertEquals(10_000L, GoalEditDialog.parseQuantity("10k"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsZeroQuantity()
	{
		GoalEditDialog.parseQuantity("0");
	}

	@Test
	public void questSearchFindsByName()
	{
		List<GoalEditDialog.QuestOption> results = GoalEditDialog.filterQuests(CompletionStatus.UNKNOWN, "dragon slayer");

		assertEquals("Dragon Slayer I", results.get(0).getName());
	}

	@Test
	public void finishedQuestsAreHiddenAndStartedOnesMarked()
	{
		Quest finished = Quest.COOKS_ASSISTANT;
		Quest started = Quest.DRAGON_SLAYER_I;
		CompletionStatus status = new CompletionStatus(true,
			Map.of(finished.getId(), QuestState.FINISHED, started.getId(), QuestState.IN_PROGRESS),
			Map.of(), new int[0]);

		List<GoalEditDialog.QuestOption> all = GoalEditDialog.filterQuests(status, "");

		assertTrue(all.stream().noneMatch(q -> q.getId() == finished.getId()));
		GoalEditDialog.QuestOption option = all.stream().filter(q -> q.getId() == started.getId()).findFirst().get();
		assertTrue(option.isInProgress());
		assertEquals("Dragon Slayer I (in progress)", option.toString());
	}

	@Test
	public void completedDiaryTiersAreHidden()
	{
		DiaryArea area = DiaryArea.VARROCK;
		CompletionStatus status = new CompletionStatus(true, Map.of(),
			Map.of(area.varbitFor(DiaryTier.EASY), 1, area.varbitFor(DiaryTier.MEDIUM), 0), new int[0]);

		List<GoalEditDialog.DiaryOption> varrock = GoalEditDialog.filterDiaries(status, "varrock");

		assertEquals(3, varrock.size());
		assertTrue(varrock.stream().noneMatch(d -> d.getTier() == DiaryTier.EASY));
		// Utan inloggning visas allt, 12 områden x 4 nivåer.
		assertEquals(48, GoalEditDialog.filterDiaries(CompletionStatus.UNKNOWN, "").size());
	}

	@Test
	public void parsesLevelInRange()
	{
		assertEquals(99, GoalEditDialog.parseLevel("99"));
		assertEquals(126, GoalEditDialog.parseLevel("126"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsLevelOutOfRange()
	{
		GoalEditDialog.parseLevel("1");
	}
}
