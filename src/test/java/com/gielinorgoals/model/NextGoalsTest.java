package com.gielinorgoals.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class NextGoalsTest
{
	@Test
	public void childrenComeBeforeTheirParent()
	{
		GoalTree tree = new GoalTree();
		Goal fireCape = new Goal("Fire cape");
		tree.add(null, fireCape);
		tree.add(fireCape.getId(), new Goal("70 Ranged"));
		tree.add(fireCape.getId(), new Goal("43 Prayer"));
		tree.add(null, new Goal("Barrows gloves"));

		assertEquals(List.of("70 Ranged", "43 Prayer", "Barrows gloves"), titles(NextGoals.select(tree, id -> null, 5)));
	}

	@Test
	public void parentAppearsWhenAllChildrenAreDone()
	{
		GoalTree tree = new GoalTree();
		Goal parent = new Goal("Fire cape");
		Goal child = new Goal("70 Ranged");
		child.markCompleted(true, 1L);
		tree.add(null, parent);
		tree.add(parent.getId(), child);

		assertEquals(List.of("Fire cape"), titles(NextGoals.select(tree, id -> null, 5)));
	}

	@Test
	public void completedGoalsAndTheirSubtreesAreSkipped()
	{
		GoalTree tree = new GoalTree();
		Goal done = new Goal("Done");
		done.markCompleted(true, 1L);
		tree.add(null, done);
		tree.add(done.getId(), new Goal("Leftover child"));
		tree.add(null, new Goal("Next"));

		assertEquals(List.of("Next"), titles(NextGoals.select(tree, id -> null, 5)));
	}

	@Test
	public void limitIsRespectedAndProgressIsIncluded()
	{
		GoalTree tree = new GoalTree();
		Goal first = new Goal("First");
		tree.add(null, first);
		tree.add(null, new Goal("Second"));
		tree.add(null, new Goal("Third"));
		Map<String, GoalProgress> progress = Map.of(first.getId(), new GoalProgress(1, 4, false, "1/4"));

		List<NextGoals.Entry> entries = NextGoals.select(tree, progress::get, 2);

		assertEquals(List.of("First", "Second"), titles(entries));
		assertEquals("1/4", entries.get(0).getProgressLabel());
		assertEquals(0.25, entries.get(0).getFraction(), 0.0);
		assertNull(entries.get(1).getProgressLabel());
	}

	private static List<String> titles(List<NextGoals.Entry> entries)
	{
		List<String> titles = new ArrayList<>();
		entries.forEach(entry -> titles.add(entry.getTitle()));
		return titles;
	}

	@Test
	public void sharedGoalIsSuggestedOnce()
	{
		GoalTree tree = new GoalTree();
		Goal gloves = new Goal("Barrows gloves");
		Goal questCape = new Goal("Quest cape");
		Goal rfd = new Goal("Recipe for Disaster");
		tree.add(null, gloves);
		tree.add(null, questCape);
		tree.add(gloves.getId(), rfd);
		tree.link(questCape.getId(), rfd.getId());
		tree.add(questCape.getId(), new Goal("Dragon Slayer II"));

		assertEquals(List.of("Recipe for Disaster", "Dragon Slayer II"), titles(NextGoals.select(tree, id -> null, 5)));
	}
}
