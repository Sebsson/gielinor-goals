package com.gielinorgoals.persistence;

import net.runelite.client.hiscore.HiscoreSkill;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.ActivityCounters;
import com.gielinorgoals.model.TotalLevelRequirement;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.CombatLevelRequirement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.CombatAchievementRequirement;
import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.SkillRequirement;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;
import org.junit.Test;

public class GoalSerializerTest
{
	private final GoalSerializer serializer = new GoalSerializer(new Gson(), new SchemaMigrator());

	@Test
	public void roundTripKeepsTreeAndFields()
	{
		Goal parent = new Goal("Get Fire cape");
		parent.setNote("Bring stamina potions");
		parent.markCompleted(true, 42L);
		parent.setExpanded(false);
		Goal child = new Goal("Ranged level 75");
		GoalTree tree = treeOf(parent);
		tree.add(parent.getId(), child);
		parent.setExpanded(false);

		String json = serializer.serialize(tree);
		LoadResult result = serializer.deserialize(json);

		assertEquals(LoadResult.Status.OK, result.getStatus());
		assertFalse(result.isMigrated());
		assertEquals(1, result.getGoals().size());

		Goal loaded = result.getGoals().get(0);
		assertEquals(parent.getId(), loaded.getId());
		assertEquals("Get Fire cape", loaded.getTitle());
		assertEquals("Bring stamina potions", loaded.getNote());
		assertTrue(loaded.isCompleted());
		assertEquals(Long.valueOf(42L), loaded.getCompletedAt());
		assertFalse(loaded.isExpanded());
		assertEquals(List.of(child.getId()), loaded.getChildIds());
		assertEquals("Ranged level 75", result.getTree().find(child.getId()).getTitle());
	}

	private static GoalTree treeOf(Goal... roots)
	{
		GoalTree tree = new GoalTree();
		for (Goal root : roots)
		{
			tree.add(null, root);
		}
		return tree;
	}

	@Test
	public void serializedJsonContainsSchemaVersion()
	{
		JsonObject root = new Gson().fromJson(serializer.serialize(new GoalTree()), JsonObject.class);
		assertEquals(SchemaMigrator.CURRENT_VERSION, root.get("schemaVersion").getAsInt());
	}

	@Test
	public void missingDataGivesEmptyList()
	{
		assertTrue(serializer.deserialize(null).getGoals().isEmpty());
		assertEquals(LoadResult.Status.OK, serializer.deserialize("  ").getStatus());
	}

	@Test
	public void invalidJsonIsReportedAsCorrupt()
	{
		assertEquals(LoadResult.Status.CORRUPT, serializer.deserialize("{not json").getStatus());
		assertEquals(LoadResult.Status.CORRUPT, serializer.deserialize("[1,2,3]").getStatus());
		assertEquals(LoadResult.Status.CORRUPT, serializer.deserialize("{\"schemaVersion\":1,\"goals\":5}").getStatus());
		assertEquals(LoadResult.Status.CORRUPT, serializer.deserialize("{\"schemaVersion\":\"x\"}").getStatus());
	}

	@Test
	public void newerVersionIsReadOnly()
	{
		String json = "{\"schemaVersion\":99,\"goals\":[{\"id\":\"a\",\"title\":\"Future\"}]}";
		LoadResult result = serializer.deserialize(json);

		assertEquals(LoadResult.Status.NEWER_VERSION, result.getStatus());
		assertTrue(result.isReadOnly());
		assertEquals("Future", result.getGoals().get(0).getTitle());
	}

