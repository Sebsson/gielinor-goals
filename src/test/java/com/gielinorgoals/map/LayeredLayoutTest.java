package com.gielinorgoals.map;

import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.template.QuestCapeExclusions;
import com.gielinorgoals.template.QuestCapeTemplate;
import com.gielinorgoals.template.QuestChains;
import com.gielinorgoals.template.QuestRequirementsDb;
import com.gielinorgoals.template.TemplateInserter;
import com.gielinorgoals.template.TemplateNode;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LayeredLayoutTest
{
	private final GoalTree tree = new GoalTree();

	@Test
	public void simpleTreeHasOneLayerPerDepthAndCentersTheParent()
	{
		Goal root = add(null, "Root");
		Goal a = add(root, "A");
		Goal b = add(root, "B");
		Goal c = add(a, "C");

		MapLayout layout = layout(MapView.of(root.getId()));

		assertEquals(0, node(layout, root).getLayer());
		assertEquals(1, node(layout, a).getLayer());
		assertEquals(1, node(layout, b).getLayer());
		assertEquals(2, node(layout, c).getLayer());
		assertTrue(node(layout, a).getX() < node(layout, b).getX());
		assertEquals((node(layout, a).centerX() + node(layout, b).centerX()) / 2, node(layout, root).centerX(), 1.0);
		assertEquals(3, layout.getEdges().size());
		assertNoOverlaps(layout);
	}

	@Test
	public void sharedGoalAppearsOnceWithTwoIncomingEdges()
	{
		Goal root = add(null, "Root");
		Goal left = add(root, "Left");
		Goal right = add(root, "Right");
		Goal shared = add(left, "Shared");
		tree.link(right.getId(), shared.getId());

		MapLayout layout = layout(MapView.of(root.getId()));

		assertEquals(4, layout.getNodes().size());
		assertEquals(2, edgesInto(layout, shared).size());
		assertEquals(2, node(layout, shared).getLayer());
		// Under båda föräldrarna: mitt emellan dem.
		assertEquals((node(layout, left).centerX() + node(layout, right).centerX()) / 2,
			node(layout, shared).centerX(), 1.0);
	}

	@Test
	public void diamondUsesTheLongestPathAndAnEdgeOverTwoLayers()
	{
		Goal root = add(null, "Root");
		Goal a = add(root, "A");
		Goal b = add(a, "B");
		Goal c = add(b, "C");
		tree.link(root.getId(), c.getId());

		MapLayout layout = layout(MapView.of(root.getId()));

		assertEquals(3, node(layout, c).getLayer());
		MapLayout.Edge shortcut = edge(layout, root, c);
		assertEquals(3, shortcut.getSpan());
		assertEquals(node(layout, root).getY() + LayeredLayout.NODE_HEIGHT, shortcut.getY1(), 0.0);
		assertEquals(node(layout, c).getY(), shortcut.getY2(), 0.0);
	}

	@Test
	public void collapsedAggregateHidesItsSubgraphUntilExpanded()
	{
		Goal root = add(null, "Root");
		Goal skills = add(root, "Skills");
		skills.setAggregate(true);
		Goal attack = add(skills, "Attack");
		add(attack, "Deeper");

		MapView view = MapView.of(root.getId());
		MapGraph graph = MapGraph.build(tree, view);
		assertEquals(2, graph.size());
		assertTrue(graph.isCollapsed(skills.getId()));
		assertNull(layout(view).node(attack.getId()));

		MapGraph open = MapGraph.build(tree, view.withExpanded(skills.getId(), true));
		assertEquals(4, open.size());
		assertFalse(open.isCollapsed(skills.getId()));
		// Kartans utfällning rör inte panelens.
		assertTrue(skills.isExpanded());
	}

	@Test
	public void scopeRootIsExpandedEvenWhenItIsAnAggregate()
	{
		Goal cape = add(null, "Quest cape");
		cape.setAggregate(true);
		add(cape, "Cook's Assistant");

		assertEquals(2, MapGraph.build(tree, MapView.of(cape.getId())).size());
		// Under "Alla mål" är rötterna också omfångets rötter.
		assertEquals(2, MapGraph.build(tree, MapView.allGoals()).size());
		assertEquals(1, MapGraph.build(tree, MapView.allGoals().withExpanded(cape.getId(), false)).size());
	}

	@Test
	public void hideCompletedKeepsDoneGoalsThatHaveOpenSubGoals()
	{
		Goal root = add(null, "Root");
		root.markCompleted(true, 1L);
		Goal done = add(root, "Done");
		done.markCompleted(true, 1L);
		Goal doneWithOpen = add(root, "Done but open below");
		doneWithOpen.markCompleted(true, 1L);
		Goal open = add(doneWithOpen, "Open");

		MapGraph all = MapGraph.build(tree, MapView.of(root.getId()));
		MapGraph hidden = MapGraph.build(tree, MapView.of(root.getId()).withHideCompleted(true));

		assertEquals(4, all.size());
		assertEquals(3, hidden.size());
		assertFalse(hidden.contains(done.getId()));
		assertTrue(hidden.contains(doneWithOpen.getId()));
		assertTrue(hidden.contains(open.getId()));
		// Omfångets rot visas alltid, även klar.
		assertTrue(hidden.contains(root.getId()));
	}

	@Test
	public void sameInputGivesSameCoordinates()
	{
		Goal root = add(null, "Root");
		for (int i = 0; i < 6; i++)
		{
			Goal child = add(root, "Child " + i);
			for (int j = 0; j < 3; j++)
			{
				add(child, "Leaf " + i + "." + j);
			}
		}
		tree.link(tree.getRoots().get(0).getId(), tree.getChildren(tree.getChildren(root).get(2)).get(1).getId());

		MapLayout first = layout(MapView.of(root.getId()));
		MapLayout second = layout(MapView.of(root.getId()));

		assertEquals(coordinates(first), coordinates(second));
	}

	@Test
	public void cacheRecomputesOnlyWhenGoalsOrViewChange()
	{
		Goal root = add(null, "Root");
		add(root, "A");
		MapLayoutCache cache = new MapLayoutCache();
		MapView view = MapView.of(root.getId());

		MapLayout layout = cache.get(tree, 1, view);
		assertEquals(layout, cache.get(tree, 1, MapView.of(root.getId())));
		assertEquals(1, cache.getComputations());

		cache.get(tree, 1, view.withHideCompleted(true));
		assertEquals(2, cache.getComputations());
		cache.get(tree, 2, view.withHideCompleted(true));
		assertEquals(3, cache.getComputations());
		assertNotNull(cache.getGraph());
	}

	@Test
	public void questCapeWithFullChainsIsFast()
	{
		Gson gson = new Gson();
		TemplateNode cape = new QuestCapeTemplate(QuestCapeExclusions.load(gson)).build(Map.of());
		QuestChains.expand(cape, QuestRequirementsDb.load(gson));
		TemplateInserter.insert(tree, cape, null);
		String rootId = tree.getRoots().get(0).getId();

		// Första körningen värmer upp JIT; den andra mäts.
		layout(MapView.of(rootId));
		long start = System.nanoTime();
		MapLayout layout = layout(MapView.of(rootId));
		long millis = (System.nanoTime() - start) / 1_000_000;

		// Questdatabasen saknar ännu många quests, så kedjorna blir ~220 noder i stället för ~300.
		assertTrue("nodes: " + layout.getNodes().size(), layout.getNodes().size() >= 200);
		assertTrue("took " + millis + " ms", millis < 1000);
		assertNoOverlaps(layout);
		for (MapLayout.Edge edge : layout.getEdges())
		{
			assertTrue(edge.getSpan() >= 1);
		}
	}

	@Test
	public void sixHundredNodesWithSharedSubGoalsAreFast()
	{
		// Deterministisk DAG: 12 lager à 50 noder, varje nod under 1-3 noder i lagret ovanför.
		List<List<Goal>> layers = new ArrayList<>();
		Goal root = add(null, "Root");
		layers.add(List.of(root));
		long seed = 42;
		for (int l = 1; l <= 12; l++)
		{
			List<Goal> layer = new ArrayList<>();
			List<Goal> above = layers.get(l - 1);
			for (int i = 0; i < 50; i++)
			{
				seed = seed * 6364136223846793005L + 1442695040888963407L;
				Goal goal = add(above.get((int) Math.floorMod(seed >>> 33, (long) above.size())), "N" + l + "." + i);
				for (int extra = 0; extra < (int) Math.floorMod(seed >>> 20, 3L); extra++)
				{
					seed = seed * 6364136223846793005L + 1442695040888963407L;
					tree.link(above.get((int) Math.floorMod(seed >>> 33, (long) above.size())).getId(), goal.getId());
				}
				layer.add(goal);
			}
			layers.add(layer);
		}

		layout(MapView.of(root.getId()));
		long start = System.nanoTime();
		MapLayout layout = layout(MapView.of(root.getId()));
		long millis = (System.nanoTime() - start) / 1_000_000;

		assertEquals(601, layout.getNodes().size());
		assertTrue("took " + millis + " ms", millis < 2000);
		assertNoOverlaps(layout);
		assertEquals(coordinates(layout), coordinates(layout(MapView.of(root.getId()))));
	}

	private MapLayout layout(MapView view)
	{
		return LayeredLayout.compute(MapGraph.build(tree, view));
	}

	private Goal add(Goal parent, String title)
	{
		Goal goal = new Goal(title);
		tree.add(parent == null ? null : parent.getId(), goal);
		return goal;
	}

	private static MapLayout.Node node(MapLayout layout, Goal goal)
	{
		MapLayout.Node node = layout.node(goal.getId());
		assertNotNull(goal.getTitle(), node);
		return node;
	}

	private static List<MapLayout.Edge> edgesInto(MapLayout layout, Goal goal)
	{
		List<MapLayout.Edge> edges = new ArrayList<>();
		for (MapLayout.Edge edge : layout.getEdges())
		{
			if (edge.getToId().equals(goal.getId()))
			{
				edges.add(edge);
			}
		}
		return edges;
	}

	private static MapLayout.Edge edge(MapLayout layout, Goal from, Goal to)
	{
		for (MapLayout.Edge edge : layout.getEdges())
		{
			if (edge.getFromId().equals(from.getId()) && edge.getToId().equals(to.getId()))
			{
				return edge;
			}
		}
		throw new AssertionError("No edge " + from.getTitle() + " -> " + to.getTitle());
	}

	private static Map<String, String> coordinates(MapLayout layout)
	{
		Map<String, String> coordinates = new HashMap<>();
		layout.getNodes().forEach(node -> coordinates.put(node.getGoalId(), node.getX() + "," + node.getY()));
		return coordinates;
	}

	private static void assertNoOverlaps(MapLayout layout)
	{
		Map<Integer, List<MapLayout.Node>> byLayer = new HashMap<>();
		layout.getNodes().forEach(node -> byLayer.computeIfAbsent(node.getLayer(), l -> new ArrayList<>()).add(node));
		for (List<MapLayout.Node> layer : byLayer.values())
		{
			layer.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
			for (int i = 1; i < layer.size(); i++)
			{
				double gap = layer.get(i).getX() - layer.get(i - 1).getX();
				assertTrue("gap " + gap, gap >= LayeredLayout.NODE_WIDTH + LayeredLayout.H_GAP - 1);
			}
		}
	}
}
