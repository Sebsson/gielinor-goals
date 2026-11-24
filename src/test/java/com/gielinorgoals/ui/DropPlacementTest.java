package com.gielinorgoals.ui;

import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Var en släppt rad hamnar när några syskon är dolda ("Hide completed"): platsen räknas från
 * grannarnas id, inte från synligt radindex.
 */
public class DropPlacementTest
{
	private GoalTree tree;
	private Goal parent;
	// Fullständig ordning: A, B (klar, dold), C, D (klar, dold), E. Synligt: A, C, E.
	private List<String> visible;

	@Before
	public void setUp()
	{
		tree = new GoalTree();
		parent = new Goal("Parent");
		tree.add(null, parent);
		for (String title : List.of("A", "B", "C", "D", "E"))
		{
			Goal goal = new Goal(title);
			goal.markCompleted(title.equals("B") || title.equals("D"), 1L);
			tree.add(parent.getId(), goal);
		}
		visible = new ArrayList<>();
		for (Goal goal : tree.getChildren(parent))
		{
			if (!goal.isCompleted())
			{
				visible.add(goal.getId());
			}
		}
	}

	@Test
	public void dropBeforeAVisibleSiblingKeepsHiddenOnesInPlace()
	{
		// E släpps i luckan före C (lucka 1): hamnar mellan den dolda B och C.
		drop("E", 1);
		assertEquals(List.of("A", "B", "E", "C", "D"), order());
	}

	@Test
	public void dropFirst()
	{
		drop("E", 0);
		assertEquals(List.of("E", "A", "B", "C", "D"), order());
	}

	@Test
	public void dropAfterTheLastVisibleSiblingGoesRightAfterIt()
	{
		// A släpps efter E (sista luckan): direkt efter E, alltså sist.
		drop("A", 3);
		assertEquals(List.of("B", "C", "D", "E", "A"), order());
	}

	@Test
	public void dropAfterTheLastVisibleKeepsHiddenSiblingsBehindIt()
	{
		// Nu är det sista syskonet dolt: A, B (dold), C, D (dold). Synligt: A, C.
		Goal e = tree.getChildren(parent).get(4);
		tree.remove(e.getId());
		visible.remove(e.getId());
		drop("A", 2);
		// "Efter C" = före D, så den dolda D ligger kvar efter.
		assertEquals(List.of("B", "C", "A", "D"), order());
	}

	@Test
	public void theGapsAroundTheRowItselfChangeNothing()
	{
		String c = id("C");
		assertTrue(DropPlacement.changesNothing(visible, c, 1));
		assertTrue(DropPlacement.changesNothing(visible, c, 2));
		assertFalse(DropPlacement.changesNothing(visible, c, 0));
		assertFalse(DropPlacement.changesNothing(visible, c, 3));
	}

	private void drop(String title, int gap)
	{
		String moving = id(title);
		assertFalse(DropPlacement.changesNothing(visible, moving, gap));
		String before = DropPlacement.beforeId(tree, parent.getId(), moving, visible, gap);
		assertTrue(tree.moveWithinParent(parent.getId(), moving, before));
	}

	private String id(String title)
	{
		for (Goal goal : tree.getChildren(parent))
		{
			if (goal.getTitle().equals(title))
			{
				return goal.getId();
			}
		}
		throw new AssertionError(title);
	}

	private List<String> order()
	{
		List<String> titles = new ArrayList<>();
		tree.getChildren(parent).forEach(goal -> titles.add(goal.getTitle()));
		return titles;
	}
}
