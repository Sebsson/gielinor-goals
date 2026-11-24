package com.gielinorgoals.model;

import com.gielinorgoals.persistence.GoalSerializer;
import com.gielinorgoals.persistence.LoadResult;
import com.gielinorgoals.persistence.SchemaMigrator;
import com.gielinorgoals.template.TemplateKeys;
import com.gielinorgoals.tracking.WatchedAchievements;
import com.google.gson.Gson;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * POINTS-mål: läsning från varbits och varps, okänt värde, sparformat och bevakning.
 */
public class PointsRequirementTest
{
	@Test
	public void slayerPointsAreReadFromTheirVarbit()
	{
		PointsRequirement requirement = new PointsRequirement(PointsActivity.SLAYER, 750);
		GoalProgress progress = requirement.progress(data(Map.of(VarbitID.SLAYER_POINTS, 320), Map.of()));
		assertEquals(320, progress.getCurrent());
		assertEquals(750, progress.getTarget());
		assertFalse(progress.isComplete());
		assertEquals("320/750", progress.getLabel());
		assertEquals("Slayer points 320 / 750\n430 points left", requirement.describeProgress(progress));

		assertTrue(requirement.progress(data(Map.of(VarbitID.SLAYER_POINTS, 750), Map.of())).isComplete());
		assertEquals("750 Slayer points", requirement.describe());
	}

	@Test
	public void varpActivitiesAreReadFromTheirVarp()
	{
		PointsRequirement reputation = new PointsRequirement(PointsActivity.GIANTS_FOUNDRY, 2000);
		GoalProgress progress = reputation.progress(data(Map.of(),
			Map.of(VarPlayerID.GIANTS_FOUNDRY_REWARD_SHOP_POINTS, 2500)));
		assertTrue(progress.isComplete());
		// En varbit med samma nummer räknas inte.
		assertNull(reputation.progress(data(Map.of(VarPlayerID.GIANTS_FOUNDRY_REWARD_SHOP_POINTS, 2500), Map.of())));
	}

	@Test
	public void unknownValueGivesUnknownProgress()
	{
		for (PointsActivity activity : PointsActivity.values())
		{
			assertNull(activity.name(), new PointsRequirement(activity, 1).progress(GameData.EMPTY));
		}
		// Räknarlagret ändrar inget för varbits och varps.
		assertNull(new PointsRequirement(PointsActivity.SLAYER, 1).progress(
			new CountedGameData(GameData.EMPTY, new ActivityCounters())));
	}

	@Test
	public void targetsAreChecked()
	{
		for (int bad : new int[]{0, -1, PointsRequirement.MAX + 1})
		{
			try
			{
				new PointsRequirement(PointsActivity.SLAYER, bad);
				fail("Expected an error for " + bad);
			}
			catch (IllegalArgumentException expected)
			{
				// Väntat.
			}
		}
	}

	@Test
	public void pointsGoalsAreSavedAndLoaded()
	{
		GoalSerializer serializer = new GoalSerializer(new Gson(), new SchemaMigrator());
		GoalTree tree = new GoalTree();
		Goal goal = new Goal("");
		goal.setRequirement(new PointsRequirement(PointsActivity.TEMPOROSS, 500));
		tree.add(null, goal);

		String json = serializer.serialize(tree);
		assertTrue(json, json.contains("\"type\":\"POINTS\"") || json.contains("POINTS"));
		LoadResult loaded = serializer.deserialize(json);
		assertFalse(loaded.isMigrated());
		assertEquals(new PointsRequirement(PointsActivity.TEMPOROSS, 500), loaded.getTree().find(goal.getId())
			.getRequirement());
		assertTrue(SchemaMigrator.CURRENT_VERSION >= 13);

		// En sparfil från version 12 läses och höjs utan att något ändras.
		LoadResult old = serializer.deserialize("{\"schemaVersion\":12,\"rootIds\":[\"a\"],\"goals\":[{\"id\":\"a\","
			+ "\"title\":\"Plan\",\"childIds\":[]}]}");
		assertTrue(old.isMigrated());
		assertEquals("Plan", old.getTree().find("a").getTitle());
	}

	@Test
	public void pointsGoalsWatchTheirVariableAndHaveAKey()
	{
		GoalTree tree = new GoalTree();
		Goal slayer = new Goal("");
		slayer.setRequirement(new PointsRequirement(PointsActivity.SLAYER, 750));
		Goal zeal = new Goal("");
		zeal.setRequirement(new PointsRequirement(PointsActivity.SOUL_WARS, 100));
		tree.add(null, slayer);
		tree.add(null, zeal);

		WatchedAchievements watched = WatchedAchievements.from(tree);
		assertTrue(watched.getVarbits().contains(VarbitID.SLAYER_POINTS));
		assertTrue(watched.getVarps().contains(VarPlayerID.SOUL_WARS_ZEAL));
		assertEquals("points:SLAYER:750", TemplateKeys.of(slayer.getRequirement()));
		assertEquals(RequirementType.POINTS, slayer.getRequirement().getType());
		assertTrue(CompletedLog.TypeFilter.POINTS.matches(slayer));
	}

	@Test
	public void iconSkillsAreRealSkills()
	{
		assertEquals(Skill.SLAYER, PointsActivity.SLAYER.getIconSkill());
		assertNull(PointsActivity.SOUL_WARS.getIconSkill());
	}

	private static GameData data(Map<Integer, Integer> varbits, Map<Integer, Integer> varps)
	{
		return new GameData()
		{
			@Override
			public long getXp(Skill skill)
			{
				return -1;
			}

			@Override
			public int getVarbit(int varbitId)
			{
				return varbits.getOrDefault(varbitId, -1);
			}

			@Override
			public int getVarp(int varpId)
			{
				return varps.getOrDefault(varpId, -1);
			}
		};
	}
}
