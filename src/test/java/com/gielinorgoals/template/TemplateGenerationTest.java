package com.gielinorgoals.template;

import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.SkillRequirement;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TemplateGenerationTest
{
	private final QuestCapeExclusions exclusions = QuestCapeExclusions.load(new Gson());

	@Test
	public void everyExcludedNameExistsInTheQuestEnum() throws Exception
	{
		JsonObject raw;
		try (InputStreamReader in = new InputStreamReader(
			QuestCapeExclusions.class.getResourceAsStream(QuestCapeExclusions.RESOURCE), StandardCharsets.UTF_8))
		{
			raw = new Gson().fromJson(in, JsonObject.class);
		}

		// Ett namn som RuneLite har bytt skulle tyst hoppas över; här märks det.
		assertEquals(raw.getAsJsonArray("miniquests").size(), exclusions.getMiniquests().size());
		assertEquals(raw.getAsJsonArray("subquests").size(), exclusions.getSubquests().size());
		assertEquals(19, exclusions.getMiniquests().size());
		assertEquals(10, exclusions.getSubquests().size());
	}

	@Test
	public void questCapeSkipsMiniquestsAndRecipeForDisasterParts()
	{
		TemplateNode cape = new QuestCapeTemplate(exclusions).build(Map.of());

		assertTrue(cape.isAggregate());
		assertEquals("Quest cape", cape.getTitle());
		int expected = Quest.values().length - exclusions.getMiniquests().size() - exclusions.getSubquests().size();
		assertEquals(expected, cape.getChildren().size());

		Set<Integer> ids = questIds(cape);
		assertTrue(ids.contains(Quest.RECIPE_FOR_DISASTER.getId()));
		assertFalse(ids.contains(Quest.RECIPE_FOR_DISASTER__CULINAROMANCER.getId()));
		assertFalse(ids.contains(Quest.MAGE_ARENA_I.getId()));
		assertFalse(ids.contains(Quest.ALFRED_GRIMHANDS_BARCRAWL.getId()));
		// Bone Voyage låter som en miniquest men är en quest.
		assertTrue(ids.contains(Quest.BONE_VOYAGE.getId()));
		assertEquals(cape.getChildren().size(), ids.size());
	}

	@Test
	public void questCapeWithoutExclusionsHasEveryQuest()
	{
		TemplateNode cape = new QuestCapeTemplate(QuestCapeExclusions.none()).build(Map.of());

		assertEquals(Quest.values().length, cape.getChildren().size());
	}

	@Test
	public void diaryCapeHasFourTiersInTwelveAreas()
	{
		TemplateNode cape = new DiaryCapeTemplate().build(Map.of());

		assertEquals(12, DiaryArea.values().length);
		assertEquals(DiaryArea.values().length, cape.getChildren().size());
		for (int i = 0; i < DiaryArea.values().length; i++)
		{
			TemplateNode area = cape.getChildren().get(i);
			assertTrue(area.isAggregate());
			assertEquals(DiaryTier.values().length, area.getChildren().size());
			for (int t = 0; t < DiaryTier.values().length; t++)
			{
				assertEquals(new DiaryRequirement(DiaryArea.values()[i], DiaryTier.values()[t]),
					area.getChildren().get(t).getRequirement());
			}
		}
		assertEquals(1 + 12 + 12 * 4, cape.size());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void baseLevelHasOneGoalPerSkillWithoutOverall()
	{
		BaseLevelTemplate template = new BaseLevelTemplate();

		TemplateNode base70 = template.build(Map.of());
		assertEquals("Base 70", base70.getTitle());
		// OVERALL finns inte alltid i Skill.values(), så räkna de riktiga skillsen.
		long realSkills = java.util.Arrays.stream(Skill.values()).filter(skill -> skill != Skill.OVERALL).count();
		assertEquals(realSkills, base70.getChildren().size());
		Set<Skill> skills = new HashSet<>();
		for (TemplateNode node : base70.getChildren())
		{
			SkillRequirement requirement = (SkillRequirement) node.getRequirement();
			assertEquals(SkillRequirement.Mode.LEVEL, requirement.getMode());
			assertEquals(70, requirement.getTarget());
			skills.add(requirement.getSkill());
		}
		assertFalse(skills.contains(Skill.OVERALL));
		assertTrue(skills.contains(Skill.SAILING));

		assertEquals("Base 99", template.build(Map.of("level", 99)).getTitle());
		// Utanför gränserna klampas värdet.
		assertEquals("Base 99", template.build(Map.of("level", 150)).getTitle());
		assertEquals("Base 2", template.build(Map.of("level", 1)).getTitle());
	}

	@Test
	public void everyBuildGivesAFreshTree()
	{
		QuestCapeTemplate template = new QuestCapeTemplate(exclusions);
		TemplateNode first = template.build(Map.of());
		first.getChildren().get(0).setIncluded(false);

		assertTrue(template.build(Map.of()).getChildren().get(0).isIncluded());
	}

	private static Set<Integer> questIds(TemplateNode cape)
	{
		Set<Integer> ids = new HashSet<>();
		for (TemplateNode node : cape.getChildren())
		{
			ids.add(((QuestRequirement) node.getRequirement()).getQuestId());
		}
		return ids;
	}
}
