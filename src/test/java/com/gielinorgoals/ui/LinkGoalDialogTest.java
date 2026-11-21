package com.gielinorgoals.ui;

import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class LinkGoalDialogTest
{
	@Test
	public void cycleAndExistingLinksAreBlockedWithReason()
	{
		GoalTree tree = new GoalTree();
		Goal gloves = new Goal("Barrows gloves");
		Goal rfd = new Goal("Recipe for Disaster");
		Goal sub = new Goal("Desert Treasure");
		Goal questCape = new Goal("Quest cape");
		tree.add(null, gloves);
		tree.add(gloves.getId(), rfd);
		tree.add(rfd.getId(), sub);
		tree.add(null, questCape);

		List<LinkGoalDialog.Option> options = LinkGoalDialog.options(tree, rfd);

		// Målet självt finns inte med; ordningen följer trädet.
		assertEquals(3, options.size());
		assertEquals(gloves, options.get(0).getGoal());
		assertEquals("already linked", options.get(0).getBlockedReason());
		assertEquals(sub, options.get(1).getGoal());
		assertEquals("sub-goal of this goal", options.get(1).getBlockedReason());
		assertEquals(questCape, options.get(2).getGoal());
		assertNull(options.get(2).getBlockedReason());
	}
}
