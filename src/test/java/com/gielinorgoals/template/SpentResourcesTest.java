package com.gielinorgoals.template;

import com.gielinorgoals.GoalManager;
import com.gielinorgoals.TestGoalManagers;
import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalEvaluator;
import com.gielinorgoals.model.GoalProgress;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.persistence.GoalSerializer;
import com.gielinorgoals.persistence.SchemaMigrator;
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
 * Resurser och poäng som redan gått åt (spel-testet av Mid ironman): zenyte-shards i ett smycke
 * och Pest Control-poäng i elite void ska räknas som klara.
 */
public class SpentResourcesTest
{
	private final Gson gson = new Gson();
	private final TemplateCatalog catalog = new TemplateCatalog(gson);

	@Test
	public void zenyteInJewelleryCountsTowardShardsAndOnyx()
	{
		TemplateNode node = catalog.find("zenyte-jewellery").build(Map.of());
		GameData anguish = items(Set.of(ItemID.ZENYTE_NECKLACE_ENCHANTED));
		GoalProgress shards = item(node, ItemID.ZENYTE_SHARD).progress(anguish);
		GoalProgress onyx = item(node, ItemID.UNCUT_ONYX).progress(anguish);
		assertEquals(1, shards.getCurrent());
		assertEquals(1, onyx.getCurrent());
		assertFalse(shards.isComplete());
	}

	@Test
	public void eliteVoidPointsAreDoneWithBothPieces()
	{
		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, catalog.find("elite-void").build(Map.of()));
		Goal points = points(tree, 400);

		GoalEvaluator.evaluate(tree, items(Set.of(ItemID.ELITE_VOID_KNIGHT_TOP)), 1L, TemplateKeys::of);
		assertFalse("one piece is not enough", points.isCompleted());
		GoalEvaluator.evaluate(tree, items(Set.of(ItemID.ELITE_VOID_KNIGHT_TOP, ItemID.ELITE_VOID_KNIGHT_ROBES)), 2L,
			TemplateKeys::of);
		assertTrue(points.isCompleted());
	}

	@Test
	public void anOldEliteVoidInsertGetsTheChangeThroughThePackage()
	{
		GoalManager manager = TestGoalManagers.inMemory("profile");
		TemplateUpdates updates = new TemplateUpdates(catalog, QuestRequirementsDb.load(gson));
		// Mid ironman som den såg ut före ändringen: poängmålet utan doneWhen.
		TemplateNode node = catalog.find("package-mid-iron").build(Map.of());
		node.setTemplateId("package-mid-iron");
		node.setTemplateParams(Map.of(TemplateUpdates.QUEST_CHAINS_PARAM, 0));
		stripDoneWhen(node);
		Goal root = manager.insertTemplate(node).getRoot();
		Goal points = points(manager.getTree(), 400);

		TemplateUpdates.Plan plan = updates.plan(manager.getTree(), root);
		assertTrue(plan.getChanges().stream().anyMatch(change -> change.getGoal() == points));
		manager.applyTemplateUpdate(root.getId(), List.of(), List.of(), List.of(), plan.getTemplateVersion(),
			plan.getChanges());
		assertTrue(Boolean.TRUE.equals(points.getDoneWhenAll()));
		assertEquals(List.of("item:13072", "item:13073"), points.getDoneWhen());
	}

	@Test
	public void doneWhenAllIsSavedAndLoaded()
	{
		GoalSerializer serializer = new GoalSerializer(gson, new SchemaMigrator());
		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, catalog.find("elite-void").build(Map.of()));
		Goal points = points(tree, 400);
		GoalTree loaded = serializer.deserialize(serializer.serialize(tree)).getTree();
		Goal back = loaded.find(points.getId());
		assertTrue(Boolean.TRUE.equals(back.getDoneWhenAll()));
		assertEquals(15, SchemaMigrator.CURRENT_VERSION);
	}

	// Tar bort doneWhen på Pest Control-målen, som i version 1 av void-templates.
	private static void stripDoneWhen(TemplateNode node)
	{
		for (TemplateNode child : node.getChildren())
		{
			if (child.getRequirement() instanceof KillCountRequirement
				&& ((KillCountRequirement) child.getRequirement()).getActivityName().equals("Pest Control points"))
			{
				child.setDoneWhen(null);
				child.setDoneWhenAll(false);
			}
			stripDoneWhen(child);
		}
	}

	private static Goal points(GoalTree tree, int count)
	{
		for (Goal goal : tree.flatten())
		{
			if (KillCountRequirement.manual("Pest Control points", count).isSameGoal(goal.getRequirement()))
			{
				return goal;
			}
		}
		throw new AssertionError("No points goal");
	}

	private static ItemRequirement item(TemplateNode node, int id)
	{
		if (node.getRequirement() instanceof ItemRequirement && ((ItemRequirement) node.getRequirement()).getItemId() == id)
		{
			return (ItemRequirement) node.getRequirement();
		}
		for (TemplateNode child : node.getChildren())
		{
			try
			{
				return item(child, id);
			}
			catch (AssertionError ignored)
			{
				// Letar vidare.
			}
		}
		throw new AssertionError("No item " + id);
	}

	private static GameData items(Set<Integer> owned)
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
				return owned.contains(itemId) ? 1 : 0;
			}
		};
	}
}
