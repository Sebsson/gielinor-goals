package com.gielinorgoals.template;

import com.gielinorgoals.GoalManager;
import com.gielinorgoals.TestGoalManagers;
import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalProgress;
import com.gielinorgoals.model.ItemRequirement;
import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Graceful version 2: ägda delar räknas mot märkena, och en insättning från version 1 får ändringen
 * via "Update from template" utan att målet flyttas.
 */
public class GracefulMarksTest
{
	private static final Set<Integer> SET = Set.of(ItemID.GRACEFUL_HOOD, ItemID.GRACEFUL_CAPE, ItemID.GRACEFUL_TOP,
		ItemID.GRACEFUL_LEGS, ItemID.GRACEFUL_GLOVES, ItemID.GRACEFUL_BOOTS);

	private final Gson gson = new Gson();
	private final TemplateCatalog catalog = new TemplateCatalog(gson);

	@Test
	public void ownedPiecesCountTowardTheMarks()
	{
		ItemRequirement marks = marks(catalog.find("graceful").build(Map.of()));
		GoalProgress progress = marks.progress(items(10));
		assertEquals(270, progress.getCurrent());
		assertTrue(progress.isComplete());
	}

	@Test
	public void anOldInsertGetsTheChangeInPlace()
	{
		GoalManager manager = TestGoalManagers.inMemory("profile");
		TemplateUpdates updates = new TemplateUpdates(catalog, QuestRequirementsDb.load(gson));
		// Version 1: märkesmålet utan motsvarigheter.
		TemplateNode node = catalog.find("graceful").build(Map.of());
		node.setTemplateId("graceful");
		node.setTemplateParams(Map.of(TemplateUpdates.QUEST_CHAINS_PARAM, 0));
		Goal root = manager.insertTemplate(withoutCountsAlso(node)).getRoot();
		Goal marksGoal = findMarks(manager);
		assertTrue(((ItemRequirement) marksGoal.getRequirement()).getCountsAlso().isEmpty());

		TemplateUpdates.Plan plan = updates.plan(manager.getTree(), root);
		assertTrue(plan.getAdditions().isEmpty());
		assertTrue(plan.getRemoved().isEmpty());
		assertEquals(1, plan.getChanges().size());
		assertEquals(marksGoal, plan.getChanges().get(0).getGoal());
		assertTrue(plan.isNewVersion());

		manager.applyTemplateUpdate(root.getId(), List.of(), List.of(), List.of(), plan.getTemplateVersion(),
			plan.getChanges());
		Goal after = findMarks(manager);
		assertEquals(marksGoal.getId(), after.getId());
		assertEquals(6, ((ItemRequirement) after.getRequirement()).getCountsAlso().size());
		assertFalse(updates.hasUpdate(manager.getTree(), root, manager.getRevision()));
	}

	@Test
	public void decliningTheChangeIsRemembered()
	{
		GoalManager manager = TestGoalManagers.inMemory("profile");
		TemplateUpdates updates = new TemplateUpdates(catalog, QuestRequirementsDb.load(gson));
		TemplateNode node = catalog.find("graceful").build(Map.of());
		node.setTemplateId("graceful");
		node.setTemplateParams(Map.of(TemplateUpdates.QUEST_CHAINS_PARAM, 0));
		Goal root = manager.insertTemplate(withoutCountsAlso(node)).getRoot();
		TemplateUpdates.Plan plan = updates.plan(manager.getTree(), root);

		manager.applyTemplateUpdate(root.getId(), List.of(), List.of(), List.of(plan.getChanges().get(0).declineKey()),
			plan.getTemplateVersion(), List.of());
		assertTrue(((ItemRequirement) findMarks(manager).getRequirement()).getCountsAlso().isEmpty());
		assertFalse(updates.hasUpdate(manager.getTree(), root, manager.getRevision()));
	}

	private static ItemRequirement marks(TemplateNode root)
	{
		for (TemplateNode group : root.getChildren())
		{
			for (TemplateNode child : group.getChildren())
			{
				if (child.getRequirement() instanceof ItemRequirement
					&& ((ItemRequirement) child.getRequirement()).getItemId() == ItemID.GRACE)
				{
					return (ItemRequirement) child.getRequirement();
				}
			}
		}
		throw new AssertionError("No marks node");
	}

	// Templaten som i version 1: samma noder, men märkesmålet utan motsvarigheter.
	private static TemplateNode withoutCountsAlso(TemplateNode node)
	{
		TemplateNode copy = new TemplateNode(node.getTitle(), plain(node), node.isAggregate());
		copy.setIcon(node.getIcon());
		copy.setNote(node.getNote());
		copy.setWikiPage(node.getWikiPage());
		copy.setDoneWhen(node.getDoneWhen());
		copy.setTemplateId(node.getTemplateId());
		copy.setTemplateParams(node.getTemplateParams());
		node.getChildren().forEach(child -> copy.add(withoutCountsAlso(child)));
		return copy;
	}

	private static com.gielinorgoals.model.Requirement plain(TemplateNode node)
	{
		if (node.getRequirement() instanceof ItemRequirement)
		{
			return ((ItemRequirement) node.getRequirement()).withCountsAlso(null);
		}
		return node.getRequirement();
	}

	private static Goal findMarks(GoalManager manager)
	{
		for (Goal goal : manager.getTree().flatten())
		{
			if (goal.getRequirement() instanceof ItemRequirement
				&& ((ItemRequirement) goal.getRequirement()).getItemId() == ItemID.GRACE)
			{
				return goal;
			}
		}
		throw new AssertionError("No marks goal");
	}

	private static GameData items(int marks)
	{
		return new GameData()
		{
			@Override
			public long getXp(Skill skill)
			{
				return -1;
			}

			@Override
			public long getItemCount(int itemId)
			{
				return itemId == ItemID.GRACE ? marks : SET.contains(itemId) ? 1 : 0;
			}
		};
	}
}
