package com.gielinorgoals.template;

import com.gielinorgoals.model.DropSource;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.SkillRequirement;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.hiscore.HiscoreSkill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Siffrorna i ironman-templaten mot wikin (se _comment i varje fil) och item-id:n mot gameval.
 */
public class IronmanTemplatesTest
{
	private static final List<GoalTemplate> TEMPLATES = JsonTemplates.load(new Gson());

	@Test
	public void bowfaAndCrystalArmour()
	{
		TemplateNode root = build("bowfa-crystal");
		assertEquals(List.of("Unlock", "Obtain"), titles(root));

		TemplateNode unlock = root.getChildren().get(0);
		assertEquals(quest(Quest.SONG_OF_THE_ELVES), unlock.getChildren().get(0).getRequirement());
		assertEquals(SkillRequirement.level(Skill.SMITHING, 82), unlock.getChildren().get(1).getRequirement());
		assertEquals(SkillRequirement.level(Skill.CRAFTING, 82), unlock.getChildren().get(2).getRequirement());
		assertTrue(KillCountRequirement.hiscore(HiscoreSkill.THE_GAUNTLET, 1)
			.isSameGoal(unlock.getChildren().get(3).getRequirement()));

		List<TemplateNode> obtain = root.getChildren().get(1).getChildren();
		assertItem(obtain.get(0), ItemID.PRIF_WEAPON_SEED_ENHANCED, 1);
		assertEquals(DropSource.hiscore(HiscoreSkill.THE_CORRUPTED_GAUNTLET, 400), obtain.get(0).getDropSource());
		assertItem(obtain.get(1), ItemID.PRIF_ARMOUR_SEED, 6);
		assertEquals(DropSource.hiscore(HiscoreSkill.THE_CORRUPTED_GAUNTLET, 50), obtain.get(1).getDropSource());
		assertItem(obtain.get(2), ItemID.PRIF_CRYSTAL_SHARD, 400);
		assertTrue(KillCountRequirement.hiscore(HiscoreSkill.THE_CORRUPTED_GAUNTLET, 400)
			.isSameGoal(obtain.get(3).getRequirement()));
		assertItem(obtain.get(4), ItemID.BOW_OF_FAERDHINEN_INACTIVE, 1);
		assertItem(obtain.get(5), ItemID.CRYSTAL_HELMET_INACTIVE, 1);
		assertItem(obtain.get(6), ItemID.CRYSTAL_CHESTPLATE_INACTIVE, 1);
		assertItem(obtain.get(7), ItemID.CRYSTAL_PLATELEGS_INACTIVE, 1);
	}

	@Test
	public void dragonWarhammer()
	{
		TemplateNode root = build("dragon-warhammer");
		assertEquals(List.of("Unlock", "Obtain"), titles(root));
		assertNull(root.getChildren().get(0).getChildren().get(0).getRequirement());

		TemplateNode dwh = root.getChildren().get(1).getChildren().get(0);
		assertItem(dwh, ItemID.DRAGON_WARHAMMER, 1);
		assertEquals(DropSource.npc("Lizardman shaman", 3000), dwh.getDropSource());
		// Samma räknare som ett KC-mål på NPC:n, och den som loot-eventet räknar upp.
		assertEquals("npc:lizardman shaman", dwh.getDropSource().counterKey());
	}

	@Test
	public void doomOfMokhaiotl()
	{
		TemplateNode root = build("doom-of-mokhaiotl");
		assertEquals(quest(Quest.THE_FINAL_DAWN), root.getChildren().get(0).getChildren().get(0).getRequirement());

		TemplateNode uniques = root.getChildren().get(1).getChildren().get(0);
		assertEquals("Unique drops", uniques.getTitle());
		assertTrue(uniques.isAggregate());
		int[][] expected = {
			{ItemID.AVERNIC_TREADS, 540},
			{ItemID.EYE_OF_AYAK_UNCHARGED, 540},
			{ItemID.MOKHAIOTL_CLOTH, 540},
			{ItemID.DOMPET, 250},
		};
		assertUniques(uniques, HiscoreSkill.DOOM_OF_MOKHAIOTL, expected);
	}

