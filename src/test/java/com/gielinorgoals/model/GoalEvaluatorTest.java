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

	@Test
	public void aggregateCountsDirectChildrenAndCompletesWhenAllDone()
	{
		Goal cape = new Goal("Quest cape");
		cape.setAggregate(true);
		Goal q1 = new Goal("Quest 1");
		Goal q2 = new Goal("Quest 2");
		Goal deep = new Goal("Step of quest 2");
		tree.add(null, cape);
		tree.add(cape.getId(), q1);
		tree.add(cape.getId(), q2);
		tree.add(q2.getId(), deep);
		q1.markCompleted(true, 1L);

		GoalEvaluator.Result first = GoalEvaluator.evaluate(tree, GameData.EMPTY, 1000L);
		GoalProgress counter = first.getProgress().get(cape.getId());
		assertEquals("1/2", counter.getLabel());
		assertFalse(counter.isComplete());
		assertFalse(cape.isCompleted());

		// Bara direkta delmål räknas; det djupa delmålet påverkar inte räknaren.
		q2.markCompleted(true, 2L);
		GoalEvaluator.Result second = GoalEvaluator.evaluate(tree, GameData.EMPTY, 2000L);
		assertTrue(cape.isCompleted());
		assertEquals(Long.valueOf(2000L), cape.getCompletedAt());
		assertEquals(java.util.List.of(cape), second.getNewlyCompleted());
		assertEquals("2/2", second.getProgress().get(cape.getId()).getLabel());
	}

	@Test
	public void nestedAggregatesCompleteInOneEvaluationEvenWhenShared()
	{
		// A har X och Y, och X är även delmål till Y. Alla tre är räknarmål.
		Goal a = new Goal("A");
		Goal x = new Goal("X");
		Goal y = new Goal("Y");
		Goal leaf = new Goal("Leaf");
		for (Goal goal : java.util.List.of(a, x, y))
		{
			goal.setAggregate(true);
		}
		tree.add(null, a);
		tree.add(a.getId(), x);
		tree.add(a.getId(), y);
		tree.link(y.getId(), x.getId());
		tree.add(x.getId(), leaf);
		leaf.markCompleted(true, 1L);

		GoalEvaluator.Result result = GoalEvaluator.evaluate(tree, GameData.EMPTY, 1000L);

		assertTrue(x.isCompleted());
		assertTrue(y.isCompleted());
		assertTrue(a.isCompleted());
		assertEquals(3, result.getNewlyCompleted().size());
	}

	@Test
	public void aggregateWithRequirementIsNotCompletedByChildren()
	{
		manual.setAggregate(true);
		manual.setRequirement(SkillRequirement.level(Skill.MINING, 99));
		agility.markCompleted(true, 1L);

		GoalEvaluator.Result result = GoalEvaluator.evaluate(tree, SkillRequirementTest.xp(Skill.MINING, 0), 1000L);

		// Alla delmål är klara, men med eget krav styr kravet när målet är klart,
		// och kravets progress visas (level 1 av 99).
		assertFalse(manual.isCompleted());
		assertEquals("1/99", result.getProgress().get(manual.getId()).getLabel());
	}

	@Test
	public void emptyAggregateIsNeverCompleted()
	{
		Goal empty = new Goal("Empty");
		empty.setAggregate(true);
		tree.add(null, empty);

		GoalEvaluator.evaluate(tree, GameData.EMPTY, 1000L);

		assertFalse(empty.isCompleted());
	}
}
