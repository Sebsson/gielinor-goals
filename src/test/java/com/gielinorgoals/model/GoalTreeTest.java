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
		assertEquals(List.of("A1"), titles(tree.getChildren(a)));
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
		assertTrue(tree.moveUp(null, c.getId()));
		assertEquals(List.of("A", "C", "B"), titles(tree.getRoots()));

		assertTrue(tree.moveDown(null, a.getId()));
		assertEquals(List.of("C", "A", "B"), titles(tree.getRoots()));
	}

	@Test
	public void moveBeyondEdgesFails()
	{
		assertFalse(tree.moveUp(null, a.getId()));
		assertFalse(tree.moveDown(null, c.getId()));
		assertFalse(tree.canMoveUp(null, a.getId()));
		assertFalse(tree.canMoveDown(null, c.getId()));
	}

	@Test
	public void indentMakesGoalLastChildOfPreviousSibling()
	{
		Goal existing = new Goal("A1");
		tree.add(a.getId(), existing);

		assertTrue(tree.indent(null, b.getId()));
		assertEquals(List.of("A", "C"), titles(tree.getRoots()));
		assertEquals(List.of("A1", "B"), titles(tree.getChildren(a)));
	}

	@Test
	public void indentFirstSiblingFails()
	{
		assertFalse(tree.canIndent(null, a.getId()));
		assertFalse(tree.indent(null, a.getId()));
	}

	@Test
	public void outdentPlacesGoalDirectlyAfterParent()
	{
		Goal child = new Goal("A1");
		tree.add(a.getId(), child);

		assertTrue(tree.outdent(null, a.getId(), child.getId()));
		assertEquals(List.of("A", "A1", "B", "C"), titles(tree.getRoots()));
		assertTrue(tree.getChildren(a).isEmpty());
	}

	@Test
	public void outdentRootFails()
	{
		assertFalse(tree.canOutdent(null, null, a.getId()));
		assertFalse(tree.outdent(null, b.getId(), a.getId()));
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

	@Test
	public void linkSharesGoalBetweenParentsAndStoresItOnce()
	{
		Goal rfd = new Goal("Recipe for Disaster");
		tree.add(a.getId(), rfd);

		assertTrue(tree.link(b.getId(), rfd.getId()));

		assertEquals(List.of(a, b), tree.getParents(rfd.getId()));
		assertEquals(4, tree.size());
		assertTrue(tree.isAncestor(b.getId(), rfd.getId()));
		// Redan kopplad.
		assertFalse(tree.link(b.getId(), rfd.getId()));
	}

	@Test
	public void linkingRootMakesItSubGoal()
	{
		assertTrue(tree.link(a.getId(), c.getId()));

		assertEquals(List.of("A", "B"), titles(tree.getRoots()));
		assertEquals(List.of("C"), titles(tree.getChildren(a)));
	}

	@Test
	public void linkRefusesCycles()
	{
		Goal child = new Goal("A1");
		Goal grandchild = new Goal("A1a");
		tree.add(a.getId(), child);
		tree.add(child.getId(), grandchild);

		// A är förfader till A1a, så A1a -> A skulle bli en cykel.
		assertFalse(tree.canLink(grandchild.getId(), a.getId()));
		assertFalse(tree.link(grandchild.getId(), a.getId()));
		assertFalse(tree.link(child.getId(), child.getId()));
		assertFalse(tree.link(a.getId(), "missing"));
		assertTrue(tree.getChildren(grandchild).isEmpty());
	}

	@Test
	public void unlinkKeepsGoalUnderOtherParent()
	{
		Goal rfd = new Goal("Recipe for Disaster");
		tree.add(a.getId(), rfd);
		tree.link(b.getId(), rfd.getId());

		assertTrue(tree.unlink(a.getId(), rfd.getId()));

		assertEquals(List.of(b), tree.getParents(rfd.getId()));
		assertEquals(List.of("A", "B", "C"), titles(tree.getRoots()));
	}

	@Test
	public void unlinkLastParentMakesGoalRoot()
	{
		Goal child = new Goal("A1");
		tree.add(a.getId(), child);

		assertTrue(tree.unlink(a.getId(), child.getId()));

		assertEquals(List.of("A", "B", "C", "A1"), titles(tree.getRoots()));
		assertEquals(child, tree.find(child.getId()));
		assertFalse(tree.unlink(a.getId(), child.getId()));
	}

	@Test
	public void removeKeepsSharedChildrenAndDropsOwnedOnes()
	{
		Goal shared = new Goal("Shared");
		Goal owned = new Goal("Owned");
		Goal ownedGrandchild = new Goal("Owned child");
		tree.add(a.getId(), shared);
		tree.add(a.getId(), owned);
		tree.add(owned.getId(), ownedGrandchild);
		tree.link(b.getId(), shared.getId());

		assertTrue(tree.remove(a.getId()));

		assertNull(tree.find(a.getId()));
		assertNull(tree.find(owned.getId()));
		assertNull(tree.find(ownedGrandchild.getId()));
		assertEquals(shared, tree.find(shared.getId()));
		assertEquals(List.of(b), tree.getParents(shared.getId()));
		assertEquals(List.of("B", "C"), titles(tree.getRoots()));
	}

	@Test
	public void removeSharedGoalRemovesItFromAllParents()
	{
		Goal shared = new Goal("Shared");
		tree.add(a.getId(), shared);
		tree.link(b.getId(), shared.getId());

		assertTrue(tree.remove(shared.getId()));

		assertTrue(tree.getChildren(a).isEmpty());
		assertTrue(tree.getChildren(b).isEmpty());
		assertEquals(3, tree.size());
	}

	@Test
	public void moveOnlyAffectsTheGivenParent()
	{
		Goal x = new Goal("X");
		Goal y = new Goal("Y");
		tree.add(a.getId(), x);
		tree.add(a.getId(), y);
		tree.link(b.getId(), y.getId());
		tree.link(b.getId(), x.getId());

		assertTrue(tree.moveUp(a.getId(), y.getId()));

		assertEquals(List.of("Y", "X"), titles(tree.getChildren(a)));
		assertEquals(List.of("Y", "X"), titles(tree.getChildren(b)));
		assertTrue(tree.moveUp(b.getId(), x.getId()));
		assertEquals(List.of("Y", "X"), titles(tree.getChildren(a)));
		assertEquals(List.of("X", "Y"), titles(tree.getChildren(b)));
	}

	@Test
	public void outdentSharedGoalKeepsOtherParent()
	{
		Goal shared = new Goal("Shared");
		tree.add(a.getId(), shared);
		tree.link(b.getId(), shared.getId());

		assertTrue(tree.outdent(null, a.getId(), shared.getId()));

		assertTrue(tree.getChildren(a).isEmpty());
		assertEquals(List.of(b), tree.getParents(shared.getId()));
		// Under en annan förälder blir det inte även rot.
		assertEquals(List.of("A", "Shared", "B", "C"), titles(tree.getRoots()));
	}

	@Test
	public void indentRefusesToPutGoalUnderItsOwnDescendant()
	{
		// Under A ligger X och Y, och X är även delmål till Y.
		Goal x = new Goal("X");
		Goal y = new Goal("Y");
		tree.add(a.getId(), x);
		tree.add(a.getId(), y);
		assertTrue(tree.link(y.getId(), x.getId()));

		// Indrag skulle lägga Y under X, men Y är förfader till X.
		assertFalse(tree.canIndent(a.getId(), y.getId()));
		assertFalse(tree.indent(a.getId(), y.getId()));
	}

	@Test
	public void forEachVisitsSharedGoalOnce()
	{
		Goal shared = new Goal("Shared");
		tree.add(a.getId(), shared);
		tree.link(b.getId(), shared.getId());
		tree.add(shared.getId(), new Goal("Deep"));

		List<String> visited = new ArrayList<>();
		tree.forEach(goal -> visited.add(goal.getTitle()));

		assertEquals(List.of("A", "Shared", "Deep", "B", "C"), visited);
		assertEquals(visited.size(), tree.flatten().size());
	}

	@Test
	public void loadRepairsBrokenData()
	{
		Goal x = new Goal("X");
		Goal y = new Goal("Y");
		Goal orphan = new Goal("Orphan");
		x.getChildIds().addAll(List.of(y.getId(), "missing", y.getId()));
		// Cykel X -> Y -> X.
		y.getChildIds().add(x.getId());

		GoalTree loaded = GoalTree.load(List.of(x.getId(), x.getId(), "missing"), List.of(x, y, orphan, x));

		assertEquals(List.of("X", "Orphan"), titles(loaded.getRoots()));
		assertEquals(List.of(y.getId()), x.getChildIds());
		assertTrue(y.getChildIds().isEmpty());
		assertEquals(3, loaded.size());
	}

	private static List<String> titles(List<Goal> goals)
	{
		List<String> result = new ArrayList<>();
		goals.forEach(goal -> result.add(goal.getTitle()));
		return result;
	}
}
