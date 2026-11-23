package com.gielinorgoals.template;

import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.QuestRequirement;
import com.google.gson.Gson;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TemplatePlannerTest
{
	@Test
	public void planMatchesWhatInsertDoes()
	{
		GoalTree tree = new GoalTree();
		Goal cooks = new Goal("");
		cooks.setRequirement(new QuestRequirement(Quest.COOKS_ASSISTANT.getId(), "Cook's Assistant"));
		cooks.markCompleted(true, 1L);
		tree.add(null, cooks);
		TemplateNode cape = new QuestCapeTemplate(QuestCapeExclusions.load(new Gson())).build(Map.of());
		cape.getChildren().get(5).setIncluded(false);
		GameData data = finished(Set.of(Quest.DEMON_SLAYER.getId(), Quest.COOKS_ASSISTANT.getId()));

		TemplatePlanner.Plan plan = TemplatePlanner.plan(tree, cape, data);
		TemplateInserter.Result result = TemplateInserter.insert(tree, cape);

		assertEquals(result.getCreated().size() - 1, plan.getNewGoals());
		assertEquals(result.getLinked().size(), plan.getLinkedGoals());
		assertEquals(1, plan.getLinkedGoals());
		// Cook's Assistant (inkopplat och klart) och Demon Slayer (nytt men redan klart).
		assertEquals(2, plan.getAlreadyDone());
		assertEquals(cape.getChildren().size() - 2, plan.getNewGoals());
		assertEquals("Adds 1 root goal, " + plan.getNewGoals() + " new sub-goals, links 1 existing, 2 already done",
			plan.summary());
	}

	@Test
	public void statusesForLinkedSkippedAndDuplicateNodes()
	{
		GoalTree tree = new GoalTree();
		Goal rfd = new Goal("");
		rfd.setRequirement(new QuestRequirement(Quest.RECIPE_FOR_DISASTER.getId(), "Recipe for Disaster"));
		tree.add(null, rfd);
		TemplateNode gloves = HandpickedTemplatesTest.barrowsGloves().build(Map.of());
		TemplateNode rfdNode = gloves.getChildren().get(0);
		TemplateNode skills = gloves.getChildren().get(2);
		skills.getChildren().get(0).setIncluded(false);

		TemplatePlanner.Plan plan = TemplatePlanner.plan(tree, gloves, GameData.EMPTY);

		assertEquals(TemplatePlanner.Status.NEW, plan.statusOf(gloves));
		assertEquals(TemplatePlanner.Status.LINKED, plan.statusOf(rfdNode));
		// Det inkopplade målet behåller sina egna delmål, så templatens delquests läggs inte till.
		assertEquals(TemplatePlanner.Status.SKIPPED, plan.statusOf(rfdNode.getChildren().get(0)));
		assertEquals(TemplatePlanner.Status.SKIPPED, plan.statusOf(skills.getChildren().get(0)));
		assertEquals(TemplatePlanner.Status.NEW, plan.statusOf(skills.getChildren().get(1)));
		// QP-målet och skill-gruppen med 12 kvar.
		assertEquals(1 + 1 + 12, plan.getNewGoals());
	}

	@Test
	public void duplicateInsideTemplateIsMarked()
	{
		TemplateNode root = TemplateNode.aggregate("Root")
			.add(TemplateNode.requirement(new QuestRequirement(17, "Cook's Assistant")))
			.add(TemplateNode.requirement(new QuestRequirement(17, "Cook's Assistant")));

		TemplatePlanner.Plan plan = TemplatePlanner.plan(new GoalTree(), root, GameData.EMPTY);

		assertEquals(TemplatePlanner.Status.NEW, plan.statusOf(root.getChildren().get(0)));
		assertEquals(TemplatePlanner.Status.DUPLICATE, plan.statusOf(root.getChildren().get(1)));
		assertEquals(1, plan.getNewGoals());
	}

	@Test
	public void counterIsDoneWhenAllIncludedChildrenAre()
	{
		TemplateNode base = new BaseLevelTemplate().build(Map.of("level", 50));
		GameData data = skill -> skill == Skill.SAILING ? 0 : 13_034_431L;

		TemplatePlanner.Plan plan = TemplatePlanner.plan(new GoalTree(), base, data);
		assertFalse(plan.isDone(base));

		// Utan Sailing är resten klart, och då räknas räknarmålet som klart.
		for (TemplateNode child : base.getChildren())
		{
			if (child.getDisplayTitle().contains("Sailing"))
			{
				child.setIncluded(false);
			}
		}
		assertTrue(TemplatePlanner.plan(new GoalTree(), base, data).isDone(base));
	}

	private static GameData finished(Set<Integer> questIds)
	{
		return new GameData()
		{
			@Override
			public long getXp(Skill skill)
			{
				return -1;
			}

			@Override
			public QuestState getQuestState(int questId)
			{
				return questIds.contains(questId) ? QuestState.FINISHED : QuestState.NOT_STARTED;
			}
		};
	}
}
