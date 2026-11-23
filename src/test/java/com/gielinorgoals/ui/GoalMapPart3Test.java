package com.gielinorgoals.ui;

import com.gielinorgoals.GoalManager;
import com.gielinorgoals.TestGoalManagers;
import com.gielinorgoals.map.LayeredLayout;
import com.gielinorgoals.map.MapGraph;
import com.gielinorgoals.map.MapView;
import com.gielinorgoals.model.ActivityCounters;
import com.gielinorgoals.model.DropSource;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class GoalMapPart3Test
{
	private GoalManager manager;

	@Before
	public void setUp()
	{
		manager = TestGoalManagers.inMemory("profile");
	}

	@Test
	public void mapMenuHasNoOrderingButOffersEveryParent()
	{
		Goal shared = sharedGoal();

		List<String> map = items(new GoalActions(new JPanel(), shared, manager.getTree(), manager, null)
			.menu(GoalActions.Context.map()));
		assertFalse(map.contains("Move up"));
		assertFalse(map.contains("Show on map"));
		assertTrue(map.contains("Remove from Barrows gloves"));
		assertTrue(map.contains("Remove from Quest cape"));
		assertTrue(map.contains("Mark as done"));
		assertTrue(map.contains("Link to another goal..."));

		String parentId = manager.getTree().getParents(shared.getId()).get(0).getId();
		List<String> panel = items(new GoalActions(new JPanel(), shared, manager.getTree(), manager, null)
			.menu(GoalActions.Context.panel(parentId, null)));
		assertTrue(panel.contains("Move up"));
		assertTrue(panel.contains("Show on map"));
		// I panelen bara föräldern raden ligger under.
		assertTrue(panel.contains("Remove from Barrows gloves"));
		assertFalse(panel.contains("Remove from Quest cape"));
	}

	@Test
	public void detailsAreNotRebuiltWhenNothingChangedAndLinksActOnPress()
	{
		Goal shared = sharedGoal();
		List<String> shown = new ArrayList<>();
		GoalMapDetails details = new GoalMapDetails(manager, shown::add, goal ->
		{
		});
		details.show(shared.getId());
		Component[] before = details.componentsForTest();

		// Kartan uppdateras ofta (t.ex. XP varje tick); samma innehåll ska ge samma komponenter,
		// så att en länk inte byts ut mitt i ett klick.
		details.show(shared.getId());
		assertArrayEquals(before, details.componentsForTest());

		javax.swing.JLabel link = null;
		for (Component component : before)
		{
			if (component instanceof javax.swing.JLabel && ((javax.swing.JLabel) component).getText().contains("Quest cape"))
			{
				link = (javax.swing.JLabel) component;
			}
		}
		assertTrue(link != null);
		for (java.awt.event.MouseListener listener : link.getMouseListeners())
		{
			listener.mousePressed(new java.awt.event.MouseEvent(link, java.awt.event.MouseEvent.MOUSE_PRESSED,
				0L, java.awt.event.InputEvent.BUTTON1_DOWN_MASK, 2, 2, 1, false, java.awt.event.MouseEvent.BUTTON1));
		}
		String capeId = manager.getTree().getRoots().get(1).getId();
		assertTrue(shown.toString(), shown.contains(capeId));
	}

	@Test
	public void detailsShowParentsDropMeterAndNote()
	{
		Goal shared = sharedGoal();
		manager.addGoal(null, "", "Bought for 750 slayer points", new ItemRequirement(13576, "Dragon warhammer", 1, false),
			false, DropSource.npc("Lizardman shaman", 3000));
		Goal dwh = manager.getTree().getRoots().get(manager.getTree().getRoots().size() - 1);
		manager.setCounter("npc:lizardman shaman", 1500);

		GoalMapDetails details = new GoalMapDetails(manager, id ->
		{
		}, goal ->
		{
		});
		details.show(shared.getId());
		String text = details.textForTest();
		assertTrue(text, text.contains("Part of 2 goals"));
		assertTrue(text, text.contains("Barrows gloves"));
		assertTrue(text, text.contains("Quest cape"));

		details.show(dwh.getId());
		text = details.textForTest();
		assertTrue(text, text.contains("Drop rate 1/3,000 from Lizardman shaman"));
		assertTrue(text, text.contains("1,500 kc · 0.5× rate"));
		assertTrue(text, text.contains("Bought for 750 slayer points"));

		details.show("gone");
		assertFalse(details.isVisible());
	}

	@Test
	public void pulseTimerStopsWhenAsked()
	{
		Goal shared = sharedGoal();
		GoalTree tree = manager.getTree();
		MapGraph graph = MapGraph.build(tree, MapView.allGoals());
		ActivityCounters counters = new ActivityCounters();
		GoalMapCanvas canvas = new GoalMapCanvas(null, id -> null, () -> counters);
		canvas.setData(tree, graph, LayeredLayout.compute(graph), Set.of(), true);

		canvas.pulse(Set.of(shared.getId()));
		assertTrue(canvas.isPulsing());
		canvas.stopPulses();
		assertFalse(canvas.isPulsing());
	}

	// Ett mål under både Barrows gloves och Quest cape.
	private Goal sharedGoal()
	{
		manager.addGoal(null, "Barrows gloves", null, null);
		manager.addGoal(null, "Quest cape", null, null);
		List<Goal> roots = manager.getTree().getRoots();
		Goal gloves = roots.get(0);
		Goal cape = roots.get(1);
		manager.addGoal(gloves.getId(), "Recipe for Disaster", null, null);
		Goal rfd = manager.getTree().getChildren(gloves).get(0);
		manager.link(cape.getId(), rfd.getId());
		return rfd;
	}

	private static List<String> items(JPopupMenu menu)
	{
		List<String> texts = new ArrayList<>();
		for (Component component : menu.getComponents())
		{
			if (component instanceof JMenuItem)
			{
				texts.add(((JMenuItem) component).getText());
			}
		}
		return texts;
	}
}
