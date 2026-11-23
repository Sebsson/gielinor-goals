package com.gielinorgoals.template;

import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.DropSource;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.SkillRequirement;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.hiscore.HiscoreSkill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Siffrorna i templates 5-13 mot wikin (se _comment i varje fil) och item-id:n mot gameval.
 */
public class IronmanTemplatesPart3Test
{
	private static final List<GoalTemplate> TEMPLATES = JsonTemplates.load(new Gson());

	@Test
	public void fireCapeIsSharedWithTheInfernalCape()
	{
		List<TemplateNode> fire = obtain("fire-cape");
		assertKc(fire.get(0), HiscoreSkill.TZTOK_JAD, 1);
		assertItem(fire.get(1), ItemID.TZHAAR_CAPE_FIRE, 1);

		TemplateNode infernal = build("infernal-cape");
		assertItem(infernal.getChildren().get(0).getChildren().get(0), ItemID.TZHAAR_CAPE_FIRE, 1);
		List<TemplateNode> obtain = infernal.getChildren().get(1).getChildren();
		assertKc(obtain.get(0), HiscoreSkill.TZKAL_ZUK, 1);
		assertItem(obtain.get(1), ItemID.INFERNAL_CAPE, 1);

		// Fire cape-målet kopplas in i stället för att kopieras när båda läggs till.
		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, build("fire-cape"), null);
		TemplateInserter.insert(tree, build("infernal-cape"), null);
		Goal cape = findItem(tree, ItemID.TZHAAR_CAPE_FIRE);
		assertEquals(2, tree.getParents(cape.getId()).size());
	}

	@Test
	public void dragonDefenderInOrder()
	{
		assertNull(unlock("dragon-defender").get(0).getRequirement());
		List<TemplateNode> obtain = obtain("dragon-defender");
		int[] ids = {ItemID.BRONZE_PARRYINGDAGGER, ItemID.IRON_PARRYINGDAGGER, ItemID.STEEL_PARRYINGDAGGER,
			ItemID.BLACK_PARRYINGDAGGER, ItemID.MITHRIL_PARRYINGDAGGER, ItemID.ADAMANT_PARRYINGDAGGER,
			ItemID.RUNE_PARRYINGDAGGER, ItemID.DRAGON_PARRYINGDAGGER};
		assertEquals(ids.length, obtain.size());
		for (int i = 0; i < ids.length; i++)
		{
			assertItem(obtain.get(i), ids[i], 1);
		}
		assertEquals(DropSource.npc("Cyclops", 100), obtain.get(7).getDropSource());
	}

	@Test
	public void fighterTorso()
	{
		List<TemplateNode> obtain = obtain("fighter-torso");
		for (int i = 0; i < 4; i++)
		{
			KillCountRequirement points = (KillCountRequirement) obtain.get(i).getRequirement();
			assertEquals(KillCountRequirement.ActivityType.MANUAL, points.getActivityType());
			assertEquals(375, points.getTarget());
		}
		assertTrue(KillCountRequirement.manual("Penance Queen kills", 1).isSameGoal(obtain.get(4).getRequirement()));
		assertItem(obtain.get(5), ItemID.BARBASSAULT_PENANCE_FIGHTER_TORSO, 1);
	}

	@Test
	public void avasAssembler()
	{
		List<TemplateNode> unlock = unlock("avas-assembler");
		assertEquals(quest(Quest.ANIMAL_MAGNETISM), unlock.get(0).getRequirement());
		assertEquals(SkillRequirement.level(Skill.RANGED, 50), unlock.get(1).getRequirement());
		assertEquals(quest(Quest.DRAGON_SLAYER_II), unlock.get(2).getRequirement());

		List<TemplateNode> obtain = obtain("avas-assembler");
		assertItem(obtain.get(0), ItemID.ANMA_50_REWARD, 1);
		assertItem(obtain.get(1), ItemID.VORKATH_HEAD, 1);
		assertEquals(DropSource.hiscore(HiscoreSkill.VORKATH, 50), obtain.get(1).getDropSource());
		assertKc(obtain.get(2), HiscoreSkill.VORKATH, 50);
		assertItem(obtain.get(3), ItemID.MITHRIL_ARROW, 75);
		assertItem(obtain.get(4), ItemID.AVAS_ASSEMBLER, 1);
	}

	@Test
	public void gracefulCosts260Marks()
	{
		List<TemplateNode> obtain = obtain("graceful");
		assertItem(obtain.get(0), ItemID.GRACE, 260);
		int[] pieces = {ItemID.GRACEFUL_HOOD, ItemID.GRACEFUL_CAPE, ItemID.GRACEFUL_TOP, ItemID.GRACEFUL_LEGS,
			ItemID.GRACEFUL_GLOVES, ItemID.GRACEFUL_BOOTS};
		for (int i = 0; i < pieces.length; i++)
		{
			assertItem(obtain.get(i + 1), pieces[i], 1);
			assertTrue(((ItemRequirement) obtain.get(i + 1).getRequirement()).isIncludeVariants());
		}
	}

	@Test
	public void runePouch()
	{
		assertTrue(KillCountRequirement.manual("Slayer points", 750).isSameGoal(unlock("rune-pouch").get(0).getRequirement()));
		assertItem(obtain("rune-pouch").get(0), ItemID.BH_RUNE_POUCH, 1);
	}

	@Test
	public void imbuedGodCape()
	{
		assertEquals(quest(Quest.MAGE_ARENA_II), unlock("imbued-god-cape").get(0).getRequirement());
		List<TemplateNode> obtain = obtain("imbued-god-cape");
		assertItem(obtain.get(0), ItemID.SARADOMIN_CAPE, 1);
		assertItem(obtain.get(1), ItemID.MA2_SARADOMIN_CAPE, 1);
	}

	@Test
	public void zenyteJewellery()
	{
		List<TemplateNode> unlock = unlock("zenyte-jewellery");
		assertEquals(quest(Quest.MONKEY_MADNESS_II), unlock.get(0).getRequirement());
		assertEquals(SkillRequirement.level(Skill.CRAFTING, 89), unlock.get(1).getRequirement());
		assertEquals(SkillRequirement.level(Skill.CRAFTING, 98), unlock.get(2).getRequirement());
		assertEquals(SkillRequirement.level(Skill.MAGIC, 93), unlock.get(3).getRequirement());

		List<TemplateNode> obtain = obtain("zenyte-jewellery");
		assertItem(obtain.get(0), ItemID.ZENYTE_SHARD, 4);
		assertEquals(DropSource.npc("Demonic gorilla", 300), obtain.get(0).getDropSource());
		assertItem(obtain.get(1), ItemID.UNCUT_ONYX, 4);
		List<TemplateNode> jewellery = obtain.get(2).getChildren();
		assertItem(jewellery.get(0), ItemID.ZENYTE_RING_ENCHANTED, 1);
		assertItem(jewellery.get(1), ItemID.ZENYTE_NECKLACE_ENCHANTED, 1);
		assertItem(jewellery.get(2), ItemID.ZENYTE_BRACELET_ENCHANTED, 1);
		assertItem(jewellery.get(3), ItemID.ZENYTE_AMULET_ENCHANTED, 1);
	}

	@Test
	public void ardougneCloakAndFairyRings()
	{
		List<TemplateNode> fairyRings = unlock("ardougne-cloak").get(0).getChildren();
		assertEquals(quest(Quest.FAIRYTALE_I__GROWING_PAINS), fairyRings.get(0).getRequirement());
		// Bara en del av Fairytale II krävs, så det är ett manuellt mål och inte questet.
		assertNull(fairyRings.get(1).getRequirement());
		assertItem(fairyRings.get(2), ItemID.DRAMEN_STAFF, 1);

		List<TemplateNode> cloaks = obtain("ardougne-cloak");
		int[] ids = {ItemID.ARDY_CAPE_EASY, ItemID.ARDY_CAPE_MEDIUM, ItemID.ARDY_CAPE_HARD, ItemID.ARDY_CAPE_ELITE};
		DiaryTier[] tiers = {DiaryTier.EASY, DiaryTier.MEDIUM, DiaryTier.HARD, DiaryTier.ELITE};
		for (int i = 0; i < ids.length; i++)
		{
			assertItem(cloaks.get(i), ids[i], 1);
			assertEquals(new DiaryRequirement(DiaryArea.ARDOUGNE, tiers[i]),
				cloaks.get(i).getChildren().get(0).getRequirement());
		}
	}

	// doneWhen-nycklar som pekar utanför sin egen template: målet kan finnas från en annan
	// template (infernal cape och Jad-KC, Fairytale II under Quest cape, elite Lumbridge-diaryn).
	private static final java.util.Set<String> EXTERNAL_KEYS = java.util.Set.of(
		"item:21295", "kc:hiscore:TZKAL_ZUK:1", "kc:hiscore:TZTOK_JAD:1", "quest:47", "diary:LUMBRIDGE:ELITE");

	@Test
	public void everyDoneWhenKeyPointsAtARealGoal()
	{
		for (GoalTemplate template : TEMPLATES)
		{
			TemplateNode root = template.build(Map.of());
			java.util.Set<String> keys = new java.util.HashSet<>();
			collectKeys(root, keys);
			for (String key : doneWhenKeys(root))
			{
				assertTrue(template.getId() + ": " + key, keys.contains(key) || EXTERNAL_KEYS.contains(key));
			}
		}
	}

	@Test
	public void ownedDragonDefenderCompletesTheWholeTemplate()
	{
		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, build("dragon-defender"), null);
		com.gielinorgoals.model.GameData data = new com.gielinorgoals.model.GameData()
		{
			@Override
			public long getXp(Skill skill)
			{
				return 0;
			}

			@Override
			public long getItemCount(int itemId)
			{
				return itemId == ItemID.DRAGON_PARRYINGDAGGER ? 1 : 0;
			}
		};

		com.gielinorgoals.model.GoalEvaluator.evaluate(tree, data, 1L, TemplateKeys::of);

		for (Goal goal : tree.flatten())
		{
			assertTrue(goal.getDisplayTitle(), goal.isCompleted());
		}
	}

	private static void collectKeys(TemplateNode node, java.util.Set<String> out)
	{
		out.add(node.getKey());
		node.getChildren().forEach(child -> collectKeys(child, out));
	}

	private static List<String> doneWhenKeys(TemplateNode node)
	{
		List<String> keys = new ArrayList<>();
		if (node.getDoneWhen() != null)
		{
			keys.addAll(node.getDoneWhen());
		}
		node.getChildren().forEach(child -> keys.addAll(doneWhenKeys(child)));
		return keys;
	}

	@Test
	public void newQuestsExpandTheirChains()
	{
		QuestRequirementsDb db = QuestRequirementsDb.load(new Gson());
		TemplateNode cloak = build("ardougne-cloak");
		QuestChains.expand(cloak, db);
		List<Integer> quests = new ArrayList<>();
		collectQuests(cloak, quests);
		for (Quest quest : new Quest[]{Quest.FAIRYTALE_I__GROWING_PAINS, Quest.LOST_CITY, Quest.NATURE_SPIRIT,
			Quest.PRIEST_IN_PERIL, Quest.THE_RESTLESS_GHOST})
		{
			assertTrue(quest.getName(), quests.contains(quest.getId()));
		}
		// Hela Fairytale II krävs inte för fairy rings.
		assertTrue(!quests.contains(Quest.FAIRYTALE_II__CURE_A_QUEEN.getId()));

		TemplateNode cape = build("imbued-god-cape");
		QuestChains.expand(cape, db);
		TemplateNode ma2 = cape.getChildren().get(0).getChildren().get(0);
		List<Integer> chain = new ArrayList<>();
		collectQuests(ma2, chain);
		assertTrue(chain.contains(Quest.MAGE_ARENA_I.getId()));
		assertTrue(hasSkill(ma2, Skill.MAGIC, 75));
	}

	private static boolean hasSkill(TemplateNode node, Skill skill, int level)
	{
		if (SkillRequirement.level(skill, level).equals(node.getRequirement()))
		{
			return true;
		}
		for (TemplateNode child : node.getChildren())
		{
			if (hasSkill(child, skill, level))
			{
				return true;
			}
		}
		return false;
	}

	private static void collectQuests(TemplateNode node, List<Integer> out)
	{
		if (node.getRequirement() instanceof QuestRequirement)
		{
			out.add(((QuestRequirement) node.getRequirement()).getQuestId());
		}
		node.getChildren().forEach(child -> collectQuests(child, out));
	}

	private static Goal findItem(GoalTree tree, int itemId)
	{
		for (Goal goal : tree.flatten())
		{
			if (goal.getRequirement() instanceof ItemRequirement
				&& ((ItemRequirement) goal.getRequirement()).getItemId() == itemId)
			{
				return goal;
			}
		}
		throw new AssertionError("No goal for item " + itemId);
	}

	private static void assertKc(TemplateNode node, HiscoreSkill boss, int count)
	{
		assertTrue(KillCountRequirement.hiscore(boss, count).isSameGoal(node.getRequirement()));
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

	private static List<TemplateNode> unlock(String id)
	{
		TemplateNode node = build(id).getChildren().get(0);
		assertEquals("Unlock", node.getTitle());
		return node.getChildren();
	}

	private static List<TemplateNode> obtain(String id)
	{
		TemplateNode node = build(id).getChildren().get(1);
		assertEquals("Obtain", node.getTitle());
		return node.getChildren();
	}

	private static TemplateNode build(String id)
	{
		for (GoalTemplate template : TEMPLATES)
		{
			if (template.getId().equals(id))
			{
				return template.build(Map.of());
			}
		}
		throw new AssertionError("No template " + id);
	}
}
