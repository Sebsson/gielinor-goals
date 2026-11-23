package com.gielinorgoals.persistence;

import com.gielinorgoals.model.ActivityCounters;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.SkillRequirement;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Skill;
import net.runelite.client.hiscore.HiscoreSkill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Sparfiler från äldre versioner (fixtures/vN.json) uppgraderas till aktuell version utan
 * dataförlust: mål, datum, föräldrar, krav, räknare och template-fält. Varje fil sparas
 * sedan igen och läses in en gång till, så att uppgraderingen inte bara gäller i minnet.
 */
public class MigrationFixturesTest
{
	private final GoalSerializer serializer = new GoalSerializer(new Gson(), new SchemaMigrator());

	@Test
	public void version1()
	{
		GoalTree tree = load("v1");
		assertEquals(4, tree.size());
		assertEquals(List.of("cape", "fish"), tree.getRootIds());
		Goal cape = tree.find("cape");
		assertEquals("Quest cape", cape.getTitle());
		assertEquals("All the quests", cape.getNote());
		Goal rfd = tree.find("rfd");
		assertTrue(rfd.isCompleted());
		assertEquals(Long.valueOf(1_700_000_000_000L), rfd.getCompletedAt());
		assertFalse(rfd.isExpanded());
		assertEquals(List.of("cape"), parentIds(tree, "rfd"));
		assertEquals(List.of("rfd"), parentIds(tree, "cook"));
		assertEquals(Long.valueOf(1_690_000_000_000L), tree.find("cook").getCompletedAt());
		// Fält som inte fanns då hittas inte på.
		assertNull(cape.getCreatedAt());
		assertFalse(rfd.isCompletedAtApprox());
	}

	@Test
	public void version3()
	{
		GoalTree tree = load("v3");
		assertEquals(3, tree.size());
		assertEquals(new ItemRequirement(7462, "Barrows gloves", 1, false), tree.find("gloves").getRequirement());
		Goal cooking = tree.find("cooking");
		assertEquals(SkillRequirement.level(Skill.COOKING, 70), cooking.getRequirement());
		assertEquals(Long.valueOf(1_710_000_000_000L), cooking.getCompletedAt());
		assertEquals("From the Culinaromancer", tree.find("manual").getNote());
		assertNull(tree.find("manual").getRequirement());
		assertEquals(List.of("gloves"), parentIds(tree, "manual"));
	}

	@Test
	public void version6()
	{
		GoalTree tree = load("v6");
		Goal attack = tree.find("attack");
		assertEquals(Long.valueOf(1_720_000_000_000L), attack.getCreatedAt());
		assertEquals(Long.valueOf(1_721_000_000_000L), attack.getCompletedAt());
		assertTrue(attack.isCompletedAtApprox());
		SkillRequirement mining = (SkillRequirement) tree.find("mining").getRequirement();
		assertEquals(SkillRequirement.Mode.XP_GAIN, mining.getMode());
		assertEquals(100_000, mining.getTarget());
		assertFalse(tree.find("base").isExpanded());
		assertEquals(List.of("base"), parentIds(tree, "mining"));
	}

	@Test
	public void version9()
	{
		LoadResult result = loadResult("v9");
		GoalTree tree = result.getTree();
		assertEquals(List.of("gloves", "cape"), tree.getRootIds());
		// Delat delmål: en gång, under båda föräldrarna.
		assertEquals(4, tree.size());
		assertEquals(List.of("gloves", "cape"), parentIds(tree, "rfd"));
		assertEquals(new QuestRequirement(117, "Recipe for Disaster"), tree.find("rfd").getRequirement());
		assertEquals(Long.valueOf(1_731_000_000_000L), tree.find("rfd").getCompletedAt());
		assertTrue(tree.find("gloves").isAggregate());
		assertTrue(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 500).isSameGoal(tree.find("zulrah").getRequirement()));

		ActivityCounters counters = result.getCounters();
		assertEquals(321, counters.value("hiscore:ZULRAH"));
		assertEquals(ActivityCounters.Source.CHAT, counters.get("hiscore:ZULRAH").getSource());
		assertEquals(1_732_000_000_000L, counters.get("hiscore:ZULRAH").getUpdatedAt());
		assertEquals(45, counters.value("npc:lizardman shaman"));
	}

	@Test
	public void version10()
	{
		GoalTree tree = load("v10");
		Goal cape = tree.find("cape");
		assertEquals("quest-cape", cape.getTemplateId());
		assertEquals(Integer.valueOf(1), cape.getTemplateParams().get("questChains"));
		assertEquals(List.of("quest:17"), cape.getTemplateIgnoredKeys());
		assertEquals("group:quest cape", cape.getTemplateNodeKey());
		assertEquals("Quest point cape", cape.getWikiPage());
		assertEquals("quest:17", tree.find("cook").getTemplateNodeKey());
	}

	private GoalTree load(String name)
	{
		return loadResult(name).getTree();
	}

	// Läser fixturen, kontrollerar uppgraderingen, sparar och läser igen.
	private LoadResult loadResult(String name)
	{
		LoadResult result = serializer.deserialize(read(name));
		assertEquals(name, LoadResult.Status.OK, result.getStatus());
		assertTrue(name, result.isMigrated());

		String saved = serializer.serialize(result.getTree(), result.getCounters());
		assertEquals(SchemaMigrator.CURRENT_VERSION, new Gson().fromJson(saved, JsonObject.class).get("schemaVersion").getAsInt());
		LoadResult again = serializer.deserialize(saved);
		assertEquals(LoadResult.Status.OK, again.getStatus());
		assertFalse(again.isMigrated());
		assertEquals(result.getTree().size(), again.getTree().size());
		for (Goal goal : result.getTree().flatten())
		{
			Goal copy = again.getTree().find(goal.getId());
			assertNotNull(goal.getId(), copy);
			assertEquals(goal.getTitle(), copy.getTitle());
			assertEquals(goal.getCompletedAt(), copy.getCompletedAt());
			assertEquals(goal.getRequirement(), copy.getRequirement());
			assertEquals(goal.getChildIds(), copy.getChildIds());
		}
		assertEquals(result.getCounters().asMap().keySet(), again.getCounters().asMap().keySet());
		return again;
	}

	private static List<String> parentIds(GoalTree tree, String id)
	{
		List<String> ids = new ArrayList<>();
		tree.getParents(id).forEach(parent -> ids.add(parent.getId()));
		return ids;
	}

	private static String read(String name)
	{
		try (InputStream in = MigrationFixturesTest.class.getResourceAsStream("fixtures/" + name + ".json"))
		{
			assertNotNull("missing fixture " + name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new AssertionError(e);
		}
	}
}
