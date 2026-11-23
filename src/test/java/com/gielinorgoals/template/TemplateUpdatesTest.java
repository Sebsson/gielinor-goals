package com.gielinorgoals.template;

import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.SkillRequirement;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TemplateUpdatesTest
{
	private final TemplateCatalog catalog = new TemplateCatalog(new Gson());
	private final QuestRequirementsDb db = QuestRequirementsDb.load(new Gson());
	private final TemplateUpdates updates = new TemplateUpdates(catalog, db);

	@Test
	public void keysAreStableAndReadable()
	{
		assertEquals("quest:117", TemplateKeys.of(new QuestRequirement(117, "Recipe for Disaster")));
		assertEquals("skill:ATTACK:70", TemplateKeys.of(SkillRequirement.level(Skill.ATTACK, 70)));
		assertEquals("item:4587", TemplateKeys.of(new ItemRequirement(4587, "Dragon scimitar", 1, false)));
		assertEquals("item:995:1000", TemplateKeys.of(new ItemRequirement(995, "Coins", 1000, false)));
		assertEquals("diary:VARROCK:HARD", TemplateKeys.of(new DiaryRequirement(DiaryArea.VARROCK, DiaryTier.HARD)));
		assertEquals("group:varrock diary", TemplateKeys.of(TemplateNode.aggregate("Varrock diary")));
	}

	@Test
	public void freshlyInsertedTemplateHasNoUpdate()
	{
		for (String id : List.of("quest-cape", "diary-cape", "base-level"))
		{
			GoalTree tree = new GoalTree();
			Goal root = insert(tree, id, Map.of(), false);
			assertTrue(id, updates.plan(tree, root).isEmpty());
		}
	}

	@Test
	public void barrowsGlovesWithChainsAndAnExistingQuestHasNoUpdate()
	{
		GoalTree tree = new GoalTree();
		Goal existing = new Goal("");
		existing.setRequirement(new QuestRequirement(Quest.NATURE_SPIRIT.getId(), Quest.NATURE_SPIRIT.getName()));
		tree.add(null, existing);

		Goal root = insert(tree, "barrows-gloves", Map.of(), true);

		TemplateUpdates.Plan plan = updates.plan(tree, root);
		assertTrue("additions " + plan.getAdditions().size() + ", removed " + plan.getRemoved().size(), plan.isEmpty());
	}

	@Test
	public void deletedGoalComesBackAsAdditionUnderItsParent()
	{
		GoalTree tree = new GoalTree();
		Goal root = insert(tree, "diary-cape", Map.of(), false);
		Goal varrock = child(tree, root, "Varrock diary");
		Goal varrockHard = child(tree, varrock, "Varrock Hard diary");
		tree.remove(varrockHard.getId());

		TemplateUpdates.Plan plan = updates.plan(tree, root);

		assertEquals(1, plan.getAdditions().size());
		assertEquals("diary:VARROCK:HARD", plan.getAdditions().get(0).getNode().getKey());
		assertEquals(varrock.getId(), plan.getAdditions().get(0).getParentGoalId());
		assertTrue(plan.getRemoved().isEmpty());
		assertTrue(updates.hasUpdate(tree, root, 1));
	}

	@Test
	public void uncheckedNodesAtInsertAreNotSuggestedLater()
	{
		GoalTree tree = new GoalTree();
		TemplateNode node = catalog.find("diary-cape").build(Map.of());
		node.getChildren().get(0).getChildren().get(3).setIncluded(false);
		node.setTemplateId("diary-cape");
		node.setTemplateParams(Map.of());
		Goal root = TemplateInserter.insert(tree, node).getRoot();

		assertEquals(1, root.getTemplateIgnoredKeys().size());
		assertTrue(updates.plan(tree, root).isEmpty());
	}

	@Test
	public void goalsNoLongerInTheTemplateAreListedAsRemoved()
	{
		GoalTree tree = new GoalTree();
		Goal root = insert(tree, "base-level", Map.of("level", 70), false);
		Goal attack = tree.getChildren(root).get(0);
		// Som om templaten tidigare hade en nod som den inte har längre.
		attack.setTemplateNodeKey("skill:ATTACK:71");

		TemplateUpdates.Plan plan = updates.plan(tree, root);

		assertEquals(List.of(attack), plan.getRemoved());
		// Templatens Attack 70 har ingen motsvarande nyckel under roten och föreslås som tillägg;
		// vid insättning kopplas den till det befintliga målet med samma krav.
		assertEquals(1, plan.getAdditions().size());
		assertEquals("skill:ATTACK:70", plan.getAdditions().get(0).getNode().getKey());
	}

	@Test
	public void parametersAreKeptWhenRebuilding()
	{
		GoalTree tree = new GoalTree();
		Goal root = insert(tree, "base-level", Map.of("level", 85), false);

		assertEquals("Base 85", root.getTitle());
		assertEquals("Base 85", updates.rebuild(root).getTitle());
		assertTrue(updates.plan(tree, root).isEmpty());
	}

	@Test
	public void missingTemplateGivesNoPlan()
	{
		GoalTree tree = new GoalTree();
		Goal root = new Goal("Old");
		root.setTemplateId("removed-template");
		tree.add(null, root);

		assertNull(updates.plan(tree, root));
	}

	private Goal insert(GoalTree tree, String id, Map<String, Integer> params, boolean chains)
	{
		GoalTemplate template = catalog.find(id);
		TemplateNode node = template.build(params);
		if (chains)
		{
			QuestChains.expand(node, db, tree);
		}
		Map<String, Integer> stored = new HashMap<>(params);
		stored.put(TemplateUpdates.QUEST_CHAINS_PARAM, chains ? 1 : 0);
		node.setTemplateId(id);
		node.setTemplateParams(stored);
		return TemplateInserter.insert(tree, node).getRoot();
	}

	private static Goal child(GoalTree tree, Goal parent, String title)
	{
		for (Goal goal : tree.getChildren(parent))
		{
			if (goal.getDisplayTitle().equals(title))
			{
				return goal;
			}
		}
		throw new AssertionError("No child " + title);
	}
}
