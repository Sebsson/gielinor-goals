package com.gielinorgoals.model;

import com.gielinorgoals.template.TemplateKeys;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DoneWhenTest
{
	private static final int BRONZE = 8844;
	private static final int IRON = 8845;
	private static final int DRAGON = 12954;

	@Test
	public void aChainCompletesInOneEvaluation()
	{
		GoalTree tree = new GoalTree();
		Goal group = new Goal("Obtain");
		group.setAggregate(true);
		tree.add(null, group);
		Goal bronze = item(BRONZE, "item:" + IRON);
		Goal iron = item(IRON, "item:" + DRAGON);
		Goal dragon = item(DRAGON);
		tree.add(group.getId(), bronze);
		tree.add(group.getId(), iron);
		tree.add(group.getId(), dragon);
		Goal guild = new Goal("Enter the Warriors' Guild");
		guild.setDoneWhen(List.of("item:" + BRONZE));
		tree.add(null, guild);

		// Bara dragon defendern finns i banken.
		GoalEvaluator.Result result = GoalEvaluator.evaluate(tree, owns(DRAGON), 5L, TemplateKeys::of);

		assertTrue(dragon.isCompleted());
		assertTrue(iron.isCompleted());
		assertTrue(bronze.isCompleted());
		assertTrue(guild.isCompleted());
		// Räknargruppen blir klar när de härledda delmålen är klara.
		assertTrue(group.isCompleted());
		assertEquals(Set.of(dragon, iron, bronze, guild, group), Set.copyOf(result.getNewlyCompleted()));
	}

	@Test
	public void nothingIsImpliedByAnOpenGoal()
	{
		GoalTree tree = new GoalTree();
		Goal bronze = item(BRONZE, "item:" + IRON);
		Goal iron = item(IRON);
		tree.add(null, bronze);
		tree.add(null, iron);

		GoalEvaluator.evaluate(tree, owns(-1), 5L, TemplateKeys::of);

		assertFalse(bronze.isCompleted());
		assertFalse(iron.isCompleted());
	}

	@Test
	public void manuallyCompletedGoalsImplyToo()
	{
		GoalTree tree = new GoalTree();
		Goal torso = new Goal("Fighter torso");
		torso.setTemplateNodeKey("item:10551");
		Goal points = new Goal("BA attacker points");
		points.setDoneWhen(List.of("item:10551"));
		tree.add(null, torso);
		tree.add(null, points);
		torso.markCompleted(true, 1L);

		GoalEvaluator.evaluate(tree, owns(-1), 5L, TemplateKeys::of);

		assertTrue(points.isCompleted());
	}

	private static Goal item(int itemId, String... doneWhen)
	{
		Goal goal = new Goal("");
		goal.setRequirement(new ItemRequirement(itemId, "Item " + itemId, 1, false));
		if (doneWhen.length > 0)
		{
			goal.setDoneWhen(List.of(doneWhen));
		}
		return goal;
	}

	private static GameData owns(int ownedItemId)
	{
		return new GameData()
		{
			@Override
			public long getXp(net.runelite.api.Skill skill)
			{
				return 0;
			}

			@Override
			public long getItemCount(int itemId)
			{
				return itemId == ownedItemId ? 1 : 0;
			}
		};
	}
}
