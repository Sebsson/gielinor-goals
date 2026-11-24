package com.gielinorgoals.template;

import com.gielinorgoals.GoalManager;
import com.gielinorgoals.TestGoalManagers;
import com.gielinorgoals.model.Goal;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * "Update from template" återställer aldrig användarens ordning: nya noder läggs sist under
 * sin förälder och resten ligger kvar där användaren dragit dem.
 */
public class UserOrderTest
{
	private final Gson gson = new Gson();
	private final TemplateCatalog catalog = new TemplateCatalog(gson);

	@Test
	public void anUpdateKeepsTheUsersOrderAndAddsLast()
	{
		GoalManager manager = TestGoalManagers.inMemory("profile");
		TemplateUpdates updates = new TemplateUpdates(catalog, QuestRequirementsDb.load(gson));
		TemplateNode node = catalog.find("graceful").build(Map.of());
		node.setTemplateId("graceful");
		node.setTemplateParams(Map.of(TemplateUpdates.QUEST_CHAINS_PARAM, 0));
		Goal root = manager.insertTemplate(node).getRoot();

		// Gruppen med flest delmål: användaren drar den sista först och tar bort den andra.
		Goal group = root;
		for (Goal goal : manager.getTree().flatten())
		{
			if (goal.getChildIds().size() > group.getChildIds().size())
			{
				group = goal;
			}
		}
		List<String> ids = new ArrayList<>(group.getChildIds());
		assertTrue(ids.size() >= 4);
		String last = ids.get(ids.size() - 1);
		manager.moveWithinParent(group.getId(), last, ids.get(0));
		String removed = ids.get(1);
		manager.removeGoal(removed);
		List<String> userOrder = new ArrayList<>(group.getChildIds());

		TemplateUpdates.Plan plan = updates.plan(manager.getTree(), root);
		assertEquals(1, plan.getAdditions().size());
		manager.applyTemplateUpdate(root.getId(), plan.getAdditions(), List.of(), List.of());

		List<String> after = group.getChildIds();
		assertEquals(userOrder, after.subList(0, userOrder.size()));
		assertEquals(userOrder.size() + 1, after.size());
		// Det återskapade målet hamnar sist.
		Goal added = manager.getTree().find(after.get(after.size() - 1));
		assertEquals(TemplateKeys.of(plan.getAdditions().get(0).getNode().getRequirement()),
			TemplateKeys.of(added.getRequirement()));
	}
}
