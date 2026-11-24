package com.gielinorgoals.model;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Dra och släpp: {@link GoalTree#moveWithinParent} flyttar bara bland syskonen under en förälder.
 */
public class MoveWithinParentTest
{
	private GoalTree tree;
	private Goal parent;
	private Goal a;
	private Goal b;
	private Goal c;
	private Goal d;

	@Before
	public void setUp()
	{
		tree = new GoalTree();
		parent = new Goal("Parent");
		tree.add(null, parent);
		a = add(parent, "A");
		b = add(parent, "B");
		c = add(parent, "C");
		d = add(parent, "D");
	}

	@Test
	public void toTheFirstPlace()
	{
		assertTrue(tree.moveWithinParent(parent.getId(), c.getId(), a.getId()));
		assertEquals(List.of("C", "A", "B", "D"), children(parent));
	}

	@Test
	public void toTheLastPlace()
	{
		assertTrue(tree.moveWithinParent(parent.getId(), a.getId(), null));
		assertEquals(List.of("B", "C", "D", "A"), children(parent));
	}

	@Test
	public void intoTheMiddleBothWays()
	{
		assertTrue(tree.moveWithinParent(parent.getId(), a.getId(), d.getId()));
		assertEquals(List.of("B", "C", "A", "D"), children(parent));
		assertTrue(tree.moveWithinParent(parent.getId(), d.getId(), c.getId()));
		assertEquals(List.of("B", "D", "C", "A"), children(parent));
	}

	@Test
	public void theSamePlaceChangesNothing()
	{
		assertFalse(tree.moveWithinParent(parent.getId(), b.getId(), b.getId()));
		assertFalse(tree.moveWithinParent(parent.getId(), b.getId(), c.getId()));
		assertFalse(tree.moveWithinParent(parent.getId(), d.getId(), null));
		assertEquals(List.of("A", "B", "C", "D"), children(parent));
	}

	@Test
	public void rootsMoveInTheRootList()
	{
		Goal second = new Goal("Second");
		Goal third = new Goal("Third");
		tree.add(null, second);
		tree.add(null, third);
		assertTrue(tree.moveWithinParent(null, third.getId(), parent.getId()));
		assertEquals(List.of("Third", "Parent", "Second"), titles(tree.getRoots()));
		// Delmålen följer med sin förälder orörda.
		assertEquals(List.of("A", "B", "C", "D"), children(parent));
	}

	@Test
	public void movesToAnotherParentOrIntoAChildAreRefused()
	{
		Goal other = new Goal("Other");
		tree.add(null, other);
		Goal x = add(other, "X");
		Goal grandchild = add(a, "A1");

		// Målet finns inte under den angivna föräldern.
		assertFalse(tree.moveWithinParent(other.getId(), a.getId(), x.getId()));
		// Syskonet finns under en annan förälder.
		assertFalse(tree.moveWithinParent(parent.getId(), a.getId(), x.getId()));
		// Inuti ett eget delmål: barnbarnet är inget syskon.
		assertFalse(tree.moveWithinParent(parent.getId(), a.getId(), grandchild.getId()));
		// Rotnivån: ett delmål är ingen rot.
		assertFalse(tree.moveWithinParent(null, a.getId(), null));
		assertFalse(tree.moveWithinParent("missing", a.getId(), null));
		assertEquals(List.of("A", "B", "C", "D"), children(parent));
		assertEquals(List.of("X"), children(other));
	}

	@Test
	public void aSharedGoalMovesUnderOneParentOnly()
	{
		Goal other = new Goal("Other");
		tree.add(null, other);
		Goal x = add(other, "X");
		assertTrue(tree.link(other.getId(), c.getId()));
		assertEquals(List.of("X", "C"), children(other));

		assertTrue(tree.moveWithinParent(parent.getId(), c.getId(), a.getId()));
		assertEquals(List.of("C", "A", "B", "D"), children(parent));
		assertEquals(List.of("X", "C"), children(other));
		assertEquals(x, tree.getChildren(other).get(0));
	}

	@Test
	public void siblingAfterSkipsTheMovingGoal()
	{
		assertEquals(c.getId(), tree.siblingAfter(parent.getId(), b.getId(), a.getId()));
		// B står efter A men är målet som flyttas; nästa är C.
		assertEquals(c.getId(), tree.siblingAfter(parent.getId(), a.getId(), b.getId()));
		assertNull(tree.siblingAfter(parent.getId(), d.getId(), a.getId()));
	}

	@Test
	public void nextGoalsFollowTheNewOrder()
	{
		assertEquals(List.of("A", "B", "C", "D"), next());
		tree.moveWithinParent(parent.getId(), d.getId(), a.getId());
		assertEquals(List.of("D", "A", "B", "C"), next());

		Goal later = new Goal("Later root");
		tree.add(null, later);
		tree.moveWithinParent(null, later.getId(), parent.getId());
		assertEquals("Later root", next().get(0));
	}

	private List<String> next()
	{
		return titles(NextGoals.selectGoals(tree, 10));
	}

	private Goal add(Goal under, String title)
	{
		Goal goal = new Goal(title);
		tree.add(under.getId(), goal);
		return goal;
	}

	private List<String> children(Goal goal)
	{
		return titles(tree.getChildren(goal));
	}

	private static List<String> titles(List<Goal> goals)
	{
		List<String> titles = new ArrayList<>();
		goals.forEach(goal -> titles.add(goal.getTitle()));
		return titles;
	}
}
