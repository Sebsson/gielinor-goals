package com.gielinorgoals.template;

import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.SkillRequirement;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class QuestChainsTest
{
	private final QuestRequirementsDb db = QuestRequirementsDb.load(new Gson());

	@Test
	public void eachQuestIsExpandedOnceAndLaterCopiesAreLeaves()
	{
		TemplateNode rfd = QuestChains.forQuest("", quest(Quest.RECIPE_FOR_DISASTER), false, db, null);

		Map<Integer, Integer> withChildren = new HashMap<>();
		for (TemplateNode node : all(rfd))
		{
			if (node.getRequirement() instanceof QuestRequirement && !node.getChildren().isEmpty())
			{
				withChildren.merge(((QuestRequirement) node.getRequirement()).getQuestId(), 1, Integer::sum);
			}
		}
		for (Map.Entry<Integer, Integer> entry : withChildren.entrySet())
		{
			assertEquals("quest " + entry.getKey(), Integer.valueOf(1), entry.getValue());
		}
		// Priest in Peril finns i flera grenar (Nature Spirit, Desert Treasure I).
		assertTrue(count(rfd, quest(Quest.PRIEST_IN_PERIL)) > 1);
	}

	@Test
	public void onlyTheHighestSkillAndQuestPointRequirementsBecomeGoals()
	{
		TemplateNode rfd = QuestChains.forQuest("", quest(Quest.RECIPE_FOR_DISASTER), false, db, null);

		Set<Integer> cookingLevels = new HashSet<>();
		Set<Integer> agilityLevels = new HashSet<>();
		Set<Integer> questPoints = new HashSet<>();
		for (TemplateNode node : all(rfd))
		{
			Requirement requirement = node.getRequirement();
			if (requirement instanceof SkillRequirement && ((SkillRequirement) requirement).getSkill() == Skill.COOKING)
			{
				cookingLevels.add((int) ((SkillRequirement) requirement).getTarget());
				// Cooking 70 går att boosta enligt wikin.
				assertEquals("70 Cooking (boostable)", node.getDisplayTitle());
			}
			if (requirement instanceof SkillRequirement && ((SkillRequirement) requirement).getSkill() == Skill.AGILITY)
			{
				agilityLevels.add((int) ((SkillRequirement) requirement).getTarget());
				// Agility 48 går inte att boosta.
				assertEquals("48 Agility", node.getDisplayTitle());
			}
			if (requirement instanceof QuestPointsRequirement)
			{
				questPoints.add(((QuestPointsRequirement) requirement).getTarget());
			}
		}
		assertEquals(Set.of(70), cookingLevels);
		assertEquals(Set.of(48), agilityLevels);
		assertEquals(Set.of(175), questPoints);
	}

	@Test
	public void partialRequirementBecomesOneManualStep()
	{
		TemplateNode rfd = QuestChains.forQuest("", quest(Quest.RECIPE_FOR_DISASTER), false, db, null);

		List<String> partial = new ArrayList<>();
		for (TemplateNode node : all(rfd))
		{
			if (node.getRequirement() == null && node.getTitle().endsWith("(partial requirement)"))
			{
				partial.add(node.getTitle());
			}
		}
		assertEquals(1, partial.size());
		assertTrue(partial.get(0).contains("Legends' Quest"));
	}

	@Test
	public void insertedChainHasNoDuplicatesAndSharesQuests()
	{
		GoalTree tree = new GoalTree();
		Goal existingPriest = new Goal("");
		existingPriest.setRequirement(quest(Quest.PRIEST_IN_PERIL));
		Goal existingAgility = new Goal("");
		existingAgility.setRequirement(SkillRequirement.level(Skill.AGILITY, 48));
		Goal nearAgility = new Goal("");
		nearAgility.setRequirement(SkillRequirement.level(Skill.AGILITY, 50));
		tree.add(null, existingPriest);
		tree.add(null, existingAgility);
		tree.add(null, nearAgility);

		TemplateNode gloves = HandpickedTemplatesTest.barrowsGloves().build(Map.of());
		QuestChains.expand(gloves, db, tree);
		TemplatePlanner.Plan plan = TemplatePlanner.plan(tree, gloves, GameData.EMPTY);
		TemplateInserter.Result result = TemplateInserter.insert(tree, gloves);

		// Varje krav finns som ett enda mål.
		List<String> seen = new ArrayList<>();
		for (Goal goal : tree.flatten())
		{
			if (goal.getRequirement() != null)
			{
				for (String other : seen)
				{
					assertFalse("Duplicate " + goal.getDisplayTitle(), other.equals(key(goal.getRequirement())));
				}
				seen.add(key(goal.getRequirement()));
			}
		}
		// Befintliga exakta mål kopplas in; Agility 50 är inte samma som 48.
		assertTrue(result.getLinked().contains(existingPriest));
		assertTrue(result.getLinked().contains(existingAgility));
		assertFalse(result.getLinked().contains(nearAgility));
		assertTrue(tree.getParents(existingPriest.getId()).size() > 1);
		assertEquals(result.getCreated().size() - 1, plan.getNewGoals());
	}

	@Test
	public void questWithoutDataStaysAPlainQuestGoal()
	{
		Quest missing = null;
		for (Quest quest : Quest.values())
		{
			if (db.get(quest.getId()) == null)
			{
				missing = quest;
				break;
			}
		}
		TemplateNode node = QuestChains.forQuest("", quest(missing), false, db, null);

		assertTrue(node.getChildren().isEmpty());
	}

	@Test
	public void questCapeIsFlatByDefaultAndHandpickedTemplatesExpand()
	{
		assertFalse(new QuestCapeTemplate(QuestCapeExclusions.load(new Gson())).defaultExpandQuestChains());
		assertTrue(HandpickedTemplatesTest.barrowsGloves().defaultExpandQuestChains());
		assertFalse(QuestChains.hasQuestNodes(new BaseLevelTemplate().build(Map.of())));
	}

	private static QuestRequirement quest(Quest quest)
	{
		return new QuestRequirement(quest.getId(), quest.getName());
	}

	private static String key(Requirement requirement)
	{
		if (requirement instanceof QuestRequirement)
		{
			return "quest:" + ((QuestRequirement) requirement).getQuestId();
		}
		return requirement.getType() + ":" + requirement.describe();
	}

	private static int count(TemplateNode root, Requirement requirement)
	{
		int count = 0;
		for (TemplateNode node : all(root))
		{
			if (requirement.isSameGoal(node.getRequirement()))
			{
				count++;
			}
		}
		return count;
	}

	private static List<TemplateNode> all(TemplateNode root)
	{
		List<TemplateNode> nodes = new ArrayList<>();
		collect(root, nodes);
		return nodes;
	}

	private static void collect(TemplateNode node, List<TemplateNode> out)
	{
		out.add(node);
		for (TemplateNode child : node.getChildren())
		{
			collect(child, out);
		}
	}
}
