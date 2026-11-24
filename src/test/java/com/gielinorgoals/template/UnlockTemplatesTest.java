package com.gielinorgoals.template;

import com.gielinorgoals.GoalManager;
import com.gielinorgoals.TestGoalManagers;
import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.PointsActivity;
import com.gielinorgoals.model.PointsRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * De 17 upplåsnings-templates i våg 2 (milstolpe 13, del 2): item-id mot gameval, quests mot
 * enumen, poängkrav, delade noder och tyst avbockning av det som redan är klart.
 */
public class UnlockTemplatesTest
{
	private final Gson gson = new Gson();
	private final TemplateCatalog catalog = new TemplateCatalog(gson);
	private final QuestRequirementsDb db = QuestRequirementsDb.load(gson);

	@Test
	public void itemIdsMatchGameval()
	{
		Map<String, List<Integer>> expected = new HashMap<>();
		expected.put("prifddinas", List.of());
		expected.put("lunar-spellbook", List.of());
		expected.put("ancient-magicks", List.of());
		expected.put("arceuus-spellbook", List.of());
		expected.put("fossil-island", List.of(ItemID.FOSSIL_NUMULITE));
		expected.put("miscellania", List.of());
		expected.put("ectophial", List.of(ItemID.ECTOPHIAL));
		expected.put("dragon-scimitar", List.of(ItemID.COINS, ItemID.DRAGON_SCIMITAR));
		expected.put("spirit-trees", List.of());
		expected.put("gnome-gliders", List.of());
		expected.put("bonecrusher", List.of(ItemID.BONECRUSHER));
		expected.put("ash-sanctifier", List.of(ItemID.ASH_SANCTIFIER));
		expected.put("herb-sack", List.of(ItemID.SLAYER_HERB_SACK));
		expected.put("seed-box", List.of(ItemID.SEED_BOX));
		expected.put("slayer-helm", List.of(ItemID.HARMLESS_BLACK_MASK, ItemID.SLAYER_EARMUFFS, ItemID.SLAYER_FACEMASK,
			ItemID.SLAYER_NOSEPEG, ItemID.WALLBEAST_SPIKE_HELMET, ItemID.SLAYER_GEM, ItemID.SLAYER_HELM));
		expected.put("void-knight", List.of(ItemID.PEST_VOID_KNIGHT_TOP, ItemID.PEST_VOID_KNIGHT_ROBES,
			ItemID.PEST_VOID_KNIGHT_GLOVES, ItemID.GAME_PEST_MAGE_HELM, ItemID.GAME_PEST_ARCHER_HELM,
			ItemID.GAME_PEST_MELEE_HELM));
		expected.put("elite-void", List.of(ItemID.ELITE_VOID_KNIGHT_TOP, ItemID.PEST_VOID_KNIGHT_TOP,
			ItemID.ELITE_VOID_KNIGHT_ROBES, ItemID.PEST_VOID_KNIGHT_ROBES));

		for (Map.Entry<String, List<Integer>> entry : expected.entrySet())
		{
			GoalTemplate template = catalog.find(entry.getKey());
			assertTrue(entry.getKey(), template != null);
			List<Integer> items = new ArrayList<>();
			collectItems(template.build(Map.of()), items);
			assertEquals(entry.getKey(), entry.getValue(), items);
			assertTrue(entry.getKey(), template.getCategory().startsWith("Ironman"));
		}
	}

	@Test
	public void questsMatchTheEnum()
	{
		Map<String, List<Quest>> expected = Map.of(
			"prifddinas", List.of(Quest.SONG_OF_THE_ELVES),
			"lunar-spellbook", List.of(Quest.LUNAR_DIPLOMACY),
			"ancient-magicks", List.of(Quest.DESERT_TREASURE_I),
			"arceuus-spellbook", List.of(Quest.A_KINGDOM_DIVIDED),
			"fossil-island", List.of(Quest.BONE_VOYAGE),
			"miscellania", List.of(Quest.THRONE_OF_MISCELLANIA, Quest.ROYAL_TROUBLE),
			"ectophial", List.of(Quest.GHOSTS_AHOY),
			"dragon-scimitar", List.of(Quest.MONKEY_MADNESS_I),
			"spirit-trees", List.of(Quest.TREE_GNOME_VILLAGE, Quest.THE_GRAND_TREE, Quest.SONG_OF_THE_ELVES,
				Quest.PANDEMONIUM),
			"gnome-gliders", List.of(Quest.THE_GRAND_TREE, Quest.MONKEY_MADNESS_II));
		for (Map.Entry<String, List<Quest>> entry : expected.entrySet())
		{
			List<Integer> ids = new ArrayList<>();
			collectQuests(catalog.find(entry.getKey()).build(Map.of()), ids);
			List<Integer> want = new ArrayList<>();
			entry.getValue().forEach(quest -> want.add(quest.getId()));
			assertEquals(entry.getKey(), want, ids);
		}
		// Delvis klara quests är manuella mål som blir klara med hela questen.
		assertTrue(doneWhenKeys(catalog.find("spirit-trees").build(Map.of())).contains(
			"quest:" + Quest.THE_PATH_OF_GLOUPHRIE.getId()));
		assertTrue(doneWhenKeys(catalog.find("gnome-gliders").build(Map.of())).contains(
			"quest:" + Quest.ONE_SMALL_FAVOUR.getId()));
	}

