package com.gielinorgoals.template;

import com.gielinorgoals.model.DropSource;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalIcon;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.SkillRequirement;
import com.gielinorgoals.model.TotalLevelRequirement;
import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.client.hiscore.HiscoreSkill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class JsonTemplateFormatTest
{
	private static final String ALL_TYPES = "{\"name\":\"All\",\"root\":{\"title\":\"Root\",\"aggregate\":true,"
		+ "\"children\":["
		+ "{\"skill\":\"CRAFTING\",\"level\":70},"
		+ "{\"questPoints\":100},"
		+ "{\"totalLevel\":1500},"
		+ "{\"killCount\":{\"hiscore\":\"ZULRAH\",\"count\":100}},"
		+ "{\"killCount\":{\"npc\":\"Lizardman shaman\",\"count\":500}},"
		+ "{\"killCount\":{\"manual\":\"Slayer points\",\"count\":750},\"note\":\"Bought for 750 slayer points\"},"
		+ "{\"title\":\"Talk to Nieve\",\"note\":\"In Gnome Stronghold\",\"wikiPage\":\"Nieve\"},"
		+ "{\"item\":{\"id\":13576,\"name\":\"Dragon warhammer\",\"quantity\":1,\"includeVariants\":true},"
		+ "\"dropSource\":{\"npc\":\"Lizardman shaman\",\"rate\":5000}},"
		+ "{\"item\":{\"id\":23962,\"name\":\"Crystal shard\",\"quantity\":160}}"
		+ "]}}";

	@Test
	public void everyNodeTypeCanBeWrittenInJson()
	{
		TemplateNode root = parse(ALL_TYPES).build(Map.of());
		List<TemplateNode> children = root.getChildren();

		assertTrue(children.get(0).getRequirement().isSameGoal(SkillRequirement.level(Skill.CRAFTING, 70)));
		assertTrue(children.get(1).getRequirement().isSameGoal(new QuestPointsRequirement(100)));
		assertTrue(children.get(2).getRequirement().isSameGoal(new TotalLevelRequirement(1500)));
		assertTrue(children.get(3).getRequirement().isSameGoal(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 100)));
		assertTrue(children.get(4).getRequirement().isSameGoal(KillCountRequirement.npc("Lizardman shaman", 500)));
		assertTrue(children.get(5).getRequirement().isSameGoal(KillCountRequirement.manual("Slayer points", 750)));
		assertEquals("Bought for 750 slayer points", children.get(5).getNote());

		TemplateNode manual = children.get(6);
		assertNull(manual.getRequirement());
		assertEquals("Talk to Nieve", manual.getTitle());
		assertEquals("In Gnome Stronghold", manual.getNote());
		assertEquals("Nieve", manual.getWikiPage());

		ItemRequirement dwh = (ItemRequirement) children.get(7).getRequirement();
		assertTrue(dwh.isIncludeVariants());
		assertEquals(DropSource.npc("Lizardman shaman", 5000), children.get(7).getDropSource());
		assertEquals(160, ((ItemRequirement) children.get(8).getRequirement()).getQuantity());
	}

	@Test
	public void newNodeTypesHaveStableKeys()
	{
		List<TemplateNode> children = parse(ALL_TYPES).build(Map.of()).getChildren();
		List<TemplateNode> again = parse(ALL_TYPES).build(Map.of()).getChildren();

		assertEquals("kc:hiscore:ZULRAH:100", children.get(3).getKey());
		assertEquals("kc:npc:lizardman shaman:500", children.get(4).getKey());
		assertEquals("kc:manual:slayer points:750", children.get(5).getKey());
		assertEquals("group:talk to nieve", children.get(6).getKey());
		assertEquals("item:13576", children.get(7).getKey());
		assertEquals("item:23962:160", children.get(8).getKey());
		for (int i = 0; i < children.size(); i++)
		{
			assertEquals(children.get(i).getKey(), again.get(i).getKey());
		}
	}

	@Test
	public void insertedGoalsGetNoteAndDropSource()
	{
		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, parse(ALL_TYPES).build(Map.of()), null);

		Goal dwh = find(tree, "item:13576");
		assertEquals(DropSource.npc("Lizardman shaman", 5000), dwh.getDropSource());
		assertEquals("Bought for 750 slayer points", find(tree, "kc:manual:slayer points:750").getNote());
		assertEquals("Nieve", find(tree, "group:talk to nieve").getWikiPage());
	}

	@Test
	public void expandChainFalseKeepsTheQuestAlone()
	{
		String json = "{\"name\":\"Q\",\"root\":{\"title\":\"Root\",\"children\":["
			+ "{\"quest\":\"RECIPE_FOR_DISASTER\",\"expandChain\":false},"
			+ "{\"quest\":\"MONKEY_MADNESS_II\"}]}}";
		TemplateNode root = parse(json).build(Map.of());

		QuestChains.expand(root, QuestRequirementsDb.load(new Gson()));

		assertTrue(root.getChildren().get(0).getChildren().isEmpty());
		assertFalse(root.getChildren().get(1).getChildren().isEmpty());
	}

	@Test
	public void groupsCanHaveTheirOwnIcons()
	{
		String json = "{\"name\":\"I\",\"root\":{\"title\":\"Root\",\"icon\":{\"item\":25865},\"children\":["
			+ "{\"title\":\"Unlock\",\"icon\":{\"sideIcon\":\"QUEST\"}},"
			+ "{\"title\":\"Uniques\",\"icon\":{\"activity\":\"THE_HUEYCOATL\"}}]}}";
		JsonTemplate template = parse(json);
		TemplateNode root = template.build(Map.of());

		assertEquals(GoalIcon.item(25865), root.getIcon());
		assertEquals(GoalIcon.item(25865), template.getIcon());
		assertEquals(GoalIcon.side(GoalIcon.SideIcon.QUEST), root.getChildren().get(0).getIcon());
		assertEquals(GoalIcon.activity(HiscoreSkill.THE_HUEYCOATL), root.getChildren().get(1).getIcon());

		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, root, null);
		assertEquals(GoalIcon.side(GoalIcon.SideIcon.QUEST), find(tree, "group:unlock").getIcon());

		rejects("{\"name\":\"X\",\"root\":{\"title\":\"R\",\"icon\":{\"item\":1,\"sideIcon\":\"QUEST\"}}}");
		rejects("{\"name\":\"X\",\"root\":{\"title\":\"R\",\"icon\":{\"sideIcon\":\"NOPE\"}}}");
		rejects("{\"name\":\"X\",\"root\":{\"title\":\"R\",\"icon\":{\"activity\":\"NOT_A_BOSS\"}}}");
	}

	@Test
	public void oldVariantsFieldStillWorks()
	{
		String json = "{\"name\":\"V\",\"root\":{\"item\":{\"id\":7462,\"name\":\"Barrows gloves\",\"variants\":true}}}";

		assertTrue(((ItemRequirement) parse(json).build(Map.of()).getRequirement()).isIncludeVariants());
	}

	@Test
	public void brokenNodesAreRejected()
	{
		rejects("{\"name\":\"X\",\"root\":{\"title\":\"R\",\"dropSource\":{\"npc\":\"A\",\"rate\":10}}}");
		rejects("{\"name\":\"X\",\"root\":{\"skill\":\"ATTACK\",\"level\":5,\"expandChain\":false}}");
		rejects("{\"name\":\"X\",\"root\":{\"killCount\":{\"hiscore\":\"ZULRAH\",\"npc\":\"Zulrah\",\"count\":1}}}");
		rejects("{\"name\":\"X\",\"root\":{\"killCount\":{\"hiscore\":\"ZULRAH\"}}}");
		rejects("{\"name\":\"X\",\"root\":{\"killCount\":{\"hiscore\":\"NOT_A_BOSS\",\"count\":1}}}");
		rejects("{\"name\":\"X\",\"root\":{\"item\":{\"id\":1,\"name\":\"I\"},\"dropSource\":{\"npc\":\"A\"}}}");
	}

	private static void rejects(String json)
	{
		try
		{
			parse(json);
			fail("Should reject " + json);
		}
		catch (IllegalArgumentException expected)
		{
			// Trasiga filer ska ge ett fel när de läses, inte en halv template.
		}
	}

	private static Goal find(GoalTree tree, String key)
	{
		for (Goal goal : tree.flatten())
		{
			if (key.equals(goal.getTemplateNodeKey()))
			{
				return goal;
			}
		}
		throw new AssertionError("No goal with key " + key);
	}

	private static JsonTemplate parse(String json)
	{
		return JsonTemplate.from(new Gson().fromJson(json, JsonTemplate.FileSpec.class));
	}
}