	@Test
	public void skillRequirementRoundTrips()
	{
		Goal level = new Goal("");
		level.setRequirement(SkillRequirement.level(Skill.AGILITY, 70));
		Goal xp = new Goal("Max fishing xp");
		xp.setRequirement(SkillRequirement.xp(Skill.FISHING, 200_000_000L));

		List<Goal> loaded = serializer.deserialize(serializer.serialize(treeOf(level, xp))).getGoals();

		assertEquals(SkillRequirement.level(Skill.AGILITY, 70), loaded.get(0).getRequirement());
		assertEquals("70 Agility", loaded.get(0).getDisplayTitle());
		assertEquals(SkillRequirement.xp(Skill.FISHING, 200_000_000L), loaded.get(1).getRequirement());
		assertEquals("Max fishing xp", loaded.get(1).getDisplayTitle());
	}

	@Test
	public void xpGainRequirementKeepsStartXp()
	{
		Goal goal = new Goal("");
		goal.setRequirement(SkillRequirement.xpGain(Skill.MINING, 1_234_567L, 500_000L));

		List<Goal> loaded = serializer.deserialize(serializer.serialize(treeOf(goal))).getGoals();

		SkillRequirement requirement = (SkillRequirement) loaded.get(0).getRequirement();
		assertEquals(SkillRequirement.Mode.XP_GAIN, requirement.getMode());
		assertEquals(1_234_567L, requirement.getStartXp());
		assertEquals(500_000L, requirement.getTarget());
		assertEquals("Gain 500,000 Mining XP", loaded.get(0).getDisplayTitle());
	}

	@Test
	public void version5LoadsIntoVersion6WithoutDataLoss()
	{
		String json = "{\"schemaVersion\":5,\"goals\":[{\"id\":\"a\",\"title\":\"Barrows gloves\",\"note\":\"RFD\","
			+ "\"completed\":true,\"completedAt\":1700000000000,\"expanded\":false,\"children\":["
			+ "{\"id\":\"b\",\"title\":\"\",\"completed\":false,\"expanded\":true,\"children\":[],"
			+ "\"requirement\":{\"type\":\"SKILL\",\"skill\":\"COOKING\",\"mode\":\"LEVEL\",\"target\":70}}]}]}";

		LoadResult result = serializer.deserialize(json);

		assertEquals(LoadResult.Status.OK, result.getStatus());
		assertTrue(result.isMigrated());
		Goal parent = result.getGoals().get(0);
		assertEquals("Barrows gloves", parent.getTitle());
		assertEquals("RFD", parent.getNote());
		assertTrue(parent.isCompleted());
		assertEquals(Long.valueOf(1_700_000_000_000L), parent.getCompletedAt());
		assertFalse(parent.isExpanded());
		// Nya fält: inget datum hittas på för gamla mål.
		assertNull(parent.getCreatedAt());
		assertFalse(parent.isCompletedAtApprox());
		Goal child = result.getTree().getChildren(parent).get(0);
		assertEquals(SkillRequirement.level(Skill.COOKING, 70), child.getRequirement());
		assertNull(child.getCreatedAt());

		JsonObject saved = new Gson().fromJson(serializer.serialize(result.getTree()), JsonObject.class);
		assertEquals(SchemaMigrator.CURRENT_VERSION, saved.get("schemaVersion").getAsInt());
	}

	@Test
	public void xpGainPastMaxXpIsDroppedAsInvalid()
	{
		String json = "{\"schemaVersion\":5,\"goals\":[{\"id\":\"a\",\"title\":\"x\",\"requirement\":"
			+ "{\"type\":\"SKILL\",\"skill\":\"MINING\",\"mode\":\"XP_GAIN\",\"target\":2,\"startXp\":200000000}}]}";

		assertEquals(null, serializer.deserialize(json).getGoals().get(0).getRequirement());
	}

	@Test
	public void itemRequirementRoundTrips()
	{
		Goal goal = new Goal("");
		ItemRequirement requirement = new ItemRequirement(ItemID.TZHAAR_CAPE_FIRE, "Fire cape", 1, true);
		goal.setRequirement(requirement);

		String json = serializer.serialize(treeOf(goal));
		Goal loaded = serializer.deserialize(json).getGoals().get(0);

		assertTrue(json.contains("\"type\":\"ITEM\""));
		assertEquals(requirement, loaded.getRequirement());
		assertEquals("Fire cape", loaded.getDisplayTitle());
	}