	@Test
	public void pointsDiariesAndSkillsAreRight()
	{
		assertTrue(requirements("seed-box").contains(new PointsRequirement(PointsActivity.TITHE_FARM, 250)));
		assertTrue(requirements("herb-sack").contains(new PointsRequirement(PointsActivity.SLAYER, 750)));
		assertTrue(requirements("slayer-helm").contains(new PointsRequirement(PointsActivity.SLAYER, 400)));
		assertTrue(requirements("bonecrusher").contains(new DiaryRequirement(DiaryArea.MORYTANIA, DiaryTier.HARD)));
		assertTrue(requirements("ash-sanctifier").contains(new DiaryRequirement(DiaryArea.KOUREND, DiaryTier.HARD)));
		assertTrue(requirements("elite-void").contains(new DiaryRequirement(DiaryArea.WESTERN, DiaryTier.HARD)));
		assertTrue(requirements("slayer-helm").contains(com.gielinorgoals.model.SkillRequirement.level(Skill.CRAFTING, 55)));
		assertTrue(requirements("spirit-trees").contains(com.gielinorgoals.model.SkillRequirement.level(Skill.SAILING, 58)));
		List<Object> voidReqs = requirements("void-knight");
		for (Skill skill : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED,
			Skill.MAGIC})
		{
			assertTrue(skill.getName(), voidReqs.contains(com.gielinorgoals.model.SkillRequirement.level(skill, 42)));
		}
		assertTrue(voidReqs.contains(com.gielinorgoals.model.SkillRequirement.level(Skill.PRAYER, 22)));
	}

	@Test
	public void sharedNodesAreLinkedNotCopied()
	{
		GoalTree tree = new GoalTree();
		// Prifddinas med hela Song of the Elves-kedjan, sedan spirit trees: questen och kedjan kopplas in.
		TemplateInserter.insert(tree, QuestChains.expand(catalog.find("prifddinas").build(Map.of()), db, tree));
		int before = count(tree, "quest:" + Quest.SONG_OF_THE_ELVES.getId());
		TemplateInserter.insert(tree, QuestChains.expand(catalog.find("spirit-trees").build(Map.of()), db, tree));
		assertEquals(1, before);
		assertEquals(1, count(tree, "quest:" + Quest.SONG_OF_THE_ELVES.getId()));
		assertEquals(1, count(tree, "quest:" + Quest.MOURNINGS_END_PART_II.getId()));
		Goal sote = find(tree, "quest:" + Quest.SONG_OF_THE_ELVES.getId());
		assertEquals(2, tree.getParents(sote.getId()).size());

		// Void knight top och robe är delade delnoder i Elite void.
		TemplateInserter.insert(tree, catalog.find("void-knight").build(Map.of()));
		TemplateInserter.Result elite = TemplateInserter.insert(tree, catalog.find("elite-void").build(Map.of()));
		assertEquals(1, count(tree, "item:" + ItemID.PEST_VOID_KNIGHT_TOP));
		assertEquals(1, count(tree, "item:" + ItemID.PEST_VOID_KNIGHT_ROBES));
		assertEquals(2, elite.getLinked().size());
		Goal top = find(tree, "item:" + ItemID.PEST_VOID_KNIGHT_TOP);
		assertEquals(2, tree.getParents(top.getId()).size());

		// Diary-mål delas med t.ex. "All diaries in a tier".
		TemplateInserter.insert(tree, catalog.find("diary-tier").build(Map.of(), Map.of("tier", "HARD")));
		TemplateInserter.Result bonecrusher = TemplateInserter.insert(tree, catalog.find("bonecrusher").build(Map.of()));
		assertEquals(1, count(tree, "diary:MORYTANIA:HARD"));
		assertEquals(1, bonecrusher.getLinked().size());
	}

	@Test
	public void ownedSlayerHelmetCompletesItsPartsQuietly()
	{
		GoalManager manager = TestGoalManagers.inMemory("profile");
		List<List<Goal>> announced = new ArrayList<>();
		manager.setCompletionListener(announced::add);
		manager.updateGameData("profile", new GameData()
		{
			@Override
			public long getXp(Skill skill)
			{
				return -1;
			}

			@Override
			public long getItemCount(int itemId)
			{
				return itemId == ItemID.SLAYER_HELM ? 1 : 0;
			}
		});

		manager.insertTemplate(catalog.find("slayer-helm").build(Map.of()));
		// Hjälmen, delarna och upplåsningen (doneWhen) blir klara med ungefärligt datum, utan popups.
		for (String key : List.of("item:" + ItemID.SLAYER_HELM, "item:" + ItemID.HARMLESS_BLACK_MASK,
			"item:" + ItemID.SLAYER_EARMUFFS, "points:SLAYER:400"))
		{
			Goal goal = find(manager.getTree(), key);
			assertTrue(key, goal.isCompleted() && goal.isCompletedAtApprox());
		}
		assertTrue(announced.isEmpty());
	}

	@Test
	public void nicknamesFindTheTemplates()
	{
		List<GoalTemplate> templates = catalog.getTemplates();
		Map<String, String> nicknames = new HashMap<>();
		nicknames.put("prif", "Prifddinas access");
		nicknames.put("lunars", "Lunar spellbook");
		nicknames.put("ancients", "Ancient Magicks");
		nicknames.put("bonecrusher", "Bonecrusher");
		nicknames.put("dscim", "Dragon scimitar");
		nicknames.put("ecto", "Ectophial");
		nicknames.put("misc", "Kingdom of Miscellania");
		nicknames.put("sts", "Spirit trees");
		nicknames.put("gliders", "Gnome gliders");
		nicknames.put("slay helm", "Slayer helmet");
		nicknames.put("elite void", "Elite void");
		for (Map.Entry<String, String> entry : nicknames.entrySet())
		{
			Set<String> found = new TreeSet<>();
			TemplateCategories.group(templates, entry.getKey(), false).values()
				.forEach(list -> list.forEach(template -> found.add(template.getName())));
			assertTrue(entry.getKey() + " -> " + found, found.contains(entry.getValue()));
		}
	}

	private List<Object> requirements(String id)
	{
		List<Object> out = new ArrayList<>();
		collectRequirements(catalog.find(id).build(Map.of()), out);
		return out;
	}

	private static void collectRequirements(TemplateNode node, List<Object> out)
	{
		if (node.getRequirement() != null)
		{
			out.add(node.getRequirement());
		}
		node.getChildren().forEach(child -> collectRequirements(child, out));
	}

	private static void collectItems(TemplateNode node, List<Integer> out)
	{
		if (node.getRequirement() instanceof ItemRequirement)
		{
			out.add(((ItemRequirement) node.getRequirement()).getItemId());
		}
		node.getChildren().forEach(child -> collectItems(child, out));
	}

	private static void collectQuests(TemplateNode node, List<Integer> out)
	{
		if (node.getRequirement() instanceof QuestRequirement)
		{
			out.add(((QuestRequirement) node.getRequirement()).getQuestId());
		}
		node.getChildren().forEach(child -> collectQuests(child, out));
	}

	private static List<String> doneWhenKeys(TemplateNode node)
	{
		List<String> out = new ArrayList<>();
		if (node.getDoneWhen() != null)
		{
			out.addAll(node.getDoneWhen());
		}
		node.getChildren().forEach(child -> out.addAll(doneWhenKeys(child)));
		return out;
	}

	private static int count(GoalTree tree, String key)
	{
		int count = 0;
		for (Goal goal : tree.flatten())
		{
			count += key.equals(TemplateKeys.of(goal)) ? 1 : 0;
		}
		return count;
	}

	private static Goal find(GoalTree tree, String key)
	{
		for (Goal goal : tree.flatten())
		{
			if (key.equals(TemplateKeys.of(goal)))
			{
				return goal;
			}
		}
		throw new AssertionError("No goal " + key);
	}
}
