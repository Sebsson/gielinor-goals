package com.gielinorgoals.template;

import com.gielinorgoals.TestGoalManagers;
import com.gielinorgoals.GoalManager;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.PointsActivity;
import com.gielinorgoals.model.PointsRequirement;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Template-versionering: Rune pouch version 2 byter den manuella räknaren för slayer points mot
 * ett POINTS-mål, och den som har version 1 insatt får bytet som en uppdatering.
 */
public class TemplateVersionTest
{
	private final Gson gson = new Gson();

	@Test
	public void runePouchVersionOneGetsTheSwapAsAnUpdate() throws IOException
	{
		GoalManager manager = TestGoalManagers.inMemory("profile");
		TemplateCatalog catalog = new TemplateCatalog(gson);
		TemplateUpdates updates = new TemplateUpdates(catalog, QuestRequirementsDb.load(gson));
		Goal root = manager.insertTemplate(versionOne()).getRoot();
		Goal oldCounter = only(manager, "kc:manual:slayer points:750");
		assertNull(root.getTemplateVersion());
		assertTrue(updates.hasUpdate(manager.getTree(), root, manager.getRevision()));

		TemplateUpdates.Plan plan = updates.plan(manager.getTree(), root);
		// Bytet är en ändring: ett nytt mål som ersätter det gamla, inget löst borttaget mål.
		assertEquals(1, plan.getAdditions().size());
		TemplateUpdates.Addition swap = plan.getAdditions().get(0);
		assertEquals(new PointsRequirement(PointsActivity.SLAYER, 750), swap.getNode().getRequirement());
		assertSame(oldCounter, swap.getReplaces());
		assertTrue(plan.getRemoved().isEmpty());
		assertTrue(plan.isNewVersion());
		assertEquals(1, plan.getGoalVersion());
		assertEquals(2, plan.getTemplateVersion());

		// Som dialogen gör när bytet är valt: det nya målet läggs till och det gamla tas bort.
		manager.applyTemplateUpdate(root.getId(), plan.getAdditions(), List.of(oldCounter.getId()), List.of(),
			plan.getTemplateVersion());
		assertEquals(Integer.valueOf(2), root.getTemplateVersion());
		assertNull(find(manager, "kc:manual:slayer points:750"));
		Goal points = only(manager, "points:SLAYER:750");
		assertEquals(List.of("item:12791"), points.getDoneWhen());
		// Under Unlock, som i templaten.
		assertEquals("Unlock", manager.getTree().getParents(points.getId()).get(0).getTitle());
		assertFalse(updates.hasUpdate(manager.getTree(), root, manager.getRevision()));
	}

	@Test
	public void decliningTheSwapKeepsTheManualCounter() throws IOException
	{
		GoalManager manager = TestGoalManagers.inMemory("profile");
		TemplateUpdates updates = new TemplateUpdates(new TemplateCatalog(gson), QuestRequirementsDb.load(gson));
		Goal root = manager.insertTemplate(versionOne()).getRoot();
		TemplateUpdates.Plan plan = updates.plan(manager.getTree(), root);

		manager.applyTemplateUpdate(root.getId(), List.of(), List.of(),
			List.of(plan.getAdditions().get(0).getNode().getKey()), plan.getTemplateVersion());
		assertNotNull(find(manager, "kc:manual:slayer points:750"));
		assertNull(find(manager, "points:SLAYER:750"));
		assertFalse(updates.hasUpdate(manager.getTree(), root, manager.getRevision()));
	}

	@Test
	public void newInsertsGetTheCurrentVersion()
	{
		GoalTemplate template = new TemplateCatalog(gson).find("rune-pouch");
		assertEquals(2, template.getVersion());
		assertEquals(1, new TemplateCatalog(gson).find("barrows-gloves").getVersion());
	}

	// Rune pouch som den såg ut i version 1, med en manuell räknare för slayer points.
	private TemplateNode versionOne() throws IOException
	{
		String json;
		try (InputStream in = JsonTemplate.class.getResourceAsStream("handpicked/rune-pouch.json"))
		{
			json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		String old = json.replace("\"version\": 2,", "")
			.replace("{\"points\": {\"activity\": \"SLAYER\", \"count\": 750},", "{\"killCount\": {\"manual\": \"Slayer points\", \"count\": 750},")
			.replace("\"replaces\": [\"kc:manual:slayer points:750\"],", "");
		assertTrue(old.contains("\"manual\": \"Slayer points\""));
		TemplateNode node = JsonTemplate.from(gson.fromJson(old, JsonTemplate.FileSpec.class)).build(Map.of());
		assertTrue(KillCountRequirement.manual("Slayer points", 750).isSameGoal(
			node.getChildren().get(0).getChildren().get(0).getRequirement()));
		node.setTemplateId("rune-pouch");
		node.setTemplateParams(Map.of(TemplateUpdates.QUEST_CHAINS_PARAM, 0));
		return node;
	}

	private static Goal find(GoalManager manager, String key)
	{
		List<Goal> found = new ArrayList<>();
		for (Goal goal : manager.getTree().flatten())
		{
			if (key.equals(TemplateKeys.of(goal)))
			{
				found.add(goal);
			}
		}
		assertTrue(found.size() <= 1);
		return found.isEmpty() ? null : found.get(0);
	}

	private static Goal only(GoalManager manager, String key)
	{
		Goal goal = find(manager, key);
		assertNotNull("No goal " + key, goal);
		return goal;
	}
}
