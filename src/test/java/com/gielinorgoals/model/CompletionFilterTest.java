package com.gielinorgoals.model;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class CompletionFilterTest
{
	private GoalTree tree;
	private Goal gloves;
	private Goal quest;
	private Goal subQuest;
	private Goal skill;
	private Goal other;

	// gloves -> quest -> subQuest, gloves -> skill; other är ett eget rotmål.
	@Before
	public void setUp()
	{
		tree = new GoalTree();
		gloves = new Goal("Barrows gloves");
		quest = new Goal("Recipe for Disaster");
		subQuest = new Goal("Desert Treasure");
		skill = new Goal("70 Cooking");
		other = new Goal("Fire cape");
		tree.add(null, gloves);
		tree.add(gloves.getId(), quest);
		tree.add(quest.getId(), subQuest);
		tree.add(gloves.getId(), skill);
		tree.add(null, other);
	}

	@Test
	public void isAncestorWorksAtAnyDepth()
	{
		assertTrue(tree.isAncestor(gloves.getId(), quest.getId()));
		assertTrue(tree.isAncestor(gloves.getId(), subQuest.getId()));
		assertFalse(tree.isAncestor(subQuest.getId(), gloves.getId()));
		assertFalse(tree.isAncestor(gloves.getId(), gloves.getId()));
		assertFalse(tree.isAncestor(other.getId(), skill.getId()));
	}

	@Test
	public void getParentsReturnsDirectParents()
	{
		assertEquals(List.of(gloves), tree.getParents(quest.getId()));
		assertEquals(List.of(quest), tree.getParents(subQuest.getId()));
		assertTrue(tree.getParents(gloves.getId()).isEmpty());
	}

	@Test
	public void goalAndChildrenInSameBatchShowOnlyTopGoal()
	{
		complete(gloves, quest, skill);

		List<CompletionFilter.Entry> entries = CompletionFilter.filter(tree, List.of(gloves, quest, skill), false);

		assertEquals(1, entries.size());
		assertSame(gloves, entries.get(0).getGoal());
		assertEquals(2, entries.get(0).getHiddenDescendants());
		assertTrue(entries.get(0).isRoot());
	}

	@Test
	public void deepSubGoalIsHiddenEvenWhenMiddleGoalIsNotInBatch()
	{
		complete(gloves, subQuest);

		List<CompletionFilter.Entry> entries = CompletionFilter.filter(tree, List.of(gloves, subQuest), false);

		assertEquals(1, entries.size());
		assertSame(gloves, entries.get(0).getGoal());
		assertEquals(1, entries.get(0).getHiddenDescendants());
	}

	@Test
	public void subGoalShowsProgressInOpenParent()
	{
		complete(skill);

		List<CompletionFilter.Entry> entries = CompletionFilter.filter(tree, List.of(skill), false);

		assertEquals(1, entries.size());
		CompletionFilter.Entry entry = entries.get(0);
		assertFalse(entry.isRoot());
		assertSame(gloves, entry.getOpenParent());
		assertEquals(1, entry.getParentDone());
		assertEquals(2, entry.getParentTotal());
	}

	@Test
	public void unrelatedGoalsGetOwnEntries()
	{
		complete(skill, other);

		List<CompletionFilter.Entry> entries = CompletionFilter.filter(tree, List.of(skill, other), false);

		assertEquals(2, entries.size());
		assertSame(skill, entries.get(0).getGoal());
		assertSame(other, entries.get(1).getGoal());
		assertNull(entries.get(1).getOpenParent());
	}

	@Test
	public void rootsOnlyDropsSubGoals()
	{
		complete(skill, other);

		List<CompletionFilter.Entry> entries = CompletionFilter.filter(tree, List.of(skill, other), true);

		assertEquals(1, entries.size());
		assertSame(other, entries.get(0).getGoal());
	}

	@Test
	public void rootsOnlyKeepsRootWithItsSubGoalsCounted()
	{
		complete(gloves, quest, subQuest, skill);

		List<CompletionFilter.Entry> entries = CompletionFilter.filter(tree,
			List.of(gloves, quest, subQuest, skill), true);

		assertEquals(1, entries.size());
		assertSame(gloves, entries.get(0).getGoal());
		assertEquals(3, entries.get(0).getHiddenDescendants());
	}

	private static void complete(Goal... goals)
	{
		for (Goal goal : goals)
		{
			goal.markCompleted(true, 1L);
		}
	}

	@Test
	public void sharedGoalGivesOnePopupAndCountsInParent()
	{
		Goal questCape = new Goal("Quest cape");
		tree.add(null, questCape);
		tree.link(questCape.getId(), quest.getId());
		complete(quest);

		List<CompletionFilter.Entry> entries = CompletionFilter.filter(tree, List.of(quest), false);

		assertEquals(1, entries.size());
		assertFalse(entries.get(0).isRoot());
		// Första oklara föräldern används för progressraden.
		assertSame(gloves, entries.get(0).getOpenParent());
		assertEquals(List.of(gloves, questCape), tree.getParents(quest.getId()));
	}

	@Test
	public void sharedSubGoalIsHiddenWhenEitherAncestorCompletes()
	{
		Goal questCape = new Goal("Quest cape");
		tree.add(null, questCape);
		tree.link(questCape.getId(), subQuest.getId());
		complete(questCape, subQuest);

		List<CompletionFilter.Entry> entries = CompletionFilter.filter(tree, List.of(questCape, subQuest), false);

		assertEquals(1, entries.size());
		assertSame(questCape, entries.get(0).getGoal());
		assertEquals(1, entries.get(0).getHiddenDescendants());
	}
}
