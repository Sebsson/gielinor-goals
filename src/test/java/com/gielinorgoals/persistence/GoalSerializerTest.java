package com.gielinorgoals.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.gielinorgoals.model.Goal;
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
		parent.getChildren().add(child);

		String json = serializer.serialize(List.of(parent));
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
		assertEquals(child.getId(), loaded.getChildren().get(0).getId());
	}

	@Test
	public void serializedJsonContainsSchemaVersion()
	{
		JsonObject root = new Gson().fromJson(serializer.serialize(List.of()), JsonObject.class);
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

		List<Goal> loaded = serializer.deserialize(serializer.serialize(List.of(level, xp))).getGoals();

		assertEquals(SkillRequirement.level(Skill.AGILITY, 70), loaded.get(0).getRequirement());
		assertEquals("70 Agility", loaded.get(0).getDisplayTitle());
		assertEquals(SkillRequirement.xp(Skill.FISHING, 200_000_000L), loaded.get(1).getRequirement());
		assertEquals("Max fishing xp", loaded.get(1).getDisplayTitle());
	}

	@Test
	public void itemRequirementRoundTrips()
	{
		Goal goal = new Goal("");
		ItemRequirement requirement = new ItemRequirement(ItemID.TZHAAR_CAPE_FIRE, "Fire cape", 1, true);
		goal.setRequirement(requirement);

		String json = serializer.serialize(List.of(goal));
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

		List<Goal> loaded = serializer.deserialize(serializer.serialize(goals)).getGoals();

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
		String json = serializer.serialize(List.of(goal));

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
		assertNotNull(goals.get(0).getChildren());
		assertEquals("", goals.get(1).getTitle());
		assertNull(goals.get(1).getCompletedAt());
	}
}