	@Test
	public void achievementRequirementsRoundTrip()
	{
		List<Requirement> requirements = List.of(
			new QuestRequirement(42, "Dragon Slayer I"),
			new DiaryRequirement(DiaryArea.KARAMJA, DiaryTier.ELITE),
			new CombatAchievementRequirement(17, "Noxious Foe", "Easy"));
		List<Goal> goals = new java.util.ArrayList<>();
		for (Requirement requirement : requirements)
		{
			Goal goal = new Goal("");
			goal.setRequirement(requirement);
			goals.add(goal);
		}

		List<Goal> loaded = serializer.deserialize(serializer.serialize(treeOf(goals.toArray(new Goal[0])))).getGoals();

		for (int i = 0; i < requirements.size(); i++)
		{
			assertEquals(requirements.get(i), loaded.get(i).getRequirement());
		}
	}

	@Test
	public void serializedRequirementHasTypeField()
	{
		Goal goal = new Goal("x");
		goal.setRequirement(SkillRequirement.level(Skill.MINING, 60));
		String json = serializer.serialize(treeOf(goal));

		assertTrue(json.contains("\"type\":\"SKILL\""));
		assertTrue(json.contains("\"skill\":\"MINING\""));
	}

	@Test
	public void unknownOrInvalidRequirementBecomesManualGoal()
	{
		String json = "{\"schemaVersion\":2,\"goals\":["
			+ "{\"id\":\"a\",\"title\":\"A\",\"requirement\":{\"type\":\"TELEPORT\"}},"
			+ "{\"id\":\"b\",\"title\":\"B\",\"requirement\":{\"type\":\"SKILL\",\"skill\":\"NOPE\",\"mode\":\"LEVEL\",\"target\":50}},"
			+ "{\"id\":\"c\",\"title\":\"C\",\"requirement\":{\"type\":\"SKILL\",\"skill\":\"MINING\",\"mode\":\"LEVEL\",\"target\":500}}"
			+ "]}";
		LoadResult result = serializer.deserialize(json);

		assertEquals(LoadResult.Status.OK, result.getStatus());
		assertEquals(3, result.getGoals().size());
		result.getGoals().forEach(goal -> assertNull(goal.getRequirement()));
	}

	@Test
	public void version1DataIsMigrated()
	{
		LoadResult result = serializer.deserialize("{\"schemaVersion\":1,\"goals\":[{\"id\":\"a\",\"title\":\"Old\"}]}");

		assertEquals(LoadResult.Status.OK, result.getStatus());
		assertTrue(result.isMigrated());
		assertEquals("Old", result.getGoals().get(0).getTitle());
	}

	@Test
	public void missingFieldsAreFilledIn()
	{
		String json = "{\"schemaVersion\":1,\"goals\":[{\"title\":\"No id\"},null,{\"id\":\"b\"}]}";
		List<Goal> goals = serializer.deserialize(json).getGoals();

		assertEquals(2, goals.size());
		assertNotNull(goals.get(0).getId());
		assertNotNull(goals.get(0).getChildIds());
		assertEquals("", goals.get(1).getTitle());
		assertNull(goals.get(1).getCompletedAt());
	}