	@Test
	public void theHueycoatl()
	{
		TemplateNode root = build("the-hueycoatl");
		assertNull(root.getChildren().get(0).getChildren().get(0).getRequirement());

		TemplateNode uniques = root.getChildren().get(1).getChildren().get(0);
		assertItem(uniques.getChildren().get(0), ItemID.DRAGONHUNTER_WAND, 1);
		assertEquals(DropSource.hiscore(HiscoreSkill.THE_HUEYCOATL, 105), uniques.getChildren().get(0).getDropSource());
		assertItem(uniques.getChildren().get(1), ItemID.HUEY_HIDE, 3);
		assertEquals(DropSource.hiscore(HiscoreSkill.THE_HUEYCOATL, 29).withPerDrop(3),
			uniques.getChildren().get(1).getDropSource());
		assertItem(uniques.getChildren().get(2), ItemID.TOME_OF_EARTH_UNCHARGED, 1);
		assertEquals(DropSource.hiscore(HiscoreSkill.THE_HUEYCOATL, 90), uniques.getChildren().get(2).getDropSource());
	}

	@Test
	public void nicknamesFindTheTemplates()
	{
		assertEquals(List.of("Bow of Faerdhinen + crystal armour"), search("bowfa"));
		assertEquals(List.of("Dragon warhammer"), search("dwh"));
		assertEquals(List.of("Doom of Mokhaiotl"), search("doom"));
		assertEquals(List.of("The Hueycoatl"), search("huey"));
	}

	@Test
	public void categories()
	{
		Map<String, List<GoalTemplate>> grouped = TemplateCategories.group(TEMPLATES, "", false);
		assertEquals(List.of("Fire cape", "Dragon defender", "Fighter torso", "Graceful outfit", "Rune pouch",
			"Ardougne cloak and fairy rings", "Arceuus spellbook", "Ectophial", "Gnome gliders", "Herb sack", "Seed box"),
			names(grouped.get(TemplateCategories.IRONMAN_EARLY)));
		assertEquals(List.of("Barrows gloves", "Bow of Faerdhinen + crystal armour", "Dragon warhammer",
			"The Hueycoatl", "Ava's assembler", "Imbued god cape", "Lunar spellbook", "Ancient Magicks", "Fossil Island",
			"Kingdom of Miscellania", "Dragon scimitar", "Spirit trees", "Bonecrusher", "Ash sanctifier", "Slayer helmet",
			"Void Knight equipment", "Toxic blowpipe + Serpentine helm", "Trident of the seas", "Trident of the swamp",
			"Moons of Peril", "Dragonfire shield"), names(grouped.get(TemplateCategories.IRONMAN_MID)));
		assertEquals(List.of("Doom of Mokhaiotl", "Infernal cape", "Zenyte jewellery", "Prifddinas access", "Elite void",
			"Dragon hunter crossbow", "Sunfire fanatic armour", "Tormented bracelet + Amulet of torture",
			"Desert Treasure II rings"), names(grouped.get(TemplateCategories.IRONMAN_LATE)));
	}

	@Test
	public void questChainsExpandAndShareSongOfTheElves()
	{
		QuestRequirementsDb db = QuestRequirementsDb.load(new Gson());
		GoalTree tree = new GoalTree();
		TemplateNode bowfa = build("bowfa-crystal");
		QuestChains.expand(bowfa, db, tree);
		TemplateInserter.insert(tree, bowfa, null);

		// Song of the Elves kedja finns nu; en ny template som vill ha samma quest kopplar in samma mål.
		Goal sote = findQuest(tree, Quest.SONG_OF_THE_ELVES);
		assertTrue(sote.hasChildren());
		TemplateNode again = TemplateNode.aggregate("Other");
		again.add(TemplateNode.requirement(quest(Quest.SONG_OF_THE_ELVES)));
		TemplateInserter.insert(tree, again, null);
		assertEquals(2, tree.getParents(sote.getId()).size());

		TemplateNode doom = build("doom-of-mokhaiotl");
		QuestChains.expand(doom, db, tree);
		Set<Integer> quests = new HashSet<>();
		collectQuests(doom, quests);
		for (Quest quest : new Quest[]{Quest.THE_FINAL_DAWN, Quest.THE_HEART_OF_DARKNESS, Quest.PERILOUS_MOONS,
			Quest.TWILIGHTS_PROMISE, Quest.CHILDREN_OF_THE_SUN})
		{
			assertTrue(quest.getName(), quests.contains(quest.getId()));
		}
	}

