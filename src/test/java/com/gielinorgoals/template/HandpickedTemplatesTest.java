package com.gielinorgoals.template;

import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.SkillRequirement;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class HandpickedTemplatesTest
{
	@Test
	public void catalogHasBuiltInAndHandpickedTemplates()
	{
		List<String> names = new ArrayList<>();
		new TemplateCatalog(new Gson()).getTemplates().forEach(template -> names.add(template.getName()));

		assertEquals(List.of("Quest cape", "Achievement diary cape", "Max cape", "Base N", "Skill ladder",
			"Total level N", "Combat level N", "Combat Achievements tier", "Quest series", "All diaries in a tier",
			"Diary reward items", "Barrows set", "Barrows gloves",
			"Bow of Faerdhinen + crystal armour", "Dragon warhammer", "Doom of Mokhaiotl", "The Hueycoatl",
			"Fire cape", "Infernal cape", "Dragon defender", "Fighter torso", "Ava's assembler", "Graceful outfit",
			"Rune pouch", "Imbued god cape", "Zenyte jewellery", "Ardougne cloak and fairy rings", "Prifddinas access",
			"Lunar spellbook", "Ancient Magicks", "Arceuus spellbook", "Fossil Island", "Kingdom of Miscellania",
			"Ectophial", "Dragon scimitar", "Spirit trees", "Gnome gliders", "Bonecrusher", "Ash sanctifier", "Herb sack",
			"Seed box", "Slayer helmet", "Void Knight equipment", "Elite void", "Angler's outfit", "Prospector kit",
			"Lumberjack outfit", "Pyromancer outfit", "Rogue equipment", "Farmer's outfit", "Carpenter's outfit",
			"Smiths' uniform", "Spirit angler's outfit", "Dizana's quiver", "Mythical cape", "Champion's cape", "Early ironman",
			"Mid ironman", "Late ironman", "All capes", "Toxic blowpipe + Serpentine helm", "Trident of the seas",
			"Trident of the swamp", "Dragon hunter crossbow", "Moons of Peril", "Sunfire fanatic armour",
			"Tormented bracelet + Amulet of torture", "Dragonfire shield", "Desert Treasure II rings"), names);
	}

	@Test
	public void barrowsGlovesMatchesTheWiki()
	{
		GoalTemplate template = barrowsGloves();
		TemplateNode root = template.build(Map.of());

		// Siffran i JSON-filen ska vara gameval-konstanten för Barrows gloves.
		assertEquals(new ItemRequirement(ItemID.HUNDRED_GAUNTLETS_LEVEL_10, "Barrows gloves", 1, false),
			root.getRequirement());
		assertEquals("Barrows gloves", root.getTitle());
		assertEquals(3, root.getChildren().size());

		TemplateNode rfd = root.getChildren().get(0);
		assertEquals(new QuestRequirement(Quest.RECIPE_FOR_DISASTER.getId(), Quest.RECIPE_FOR_DISASTER.getName()),
			rfd.getRequirement());
		assertEquals(10, rfd.getChildren().size());
		// Alla tio delar är exakt RFD:s delar i Quest-enumen.
		Set<Integer> parts = new HashSet<>();
		for (TemplateNode part : rfd.getChildren())
		{
			parts.add(((QuestRequirement) part.getRequirement()).getQuestId());
		}
		Set<Integer> expectedParts = new HashSet<>();
		for (Quest quest : Quest.values())
		{
			if (quest.name().startsWith("RECIPE_FOR_DISASTER__"))
			{
				expectedParts.add(quest.getId());
			}
		}
		assertEquals(expectedParts, parts);

		assertEquals(new QuestPointsRequirement(175), root.getChildren().get(1).getRequirement());

		Map<Skill, Integer> expected = new LinkedHashMap<>();
		expected.put(Skill.COOKING, 70);
		expected.put(Skill.AGILITY, 48);
		expected.put(Skill.MINING, 50);
		expected.put(Skill.FISHING, 53);
		expected.put(Skill.THIEVING, 53);
		expected.put(Skill.HERBLORE, 25);
		expected.put(Skill.MAGIC, 59);
		expected.put(Skill.SMITHING, 40);
		expected.put(Skill.FIREMAKING, 50);
		expected.put(Skill.RANGED, 40);
		expected.put(Skill.CRAFTING, 40);
		expected.put(Skill.FLETCHING, 10);
		expected.put(Skill.WOODCUTTING, 36);
		TemplateNode skills = root.getChildren().get(2);
		assertTrue(skills.isAggregate());
		Map<Skill, Integer> actual = new LinkedHashMap<>();
		for (TemplateNode node : skills.getChildren())
		{
			SkillRequirement requirement = (SkillRequirement) node.getRequirement();
			actual.put(requirement.getSkill(), (int) requirement.getTarget());
		}
		assertEquals(expected, actual);
	}

	@Test
	public void brokenTemplateIsRejectedAsAWhole()
	{
		JsonTemplate.FileSpec spec = new Gson().fromJson("{\"name\":\"Broken\",\"root\":{\"title\":\"Root\","
			+ "\"children\":[{\"quest\":\"COOKS_ASSISTANT\"},{\"quest\":\"NOT_A_QUEST\"}]}}", JsonTemplate.FileSpec.class);

		try
		{
			JsonTemplate.from(spec);
			fail("Expected the broken template to be rejected");
		}
		catch (IllegalArgumentException e)
		{
			// Väntat: hela filen avvisas.
		}
	}

	@Test
	public void everyFormatRequirementCanBeRead()
	{
		JsonTemplate.FileSpec spec = new Gson().fromJson("{\"name\":\"All\",\"root\":{\"title\":\"Root\","
			+ "\"aggregate\":true,\"children\":["
			+ "{\"totalLevel\":1500},{\"combatLevel\":100},{\"diary\":{\"area\":\"VARROCK\",\"tier\":\"HARD\"}},"
			+ "{\"item\":{\"id\":4151,\"name\":\"Abyssal whip\"}},{\"title\":\"Manual step\"}]}}",
			JsonTemplate.FileSpec.class);

		TemplateNode root = JsonTemplate.from(spec).build(Map.of());

		assertTrue(root.isAggregate());
		assertEquals(List.of("Total level 1500", "Combat level 100", "Varrock Hard diary", "Abyssal whip", "Manual step"),
			titles(root));
		assertFalse(root.getChildren().get(4).isAggregate());
	}

	static GoalTemplate barrowsGloves()
	{
		for (GoalTemplate template : JsonTemplates.load(new Gson()))
		{
			if (template.getName().equals("Barrows gloves"))
			{
				return template;
			}
		}
		throw new AssertionError("Barrows gloves template not found");
	}

	private static List<String> titles(TemplateNode root)
	{
		List<String> titles = new ArrayList<>();
		root.getChildren().forEach(child -> titles.add(child.getDisplayTitle()));
		return titles;
	}
}
