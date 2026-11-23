package com.gielinorgoals.template;

import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.SkillRequirement;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TemplateInserterTest
{
	@Test
	public void exactMatchIsLinkedInsteadOfDuplicated()
	{
		GoalTree tree = new GoalTree();
		Goal cooks = new Goal("");
		// Annat sparat namn, samma quest-id: räknas som samma mål.
		cooks.setRequirement(new QuestRequirement(17, "Cook's Assistant (old name)"));
		tree.add(null, cooks);

		TemplateNode cape = TemplateNode.aggregate("Quest cape")
			.add(TemplateNode.requirement(new QuestRequirement(17, "Cook's Assistant")))
			.add(TemplateNode.requirement(new QuestRequirement(25, "Demon Slayer")));

		TemplateInserter.Result result = TemplateInserter.insert(tree, cape);

		assertEquals(List.of(cooks), result.getLinked());
		assertEquals(2, result.getCreated().size());
		// Det befintliga målet, kapan och Demon Slayer; ingen dubblett av Cook's Assistant.
		assertEquals(3, tree.size());
		Goal root = result.getRoot();
		assertEquals(List.of(cooks.getId(), result.getCreated().get(1).getId()), root.getChildIds());
		// Det befintliga målet är inte längre ett eget rotmål, utan ligger under kappan.
		assertEquals(List.of(root.getId()), tree.getRootIds());
		assertEquals(List.of(root), tree.getParents(cooks.getId()));
	}

	@Test
	public void nearMatchCreatesNewGoal()
	{
		GoalTree tree = new GoalTree();
		Goal attack80 = new Goal("");
		attack80.setRequirement(SkillRequirement.level(Skill.ATTACK, 80));
		tree.add(null, attack80);

		TemplateNode base = TemplateNode.aggregate("Base 85")
			.add(TemplateNode.requirement(SkillRequirement.level(Skill.ATTACK, 85)));

		TemplateInserter.Result result = TemplateInserter.insert(tree, base);

		assertTrue(result.getLinked().isEmpty());
		Goal attack85 = result.getCreated().get(1);
		assertNotSame(attack80, attack85);
		assertEquals(SkillRequirement.level(Skill.ATTACK, 85), attack85.getRequirement());
		assertTrue(tree.getParents(attack80.getId()).isEmpty());
	}

	@Test
	public void sharedGoalIsLinkedIntoSecondTemplate()
	{
		GoalTree tree = new GoalTree();
		TemplateInserter.Result gloves = TemplateInserter.insert(tree, TemplateNode.aggregate("Barrows gloves")
			.add(TemplateNode.requirement(new QuestRequirement(117, "Recipe for Disaster"))));
		Goal rfd = gloves.getCreated().get(1);

		TemplateInserter.Result cape = TemplateInserter.insert(tree, TemplateNode.aggregate("Quest cape")
			.add(TemplateNode.requirement(new QuestRequirement(117, "Recipe for Disaster"))));

		assertEquals(List.of(rfd), cape.getLinked());
		assertEquals(2, tree.getParents(rfd.getId()).size());
		assertEquals(3, tree.size());
	}

	@Test
	public void sameRequirementTwiceInOneTemplateGivesOneGoal()
	{
		GoalTree tree = new GoalTree();
		TemplateNode template = TemplateNode.aggregate("Root")
			.add(TemplateNode.aggregate("A").add(TemplateNode.requirement(SkillRequirement.level(Skill.MINING, 60))))
			.add(TemplateNode.aggregate("B").add(TemplateNode.requirement(SkillRequirement.level(Skill.MINING, 60))));

		TemplateInserter.Result result = TemplateInserter.insert(tree, template);

		// Rot, A, B och ett mining-mål som ligger under både A och B.
		assertEquals(4, tree.size());
		Goal mining = result.getCreated().get(2);
		assertEquals(2, tree.getParents(mining.getId()).size());
		// Ett mål från samma insättning räknas inte som ett inkopplat befintligt mål.
		assertTrue(result.getLinked().isEmpty());
	}

	@Test
	public void excludedNodesAreSkippedWithTheirChildren()
	{
		GoalTree tree = new GoalTree();
		TemplateNode cape = new DiaryCapeTemplate().build(Map.of());
		for (TemplateNode area : cape.getChildren())
		{
			// Hoppa över Elite överallt och hela Wilderness.
			area.getChildren().get(3).setIncluded(false);
			if (area.getTitle().startsWith("Wilderness"))
			{
				area.setIncluded(false);
			}
		}

		TemplateInserter.insert(tree, cape);

		assertEquals(1 + 11 + 11 * 3, tree.size());
		for (Goal goal : tree.flatten())
		{
			if (goal.getRequirement() instanceof DiaryRequirement)
			{
				DiaryRequirement diary = (DiaryRequirement) goal.getRequirement();
				assertFalse(diary.getTier() == DiaryTier.ELITE);
				assertFalse(diary.getArea() == DiaryArea.WILDERNESS);
			}
		}
	}

	@Test
	public void templateRootIsANewCollapsedRootGoal()
	{
		GoalTree tree = new GoalTree();
		tree.add(null, new Goal("Existing"));

		TemplateInserter.Result result = TemplateInserter.insert(tree, new DiaryCapeTemplate().build(Map.of()));

		assertEquals(2, tree.getRootIds().size());
		assertEquals(result.getRoot().getId(), tree.getRootIds().get(1));
		for (Goal goal : result.getCreated())
		{
			if (goal.isAggregate())
			{
				assertFalse(goal.isExpanded());
			}
		}
		assertTrue(result.getRoot().isAggregate());
	}
}