	@Test
	public void everyNodeInEveryTemplateHasAnIcon()
	{
		// Mål utan krav visar sin egen ikon; utan någon av dem blir raden tom bredvid titeln.
		for (GoalTemplate template : TestCombatAchievements.catalog(new Gson()).getTemplates())
		{
			assertTrue(template.getName(), template.getIconRequirement() != null || template.getIcon() != null);
			assertIcons(template.getName(), template.build(Map.of("level", 70)));
		}
		TemplateNode doom = build("doom-of-mokhaiotl");
		QuestChains.expand(doom, QuestRequirementsDb.load(new Gson()));
		assertIcons("Doom with chains", doom);
	}

	private static void assertIcons(String where, TemplateNode node)
	{
		assertTrue(where + ": " + node.getDisplayTitle(), node.getRequirement() != null
			|| (node.getIcon() != null && node.getIcon().isValid()));
		node.getChildren().forEach(child -> assertIcons(where, child));
	}

	@Test
	public void templatesHaveStableIdsAndWikiPages()
	{
		for (String id : List.of("bowfa-crystal", "dragon-warhammer", "doom-of-mokhaiotl", "the-hueycoatl", "fire-cape",
			"infernal-cape", "dragon-defender", "fighter-torso", "avas-assembler", "graceful", "rune-pouch",
			"imbued-god-cape", "zenyte-jewellery", "ardougne-cloak"))
		{
			TemplateNode root = build(id);
			assertNotNull(id, root.getWikiPage());
			assertEquals(id, template(id).getId());
		}
	}

	private static void assertUniques(TemplateNode uniques, HiscoreSkill boss, int[][] expected)
	{
		assertEquals(expected.length, uniques.getChildren().size());
		for (int i = 0; i < expected.length; i++)
		{
			TemplateNode node = uniques.getChildren().get(i);
			assertEquals(expected[i][0], ((ItemRequirement) node.getRequirement()).getItemId());
			assertEquals(DropSource.hiscore(boss, expected[i][1]), node.getDropSource());
		}
	}

	private static void assertItem(TemplateNode node, int itemId, long quantity)
	{
		ItemRequirement item = (ItemRequirement) node.getRequirement();
		assertEquals(itemId, item.getItemId());
		assertEquals(quantity, item.getQuantity());
	}

	private static QuestRequirement quest(Quest quest)
	{
		return new QuestRequirement(quest.getId(), quest.getName());
	}

	private static Goal findQuest(GoalTree tree, Quest quest)
	{
		for (Goal goal : tree.flatten())
		{
			if (goal.getRequirement() instanceof QuestRequirement
				&& ((QuestRequirement) goal.getRequirement()).getQuestId() == quest.getId())
			{
				return goal;
			}
		}
		throw new AssertionError(quest.getName() + " not in the tree");
	}

	private static void collectQuests(TemplateNode node, Set<Integer> out)
	{
		if (node.getRequirement() instanceof QuestRequirement)
		{
			out.add(((QuestRequirement) node.getRequirement()).getQuestId());
		}
		node.getChildren().forEach(child -> collectQuests(child, out));
	}

	private static List<String> search(String query)
	{
		List<String> found = new ArrayList<>();
		TemplateCategories.group(TEMPLATES, query, false).values().forEach(list -> found.addAll(names(list)));
		return found;
	}

	private static GoalTemplate template(String id)
	{
		for (GoalTemplate template : TEMPLATES)
		{
			if (template.getId().equals(id))
			{
				return template;
			}
		}
		throw new AssertionError("No template " + id);
	}

	private static TemplateNode build(String id)
	{
		return template(id).build(Map.of());
	}

	private static List<String> titles(TemplateNode root)
	{
		List<String> titles = new ArrayList<>();
		root.getChildren().forEach(child -> titles.add(child.getTitle()));
		return titles;
	}

	private static List<String> names(List<GoalTemplate> templates)
	{
		List<String> names = new ArrayList<>();
		templates.forEach(template -> names.add(template.getName()));
		return names;
	}
}
