package com.gielinorgoals.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

public class GoalEvaluatorTest
{
	private GoalTree tree;
	private Goal agility;
	private Goal manual;

	@Before
	public void setUp()
	{
		tree = new GoalTree();
		agility = new Goal("");
		agility.setRequirement(SkillRequirement.level(Skill.AGILITY, 50));
		manual = new Goal("Buy a house");
		tree.add(null, manual);
		tree.add(manual.getId(), agility);
	}

	@Test
	public void completesGoalWhenRequirementIsMet()
	{
		GoalEvaluator.Result result = GoalEvaluator.evaluate(tree, agilityXp(50), 1000L);

		assertTrue(agility.isCompleted());
		assertEquals(Long.valueOf(1000L), agility.getCompletedAt());
		assertEquals(1, result.getNewlyCompleted().size());
		assertEquals(agility, result.getNewlyCompleted().get(0));
	}

	@Test
	public void progressIsReportedOnlyForAutomaticGoals()
	{
		GoalEvaluator.Result result = GoalEvaluator.evaluate(tree, agilityXp(40), 0L);

		assertFalse(agility.isCompleted());
		assertTrue(result.getNewlyCompleted().isEmpty());
		assertEquals("40/50", result.getProgress().get(agility.getId()).getLabel());
		assertFalse(result.getProgress().containsKey(manual.getId()));
	}

	@Test
	public void completedGoalIsLatchedAndReportedOnce()
	{
		GoalEvaluator.evaluate(tree, agilityXp(50), 1000L);
		GoalEvaluator.Result second = GoalEvaluator.evaluate(tree, agilityXp(50), 2000L);

		assertTrue(second.getNewlyCompleted().isEmpty());
		assertEquals(Long.valueOf(1000L), agility.getCompletedAt());
	}

	@Test
	public void latchSurvivesDataThatNoLongerMeetsRequirement()
	{
		GoalEvaluator.evaluate(tree, agilityXp(50), 1000L);
		GoalEvaluator.evaluate(tree, agilityXp(1), 2000L);

		assertTrue(agility.isCompleted());
	}

	@Test
	public void unknownDataChangesNothing()
	{
		GoalEvaluator.Result result = GoalEvaluator.evaluate(tree, GameData.EMPTY, 0L);

		assertFalse(agility.isCompleted());
		assertTrue(result.getProgress().isEmpty());
	}

	@Test
	public void manualGoalsAreNeverTouched()
	{
		GoalEvaluator.evaluate(tree, agilityXp(99), 0L);
		assertFalse(manual.isCompleted());
	}

	private static GameData agilityXp(int level)
	{
		return SkillRequirementTest.xp(Skill.AGILITY, Experience.getXpForLevel(level));
	}

	@Test
	public void sharedGoalIsEvaluatedAndReportedOnce()
	{
		Goal other = new Goal("Quest cape");
		tree.add(null, other);
		tree.link(other.getId(), agility.getId());

		GoalEvaluator.Result result = GoalEvaluator.evaluate(tree, agilityXp(50), 1000L);

		assertEquals(1, result.getNewlyCompleted().size());
		assertEquals(agility, result.getNewlyCompleted().get(0));
	}
}
