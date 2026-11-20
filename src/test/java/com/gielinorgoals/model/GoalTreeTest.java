package com.gielinorgoals.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class GoalTreeTest
{
	private GoalTree tree;
	private Goal a;
	private Goal b;
	private Goal c;

	@Before
	public void setUp()
	{
		tree = new GoalTree();
		a = new Goal("A");
		b = new Goal("B");
		c = new Goal("C");
		tree.add(null, a);
		tree.add(null, b);
		tree.add(null, c);
	}

	@Test
	public void addAppendsToRootInOrder()
	{
		assertEquals(List.of("A", "B", "C"), titles(tree.getRoots()));
	}

	@Test
	public void addAsChildExpandsParent()
	{
		a.setExpanded(false);
		Goal child = new Goal("A1");

		assertTrue(tree.add(a.getId(), child));
		assertEquals(List.of("A1"), titles(a.getChildren()));
		assertTrue(a.isExpanded());
		assertEquals(child, tree.find(child.getId()));
	}

	@Test
	public void addToMissingParentFails()
	{
		assertFalse(tree.add("missing", new Goal("X")));
		assertEquals(3, tree.size());
	}

	@Test
	public void removeDeletesGoalWithChildren()
	{
		Goal child = new Goal("B1");
		tree.add(b.getId(), child);

		assertTrue(tree.remove(b.getId()));
		assertEquals(List.of("A", "C"), titles(tree.getRoots()));
		assertNull(tree.find(child.getId()));
	}

	@Test
	public void moveUpAndDownSwapWithinSiblings()
	{
		assertTrue(tree.moveUp(c.getId()));
		assertEquals(List.of("A", "C", "B"), titles(tree.getRoots()));

		assertTrue(tree.moveDown(a.getId()));
		assertEquals(List.of("C", "A", "B"), titles(tree.getRoots()));
	}

	@Test
	public void moveBeyondEdgesFails()
	{
		assertFalse(tree.moveUp(a.getId()));
		assertFalse(tree.moveDown(c.getId()));
		assertFalse(tree.canMoveUp(a.getId()));
		assertFalse(tree.canMoveDown(c.getId()));
	}

	@Test
	public void indentMakesGoalLastChildOfPreviousSibling()
	{
		Goal existing = new Goal("A1");
		tree.add(a.getId(), existing);

		assertTrue(tree.indent(b.getId()));
		assertEquals(List.of("A", "C"), titles(tree.getRoots()));
		assertEquals(List.of("A1", "B"), titles(a.getChildren()));
	}

	@Test
	public void indentFirstSiblingFails()
	{
		assertFalse(tree.canIndent(a.getId()));
		assertFalse(tree.indent(a.getId()));
	}

	@Test
	public void outdentPlacesGoalDirectlyAfterParent()
	{
		Goal child = new Goal("A1");
		tree.add(a.getId(), child);

		assertTrue(tree.outdent(child.getId()));
		assertEquals(List.of("A", "A1", "B", "C"), titles(tree.getRoots()));
		assertTrue(a.getChildren().isEmpty());
	}

	@Test
	public void outdentRootFails()
	{
		assertFalse(tree.canOutdent(a.getId()));
		assertFalse(tree.outdent(a.getId()));
	}

	@Test
	public void setCompletedStoresTimestamp()
	{
		assertTrue(tree.setCompleted(a.getId(), true, 1234L));
		assertTrue(a.isCompleted());
		assertEquals(Long.valueOf(1234L), a.getCompletedAt());

		tree.setCompleted(a.getId(), false, 5678L);
		assertFalse(a.isCompleted());
		assertNull(a.getCompletedAt());
	}

	@Test
	public void forEachVisitsParentsBeforeChildren()
	{
		tree.add(a.getId(), new Goal("A1"));
		List<String> visited = new ArrayList<>();
		tree.forEach(goal -> visited.add(goal.getTitle()));

		assertEquals(List.of("A", "A1", "B", "C"), visited);
	}

	private static List<String> titles(List<Goal> goals)
	{
		List<String> result = new ArrayList<>();
		goals.forEach(goal -> result.add(goal.getTitle()));
		return result;
	}
}
