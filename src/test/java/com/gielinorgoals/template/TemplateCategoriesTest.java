package com.gielinorgoals.template;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TemplateCategoriesTest
{
	private final List<GoalTemplate> templates = new TemplateCatalog(new Gson()).getTemplates();

	@Test
	public void existingTemplatesHaveTheirCategories()
	{
		Map<String, List<GoalTemplate>> grouped = TemplateCategories.group(templates, "", false);

		assertEquals(List.of(TemplateCategories.CAPES, TemplateCategories.IRONMAN_MID, TemplateCategories.IRONMAN_LATE,
			TemplateCategories.SKILLS), new ArrayList<>(grouped.keySet()));
		assertEquals(List.of("Quest cape", "Achievement diary cape"), names(grouped.get(TemplateCategories.CAPES)));
		assertEquals("Barrows gloves", names(grouped.get(TemplateCategories.IRONMAN_MID)).get(0));
		assertEquals(List.of("Base N"), names(grouped.get(TemplateCategories.SKILLS)));
	}

	@Test
	public void ironmanCategoriesComeFirstForIronmen()
	{
		Map<String, List<GoalTemplate>> grouped = TemplateCategories.group(templates, "", true);

		assertEquals(List.of(TemplateCategories.IRONMAN_MID, TemplateCategories.IRONMAN_LATE, TemplateCategories.CAPES,
			TemplateCategories.SKILLS), new ArrayList<>(grouped.keySet()));
	}

	@Test
	public void searchMatchesNamesAndTags()
	{
		assertEquals(List.of("Barrows gloves"), names(flat(TemplateCategories.group(templates, "rfd", false))));
		assertEquals(List.of("Quest cape", "Barrows gloves"),
			names(flat(TemplateCategories.group(templates, "QUEST", false))));
		assertEquals(List.of("Base N"), names(flat(TemplateCategories.group(templates, "base", false))));
		assertTrue(TemplateCategories.group(templates, "nothing like this", false).isEmpty());
	}

	@Test
	public void unknownCategoryEndsUpInOther()
	{
		JsonTemplate.FileSpec spec = new Gson().fromJson("{\"name\":\"Odd\",\"category\":\"Typo\","
			+ "\"root\":{\"title\":\"Root\"}}", JsonTemplate.FileSpec.class);
		Map<String, List<GoalTemplate>> grouped = TemplateCategories.group(List.of(JsonTemplate.from(spec)), "", false);

		assertEquals(List.of(TemplateCategories.OTHER), new ArrayList<>(grouped.keySet()));
	}

	@Test
	public void jsonNodesCanHaveTheirOwnWikiPage()
	{
		JsonTemplate.FileSpec spec = new Gson().fromJson("{\"name\":\"Wiki\",\"root\":{\"title\":\"Root\","
			+ "\"wikiPage\":\"Culinaromancer's Chest\"}}", JsonTemplate.FileSpec.class);

		assertEquals("Culinaromancer's Chest", JsonTemplate.from(spec).build(Map.of()).getWikiPage());
	}

	private static List<GoalTemplate> flat(Map<String, List<GoalTemplate>> grouped)
	{
		List<GoalTemplate> all = new ArrayList<>();
		grouped.values().forEach(all::addAll);
		return all;
	}

	private static List<String> names(List<GoalTemplate> templates)
	{
		List<String> names = new ArrayList<>();
		templates.forEach(template -> names.add(template.getName()));
		return names;
	}
}