	@Test
	public void realisticVersion6FileIsFlattenedWithoutDataLoss() throws Exception
	{
		LoadResult result = serializer.deserialize(resource("goals-v6.json"));

		assertEquals(LoadResult.Status.OK, result.getStatus());
		assertTrue(result.isMigrated());
		assertRealisticGoals(result.getTree());

		// Sparat i nya formatet: platt lista, rootIds, inga nästlade children.
		JsonObject saved = new Gson().fromJson(serializer.serialize(result.getTree()), JsonObject.class);
		assertEquals(SchemaMigrator.CURRENT_VERSION, saved.get("schemaVersion").getAsInt());
		assertEquals("[\"gloves\",\"firecape\",\"karamja\",\"noxious\",\"legacy\"]", saved.get("rootIds").toString());
		assertEquals(11, saved.getAsJsonArray("goals").size());
		for (com.google.gson.JsonElement goal : saved.getAsJsonArray("goals"))
		{
			assertFalse(goal.getAsJsonObject().has("children"));
			assertTrue(goal.getAsJsonObject().has("childIds"));
		}

		// Och läses tillbaka likadant utan ny migrering.
		LoadResult reloaded = serializer.deserialize(serializer.serialize(result.getTree()));
		assertFalse(reloaded.isMigrated());
		assertRealisticGoals(reloaded.getTree());
	}

	private static void assertRealisticGoals(GoalTree tree)
	{
		assertEquals(List.of("gloves", "firecape", "karamja", "noxious", "legacy"), tree.getRootIds());
		assertEquals(11, tree.size());

		Goal gloves = tree.find("gloves");
		assertEquals("Barrows gloves", gloves.getTitle());
		assertEquals("Cooking and RFD subquests first", gloves.getNote());
		assertEquals(Long.valueOf(1_762_000_000_000L), gloves.getCreatedAt());
		assertEquals(List.of("rfd", "qp"), gloves.getChildIds());

		Goal rfd = tree.find("rfd");
		assertEquals(new QuestRequirement(159, "Recipe for Disaster"), rfd.getRequirement());
		assertFalse(rfd.isExpanded());
		assertEquals(List.of("cooking", "dt"), rfd.getChildIds());
		assertEquals(List.of(gloves), tree.getParents("rfd"));

		Goal cooking = tree.find("cooking");
		assertEquals(SkillRequirement.level(Skill.COOKING, 70), cooking.getRequirement());
		assertTrue(cooking.isCompleted());
		assertEquals(Long.valueOf(1_763_000_000_000L), cooking.getCompletedAt());
		assertTrue(cooking.isCompletedAtApprox());

		Goal dt = tree.find("dt");
		assertEquals("Desert Treasure I", dt.getTitle());
		assertTrue(dt.isCompleted());
		assertFalse(dt.isCompletedAtApprox());

		Goal qp = tree.find("qp");
		assertNull(qp.getRequirement());
		assertNull(qp.getCreatedAt());

		Goal fireCape = tree.find("firecape");
		assertEquals(new ItemRequirement(ItemID.TZHAAR_CAPE_FIRE, "Fire cape", 1, true), fireCape.getRequirement());
		assertEquals("Bring stamina potions", fireCape.getNote());
		assertFalse(fireCape.isExpanded());
		assertEquals(List.of("ranged"), fireCape.getChildIds());
		assertEquals(SkillRequirement.xpGain(Skill.RANGED, 1_234_567L, 500_000L), tree.find("ranged").getRequirement());

		Goal karamja = tree.find("karamja");
		assertEquals(new DiaryRequirement(DiaryArea.KARAMJA, DiaryTier.ELITE), karamja.getRequirement());
		Goal task = tree.find("karamja-task");
		assertTrue(task.isCompleted());
		assertTrue(task.isCompletedAtApprox());

		Goal noxious = tree.find("noxious");
		assertEquals(new CombatAchievementRequirement(17, "Noxious Foe", "Easy"), noxious.getRequirement());
		assertTrue(noxious.isCompleted());
		// Saknade "children" i v6 blir en tom lista.
		assertTrue(noxious.getChildIds().isEmpty());

		Goal legacy = tree.find("legacy");
		assertTrue(legacy.isCompleted());
		assertEquals(Long.valueOf(1_700_000_000_000L), legacy.getCompletedAt());
		assertNull(legacy.getCreatedAt());
		assertFalse(legacy.isCompletedAtApprox());

		// Visningsordningen är densamma som i det gamla trädet.
		List<String> order = new java.util.ArrayList<>();
		tree.forEach(goal -> order.add(goal.getId()));
		assertEquals(List.of("gloves", "rfd", "cooking", "dt", "qp", "firecape", "ranged", "karamja", "karamja-task",
			"noxious", "legacy"), order);
	}

