package com.gielinorgoals.ui;

import com.gielinorgoals.map.LayeredLayout;
import com.gielinorgoals.map.MapGraph;
import com.gielinorgoals.map.MapLayout;
import com.gielinorgoals.map.MapView;
import com.gielinorgoals.model.ActivityCounters;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import java.awt.Rectangle;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GoalMapCanvasTest
{
	private final GoalTree tree = new GoalTree();

	@Test
	public void hoverMarksAncestorsAndDescendantsOnly()
	{
		Goal root = add(null, "Root");
		Goal a = add(root, "A");
		Goal b = add(root, "B");
		Goal c = add(a, "C");
		Goal d = add(b, "D");
		tree.link(a.getId(), d.getId());
		GoalMapCanvas canvas = canvas(root);

		canvas.hoverForTest(a.getId());

		assertEquals(Set.of(root.getId(), a.getId(), c.getId(), d.getId()), canvas.getRelated());
		canvas.hoverForTest(null);
		assertTrue(canvas.getRelated().isEmpty());
	}

	@Test
	public void fitKeepsTheZoomWithinLimits()
	{
		Goal root = add(null, "Root");
		for (int i = 0; i < 40; i++)
		{
			add(root, "Child " + i);
		}
		GoalMapCanvas canvas = canvas(root);
		canvas.setSize(800, 600);

		canvas.fitToWindow();
		assertTrue(canvas.getScale() >= GoalMapCanvas.MIN_SCALE && canvas.getScale() < 1.0);

		canvas.setSize(4000, 3000);
		canvas.fitToWindow();
		// Små kartor förstoras aldrig över 100 %.
		assertTrue(canvas.getScale() <= 1.0);
	}

	@Test
	public void centeredGoalStaysCenteredWhenTheMapIsResized()
	{
		Goal root = add(null, "Root");
		Goal far = null;
		for (int i = 0; i < 20; i++)
		{
			far = add(root, "Child " + i);
		}
		GoalMapCanvas canvas = canvas(root);
		canvas.setSize(1100, 700);
		canvas.fitToWindow();

		canvas.centerOn(far.getId());
		assertNodeCentered(canvas, far);
		// T.ex. när detaljpanelen visas och kartan blir smalare.
		canvas.setSize(830, 640);
		assertNodeCentered(canvas, far);
	}

	private void assertNodeCentered(GoalMapCanvas canvas, Goal goal)
	{
		java.awt.geom.Point2D center = canvas.worldCenterForTest();
		com.gielinorgoals.map.MapLayout.Node node = canvas.nodesForTest().stream()
			.filter(n -> n.getGoalId().equals(goal.getId())).findFirst().orElseThrow();
		assertEquals(node.centerX(), center.getX(), 1.0);
		assertEquals(node.getY() + node.getHeight() / 2, center.getY(), 1.0);
	}

	@Test
	public void savedWindowBoundsAreReadSafely()
	{
		assertEquals(new Rectangle(10, 20, 900, 600), GoalMapWindow.parseBounds("10,20,900,600"));
		assertEquals(new Rectangle(-1900, 5, 900, 600), GoalMapWindow.parseBounds("-1900, 5, 900, 600"));
		assertNull(GoalMapWindow.parseBounds(""));
		assertNull(GoalMapWindow.parseBounds("1,2,3"));
		assertNull(GoalMapWindow.parseBounds("a,b,c,d"));
		// För litet för att vara ett riktigt fönster.
		assertNull(GoalMapWindow.parseBounds("0,0,50,40"));
	}

	private GoalMapCanvas canvas(Goal root)
	{
		MapGraph graph = MapGraph.build(tree, MapView.of(root.getId()));
		MapLayout layout = LayeredLayout.compute(graph);
		ActivityCounters counters = new ActivityCounters();
		GoalMapCanvas canvas = new GoalMapCanvas(null, id -> null, () -> counters);
		canvas.setData(tree, graph, layout, Set.of(), true);
		return canvas;
	}

	private Goal add(Goal parent, String title)
	{
		Goal goal = new Goal(title);
		tree.add(parent == null ? null : parent.getId(), goal);
		return goal;
	}
}
