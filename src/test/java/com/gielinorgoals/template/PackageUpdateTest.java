package com.gielinorgoals.template;

import com.gielinorgoals.model.CompletionOverlay;
import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalEvaluator;
import com.gielinorgoals.model.GoalTree;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Regressioner från spel-testet av paketen: "update" direkt efter insättningen, och quests och
 * fairy rings som inte syntes som klara.
 */
public class PackageUpdateTest
{
	private final Gson gson = new Gson();
	private final TemplateCatalog catalog = new TemplateCatalog(gson);
	private final QuestRequirementsDb db = QuestRequirementsDb.load(gson);
	private final TemplateUpdates updates = new TemplateUpdates(catalog, db);

	@Test
	public void freshPackageHasNoUpdateRightAfterInsert()
	{
		for (String id : List.of("package-early-iron", "package-mid-iron", "package-late-iron", "package-all-capes"))
		{
			GoalTree tree = new GoalTree();
			TemplateInserter.insert(tree, asInDialog(tree, id));
			assertEquals(id, List.of(), rootsWithUpdates(tree));
		}
	}

	@Test
	public void packageAfterQuestCapeHasNoUpdate()
	{
		GoalTree tree = new GoalTree();
		// Quest cape som platt lista (utan kedjor), som när man har alla quests som mål sedan tidigare.
		TemplateNode questCape = catalog.find("quest-cape").build(Map.of());
		questCape.setTemplateId("quest-cape");
		questCape.setTemplateParams(Map.of(TemplateUpdates.QUEST_CHAINS_PARAM, 0));
		TemplateInserter.insert(tree, questCape);

		TemplateInserter.insert(tree, asInDialog(tree, "package-early-iron"));
		TemplateInserter.insert(tree, asInDialog(tree, "package-all-capes"));
		assertEquals(List.of(), rootsWithUpdates(tree));
	}

	@Test
	public void aRealChangeIsStillFoundAfterTheFix()
	{
		GoalTree tree = new GoalTree();
		Goal root = TemplateInserter.insert(tree, asInDialog(tree, "package-early-iron")).getRoot();
		// Användaren tar bort ett mål som paketet skapade: uppdateringen föreslår det igen.
		Goal scimitar = null;
		for (Goal goal : tree.flatten())
		{
			if ("item:4587".equals(TemplateKeys.of(goal)))
			{
				scimitar = goal;
			}
		}
		tree.remove(scimitar.getId());
		assertTrue(updates.hasUpdate(tree, root, 1));
	}

	@Test
	public void doneWhenIsMetFromTheGameWithoutAGoal()
	{
		// Fairytale II-steget i Ardougne cloak-templaten är klart när questen är klar i spelet.
		TemplateNode node = catalog.find("ardougne-cloak").build(Map.of());
		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, node);
		GameData finished = quests(Map.of(Quest.FAIRYTALE_II__CURE_A_QUEEN.getId(), QuestState.FINISHED));
		GoalEvaluator.evaluate(tree, finished, 1L, TemplateKeys::of);
		Goal step = null;
		for (Goal goal : tree.flatten())
		{
			if (goal.getTitle().startsWith("Fairytale II"))
			{
				step = goal;
			}
		}
		assertTrue(step.isCompleted());

		// Och förhandsvisningen visar det som klart.
		TemplatePlanner.Plan plan = TemplatePlanner.plan(new GoalTree(), catalog.find("ardougne-cloak").build(Map.of()),
			finished);
		assertTrue(plan.getAlreadyDone() > 0);
	}

	@Test
	public void doneWhenQuestsAndDiariesAreWatched()
	{
		// Annars läses Fairytale II aldrig, eftersom det inte är ett eget mål.
		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, catalog.find("ardougne-cloak").build(Map.of()));
		com.gielinorgoals.tracking.WatchedAchievements watched = com.gielinorgoals.tracking.WatchedAchievements.from(tree);
		assertTrue(watched.getQuestIds().contains(Quest.FAIRYTALE_II__CURE_A_QUEEN.getId()));
		assertTrue(watched.getVarbits().contains(com.gielinorgoals.model.DiaryArea.LUMBRIDGE.varbitFor(
			com.gielinorgoals.model.DiaryTier.ELITE)));
	}

	@Test
	public void previewUsesFullQuestStatus()
	{
		GameData base = quests(Map.of());
		GameData status = quests(Map.of(Quest.COOKS_ASSISTANT.getId(), QuestState.FINISHED));
		CompletionOverlay overlay = new CompletionOverlay(base, status);
		assertEquals(QuestState.FINISHED, overlay.getQuestState(Quest.COOKS_ASSISTANT.getId()));
		assertEquals(null, base.getQuestState(Quest.COOKS_ASSISTANT.getId()));
		assertFalse(overlay.getVarbit(1) >= 0);
	}

	// Som template-dialogen: kedjor påslagna, templateId och kedjeval på roten och delarna.
	private TemplateNode asInDialog(GoalTree tree, String id)
	{
		TemplateNode node = catalog.find(id).build(Map.of());
		QuestChains.expand(node, db, tree);
		node.setTemplateId(id);
		node.setTemplateParams(Map.of(TemplateUpdates.QUEST_CHAINS_PARAM, 1));
		markChains(node);
		return node;
	}

	private static void markChains(TemplateNode node)
	{
		for (TemplateNode child : node.getChildren())
		{
			if (child.getTemplateId() != null && child.getTemplateParams() != null)
			{
				Map<String, Integer> params = new HashMap<>(child.getTemplateParams());
				params.put(TemplateUpdates.QUEST_CHAINS_PARAM, 1);
				child.setTemplateParams(params);
			}
			markChains(child);
		}
	}

	private List<String> rootsWithUpdates(GoalTree tree)
	{
		List<String> out = new ArrayList<>();
		for (Goal goal : tree.flatten())
		{
			if (goal.getTemplateId() != null && !"quest-cape".equals(goal.getTemplateId()))
			{
				TemplateUpdates.Plan plan = updates.plan(tree, goal);
				if (plan != null && !plan.isEmpty())
				{
					List<String> keys = new ArrayList<>();
					plan.getAdditions().forEach(addition -> keys.add("+" + addition.getNode().getKey()));
					plan.getRemoved().forEach(removed -> keys.add("-" + TemplateKeys.of(removed)));
					out.add(goal.getTemplateId() + " " + keys.subList(0, Math.min(5, keys.size())));
				}
			}
		}
		return out;
	}

	private static GameData quests(Map<Integer, QuestState> states)
	{
		return new GameData()
		{
			@Override
			public long getXp(Skill skill)
			{
				return -1;
			}

			@Override
			public QuestState getQuestState(int questId)
			{
				return states.get(questId);
			}
		};
	}
}