	private static String resource(String name) throws java.io.IOException
	{
		try (java.io.InputStream in = GoalSerializerTest.class.getResourceAsStream(name))
		{
			return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
	}

	@Test
	public void sharedGoalIsStoredOnceAndKeepsBothParents()
	{
		Goal gloves = new Goal("Barrows gloves");
		Goal questCape = new Goal("Quest cape");
		Goal rfd = new Goal("Recipe for Disaster");
		GoalTree tree = treeOf(gloves, questCape);
		tree.add(gloves.getId(), rfd);
		tree.link(questCape.getId(), rfd.getId());

		String json = serializer.serialize(tree);
		GoalTree loaded = serializer.deserialize(json).getTree();

		assertEquals(3, new Gson().fromJson(json, JsonObject.class).getAsJsonArray("goals").size());
		assertEquals(2, loaded.getParents(rfd.getId()).size());
		assertEquals(List.of(gloves.getId(), questCape.getId()), loaded.getRootIds());
	}

	@Test
	public void accountLevelRequirementsRoundTrip()
	{
		List<Requirement> requirements = List.of(
			new TotalLevelRequirement(1500),
			new CombatLevelRequirement(100),
			new QuestPointsRequirement(150));
		GoalTree tree = new GoalTree();
		for (Requirement requirement : requirements)
		{
			Goal goal = new Goal("");
			goal.setRequirement(requirement);
			tree.add(null, goal);
		}

		String json = serializer.serialize(tree);
		List<Goal> loaded = serializer.deserialize(json).getGoals();

		assertTrue(json.contains("\"type\":\"TOTAL_LEVEL\""));
		assertTrue(json.contains("\"type\":\"COMBAT_LEVEL\""));
		assertTrue(json.contains("\"type\":\"QUEST_POINTS\""));
		for (int i = 0; i < requirements.size(); i++)
		{
			assertEquals(requirements.get(i), loaded.get(i).getRequirement());
		}
		assertEquals("Total level 1500", loaded.get(0).getDisplayTitle());
	}

	@Test
	public void accountLevelRequirementsAreReadFromJson()
	{
		String json = "{\"schemaVersion\":" + SchemaMigrator.CURRENT_VERSION + ",\"rootIds\":[\"a\",\"b\",\"c\",\"d\"],\"goals\":["
			+ "{\"id\":\"a\",\"title\":\"\",\"requirement\":{\"type\":\"TOTAL_LEVEL\",\"target\":2000}},"
			+ "{\"id\":\"b\",\"title\":\"\",\"requirement\":{\"type\":\"COMBAT_LEVEL\",\"target\":126}},"
			+ "{\"id\":\"c\",\"title\":\"\",\"requirement\":{\"type\":\"QUEST_POINTS\",\"target\":1}},"
			// Utanför gränserna: blir ett manuellt mål i stället för att försvinna.
			+ "{\"id\":\"d\",\"title\":\"Bad\",\"requirement\":{\"type\":\"COMBAT_LEVEL\",\"target\":200}}]}";

		LoadResult result = serializer.deserialize(json);

		assertEquals(LoadResult.Status.OK, result.getStatus());
		assertFalse(result.isMigrated());
		assertEquals(new TotalLevelRequirement(2000), result.getTree().find("a").getRequirement());
		assertEquals(new CombatLevelRequirement(126), result.getTree().find("b").getRequirement());
		assertEquals(new QuestPointsRequirement(1), result.getTree().find("c").getRequirement());
		assertNull(result.getTree().find("d").getRequirement());
	}

	@Test
	public void version7LoadsIntoVersion8WithoutDataLoss()
	{
		String json = "{\"schemaVersion\":7,\"rootIds\":[\"cape\"],\"goals\":["
			+ "{\"id\":\"cape\",\"title\":\"Quest cape\",\"note\":\"All quests\",\"aggregate\":true,"
			+ "\"expanded\":false,\"createdAt\":1762000000000,\"childIds\":[\"rfd\"]},"
			+ "{\"id\":\"rfd\",\"title\":\"\",\"completed\":true,\"completedAt\":1763000000000,"
			+ "\"completedAtApprox\":true,\"childIds\":[],"
			+ "\"requirement\":{\"type\":\"QUEST\",\"questId\":159,\"questName\":\"Recipe for Disaster\"}}]}";

		LoadResult result = serializer.deserialize(json);

		assertEquals(LoadResult.Status.OK, result.getStatus());
		assertTrue(result.isMigrated());
		Goal cape = result.getTree().find("cape");
		assertEquals("Quest cape", cape.getTitle());
		assertEquals("All quests", cape.getNote());
		assertTrue(cape.isAggregate());
		assertFalse(cape.isExpanded());
		assertEquals(Long.valueOf(1_762_000_000_000L), cape.getCreatedAt());
		assertEquals(List.of("rfd"), cape.getChildIds());
		Goal rfd = result.getTree().find("rfd");
		assertTrue(rfd.isCompleted());
		assertTrue(rfd.isCompletedAtApprox());
		assertEquals(new QuestRequirement(159, "Recipe for Disaster"), rfd.getRequirement());

		JsonObject saved = new Gson().fromJson(serializer.serialize(result.getTree()), JsonObject.class);
		assertEquals(SchemaMigrator.CURRENT_VERSION, saved.get("schemaVersion").getAsInt());
	}

	@Test
	public void countersAndKillCountGoalsRoundTrip()
	{
		Goal goal = new Goal("");
		goal.setRequirement(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 150));
		ActivityCounters counters = new ActivityCounters();
		counters.applyChat("hiscore:ZULRAH", 120, 1_763_000_000_000L);
		counters.setManual("manual:birdhouse runs", 4, 1_763_000_000_001L);

		String json = serializer.serialize(treeOf(goal), counters);
		LoadResult loaded = serializer.deserialize(json);

		assertTrue(json.contains("\"type\":\"KILL_COUNT\""));
		assertEquals(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 150), loaded.getGoals().get(0).getRequirement());
		ActivityCounters.Counter zulrah = loaded.getCounters().get("hiscore:ZULRAH");
		assertEquals(120, zulrah.getValue());
		assertEquals(ActivityCounters.Source.CHAT, zulrah.getSource());
		assertEquals(1_763_000_000_000L, zulrah.getUpdatedAt());
		assertEquals(4, loaded.getCounters().value("manual:birdhouse runs"));
	}

	@Test
	public void version8LoadsIntoVersion9WithEmptyCounters()
	{
		String json = "{\"schemaVersion\":8,\"rootIds\":[\"a\"],\"goals\":["
			+ "{\"id\":\"a\",\"title\":\"\",\"completed\":true,\"completedAt\":1763000000000,\"childIds\":[],"
			+ "\"requirement\":{\"type\":\"QUEST_POINTS\",\"target\":150}}]}";

		LoadResult result = serializer.deserialize(json);

		assertEquals(LoadResult.Status.OK, result.getStatus());
		assertTrue(result.isMigrated());
		assertEquals(new QuestPointsRequirement(150), result.getTree().find("a").getRequirement());
		assertTrue(result.getTree().find("a").isCompleted());
		assertTrue(result.getCounters().asMap().isEmpty());
		JsonObject saved = new Gson().fromJson(serializer.serialize(result.getTree(), result.getCounters()),
			JsonObject.class);
		assertEquals(SchemaMigrator.CURRENT_VERSION, saved.get("schemaVersion").getAsInt());
		// Utan räknare skrivs ingen tom sektion.
		assertFalse(saved.has("counters"));
	}

	@Test
	public void killCountWithUnknownBossBecomesManualGoal()
	{
		String json = "{\"schemaVersion\":9,\"rootIds\":[\"a\"],\"goals\":[{\"id\":\"a\",\"title\":\"Old boss\","
			+ "\"childIds\":[],\"requirement\":{\"type\":\"KILL_COUNT\",\"activityType\":\"HISCORE\","
			+ "\"activityId\":\"NOT_A_BOSS\",\"activityName\":\"Gone\",\"target\":5}}]}";

		assertNull(serializer.deserialize(json).getTree().find("a").getRequirement());
	}

	@Test
	public void version9LoadsIntoVersion10AndTemplateFieldsRoundTrip()
	{
		String json = "{\"schemaVersion\":9,\"rootIds\":[\"a\"],\"goals\":["
			+ "{\"id\":\"a\",\"title\":\"Quest cape\",\"aggregate\":true,\"childIds\":[]}]}";

		LoadResult result = serializer.deserialize(json);
		assertTrue(result.isMigrated());
		Goal goal = result.getTree().find("a");
		assertEquals("Quest cape", goal.getTitle());
		assertTrue(goal.isAggregate());
		assertNull(goal.getTemplateId());
		assertNull(goal.getTemplateNodeKey());

		goal.setTemplateId("quest-cape");
		goal.setTemplateParams(java.util.Map.of("questChains", 0));
		goal.setTemplateIgnoredKeys(List.of("quest:17"));
		goal.setTemplateNodeKey("group:quest cape");
		LoadResult reloaded = serializer.deserialize(serializer.serialize(result.getTree()));

		Goal again = reloaded.getTree().find("a");
		assertFalse(reloaded.isMigrated());
		assertEquals("quest-cape", again.getTemplateId());
		assertEquals(Integer.valueOf(0), again.getTemplateParams().get("questChains"));
		assertEquals(List.of("quest:17"), again.getTemplateIgnoredKeys());
		assertEquals("group:quest cape", again.getTemplateNodeKey());
	}

	@Test
	public void version10LoadsIntoVersion11AndDropSourceRoundTrips()
	{
		String json = "{\"schemaVersion\":10,\"rootIds\":[\"a\"],\"goals\":["
			+ "{\"id\":\"a\",\"title\":\"\",\"childIds\":[],\"requirement\":{\"type\":\"ITEM\","
			+ "\"itemId\":13576,\"itemName\":\"Dragon warhammer\",\"quantity\":1,\"includeVariants\":false}}]}";

		LoadResult result = serializer.deserialize(json);
		assertTrue(result.isMigrated());
		Goal goal = result.getTree().find("a");
		assertEquals("Dragon warhammer", goal.getDisplayTitle());
		assertNull(goal.getDropSource());
		assertNull(goal.getObtainedAtKc());

		goal.setDropSource(com.gielinorgoals.model.DropSource.npc("Lizardman shaman", 5000));
		goal.markCompleted(true, 5L);
		goal.setObtainedAtKc(3210L);
		String saved = serializer.serialize(result.getTree());
		LoadResult reloaded = serializer.deserialize(saved);

		Goal again = reloaded.getTree().find("a");
		assertFalse(reloaded.isMigrated());
		assertEquals(com.gielinorgoals.model.DropSource.npc("Lizardman shaman", 5000), again.getDropSource());
		assertEquals(Long.valueOf(3210L), again.getObtainedAtKc());
		assertEquals(11, new Gson().fromJson(saved, JsonObject.class).get("schemaVersion").getAsInt());

		again.setIcon(com.gielinorgoals.model.GoalIcon.activity(HiscoreSkill.THE_HUEYCOATL));
		Goal third = serializer.deserialize(serializer.serialize(reloaded.getTree())).getTree().find("a");
		assertEquals(com.gielinorgoals.model.GoalIcon.activity(HiscoreSkill.THE_HUEYCOATL), third.getIcon());
	}
}
