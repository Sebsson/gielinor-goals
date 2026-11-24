package com.gielinorgoals.template;

import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Template-referenser ({"template": "..."}) och paket (milstolpe 14, del 1).
 */
public class TemplateReferenceTest
{
	private final Gson gson = new Gson();

	private static final String A = "{\"id\":\"a\",\"name\":\"Template A\",\"root\":{\"title\":\"A\",\"aggregate\":true,"
		+ "\"icon\":{\"sideIcon\":\"QUEST\"},\"children\":[{\"quest\":\"COOKS_ASSISTANT\"},{\"quest\":\"DEMON_SLAYER\"}]}}";
	private static final String B = "{\"id\":\"b\",\"name\":\"Template B\",\"root\":{\"title\":\"B\",\"aggregate\":true,"
		+ "\"children\":[{\"quest\":\"COOKS_ASSISTANT\"},{\"quest\":\"RUNE_MYSTERIES\"}]}}";

	@Test
	public void referenceExpandsToTheWholeTemplate()
	{
		TemplateCatalog catalog = catalog(A, pack("{\"template\":\"a\"},{\"template\":\"base-level\",\"params\":{\"level\":70}}"));
		assertTrue(catalog.getLoadErrors().toString(), catalog.getLoadErrors().isEmpty());
		TemplateNode root = catalog.find("pack").build(Map.of());

		TemplateNode a = root.getChildren().get(0);
		assertEquals("A", a.getTitle());
		assertEquals("a", a.getTemplateId());
		assertEquals(2, a.getChildren().size());

		TemplateNode base = root.getChildren().get(1);
		assertEquals("Base 70", base.getTitle());
		assertEquals("base-level", base.getTemplateId());
		assertEquals(Integer.valueOf(70), base.getTemplateParams().get("level"));
		assertEquals(new BaseLevelTemplate().build(Map.of("level", 70)).getChildren().size(), base.getChildren().size());
	}

	@Test
	public void choiceParametersAreUsed()
	{
		TemplateCatalog catalog = catalog(pack("{\"template\":\"skill-ladder\",\"params\":{\"skill\":\"MINING\"}}"));
		TemplateNode ladder = catalog.find("pack").build(Map.of()).getChildren().get(0);
		assertEquals("Mining 60-99", ladder.getTitle());
		assertEquals(Map.of("skill", "MINING"), ladder.getTemplateChoices());
	}

	@Test
	public void nodesSharedBetweenReferencesAreOneGoal()
	{
		TemplateCatalog catalog = catalog(A, B, pack("{\"template\":\"a\"},{\"template\":\"b\"}"));
		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, catalog.find("pack").build(Map.of()));
		Goal cook = only(tree, "quest:" + Quest.COOKS_ASSISTANT.getId());
		assertEquals(2, tree.getParents(cook.getId()).size());
	}

	@Test
	public void twoReferencesWithTheSameValuesAreOneNode()
	{
		TemplateCatalog catalog = catalog(A, pack("{\"title\":\"X\",\"aggregate\":true,\"children\":[{\"template\":\"a\"}]},"
			+ "{\"title\":\"Y\",\"aggregate\":true,\"children\":[{\"template\":\"a\"}]}"));
		TemplateNode root = catalog.find("pack").build(Map.of());
		TemplateNode second = root.getChildren().get(1).getChildren().get(0);
		assertNotNull(second.getRef());

		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, root);
		Goal a = only(tree, "group:a");
		assertEquals(2, tree.getParents(a.getId()).size());
		assertEquals(1, count(tree, "quest:" + Quest.DEMON_SLAYER.getId()));
	}

	@Test
	public void anExistingInsertOfTheTemplateIsLinkedIn()
	{
		TemplateCatalog catalog = catalog(A, pack("{\"template\":\"a\"}"));
		GoalTree tree = new GoalTree();
		TemplateNode standalone = catalog.find("a").build(Map.of());
		standalone.setTemplateId("a");
		standalone.setTemplateParams(Map.of(TemplateUpdates.QUEST_CHAINS_PARAM, 0));
		Goal existing = TemplateInserter.insert(tree, standalone).getRoot();

		TemplateNode pack = catalog.find("pack").build(Map.of());
		TemplatePlanner.Plan plan = TemplatePlanner.plan(tree, pack, com.gielinorgoals.model.GameData.EMPTY);
		assertEquals(TemplatePlanner.Status.LINKED, plan.statusOf(pack.getChildren().get(0)));
		TemplateInserter.Result result = TemplateInserter.insert(tree, pack);
		assertTrue(result.getLinked().contains(existing));
		assertEquals(1, count(tree, "group:a"));
		List<String> created = new ArrayList<>();
		result.getCreated().forEach(goal -> created.add(goal.getDisplayTitle()));
		assertEquals(created.toString(), 1, result.getCreated().size());
	}

	@Test
	public void cyclesAndUnknownIdsAreLoadErrors()
	{
		String c = "{\"id\":\"c\",\"name\":\"C\",\"root\":{\"title\":\"C\",\"aggregate\":true,\"children\":[{\"template\":\"d\"}]}}";
		String d = "{\"id\":\"d\",\"name\":\"D\",\"root\":{\"title\":\"D\",\"aggregate\":true,\"children\":[{\"template\":\"c\"}]}}";
		String e = "{\"id\":\"e\",\"name\":\"E\",\"root\":{\"title\":\"E\",\"aggregate\":true,\"children\":[{\"template\":\"c\"}]}}";
		TemplateCatalog catalog = catalog(c, d, e, A);
		assertNull(catalog.find("c"));
		assertNull(catalog.find("d"));
		assertNull(catalog.find("e"));
		assertNotNull(catalog.find("a"));
		assertTrue(catalog.getLoadErrors().toString(), catalog.getLoadErrors().stream().anyMatch(m -> m.contains("cycle")));

		TemplateCatalog unknown = catalog(pack("{\"template\":\"nope\"}"));
		assertNull(unknown.find("pack"));
		assertTrue(unknown.getLoadErrors().get(0).contains("unknown template \"nope\""));

		TemplateCatalog badParam = catalog(pack("{\"template\":\"base-level\",\"params\":{\"n\":70}}"));
		assertTrue(badParam.getLoadErrors().get(0).contains("has no parameter \"n\""));
		TemplateCatalog badChoice = catalog(pack("{\"template\":\"skill-ladder\",\"params\":{\"skill\":\"FLYING\"}}"));
		assertTrue(badChoice.getLoadErrors().get(0).contains("not a choice"));
	}

	@Test
	public void everyBuiltInTemplateLoads()
	{
		TemplateCatalog catalog = new TemplateCatalog(gson);
		assertEquals(List.of(), catalog.getLoadErrors());
	}

	@Test
	public void aReferenceToAClientDataTemplateMakesThePackageAsync()
	{
		List<GoalTemplate> templates = new ArrayList<>();
		templates.add(new CombatAchievementTemplate(TestCombatAchievements.sample()));
		templates.add(json(pack("{\"template\":\"combat-achievements\",\"params\":{\"tier\":\"Hard\"}}")));
		TemplateCatalog catalog = new TemplateCatalog(templates);
		GoalTemplate pack = catalog.find("pack");
		assertTrue(pack.needsClientData());

		List<Runnable> clientThread = new ArrayList<>();
		List<TemplateBuilder.Result> results = new ArrayList<>();
		TemplateBuilder builder = new TemplateBuilder(clientThread::add, Runnable::run);
		builder.build(pack, Map.of(), Map.of(), results::add);
		assertTrue(results.isEmpty());
		clientThread.remove(0).run();
		assertTrue(results.get(0).isReady());
		assertEquals("Combat Achievements: Hard", results.get(0).getNode().getChildren().get(0).getTitle());

		// Utloggad: samma meddelande som för CA-templaten själv.
		List<GoalTemplate> offline = new ArrayList<>();
		offline.add(new CombatAchievementTemplate(CombatAchievementSource.NONE));
		offline.add(json(pack("{\"template\":\"combat-achievements\"}")));
		builder.build(new TemplateCatalog(offline).find("pack"), Map.of(), Map.of(), results::add);
		clientThread.remove(0).run();
		assertFalse(results.get(1).isReady());
		assertEquals(TemplateUnavailableException.LOG_IN, results.get(1).getMessage());
	}

	@Test
	public void aChangedPartTemplateGivesThePackageAnUpdate()
	{
		TemplateCatalog before = catalog(A, pack("{\"template\":\"a\"}"));
		GoalTree tree = new GoalTree();
		TemplateNode node = before.find("pack").build(Map.of());
		node.setTemplateId("pack");
		node.setTemplateParams(Map.of(TemplateUpdates.QUEST_CHAINS_PARAM, 0));
		Goal root = TemplateInserter.insert(tree, node).getRoot();
		assertFalse(new TemplateUpdates(before, QuestRequirementsDb.load(gson)).hasUpdate(tree, root, 1));

		// A version 2 med ett quest till; paketets fil är oförändrad.
		String a2 = A.replace("\"name\":\"Template A\"", "\"version\":2,\"name\":\"Template A\"")
			.replace("{\"quest\":\"DEMON_SLAYER\"}", "{\"quest\":\"DEMON_SLAYER\"},{\"quest\":\"ERNEST_THE_CHICKEN\"}");
		TemplateCatalog after = catalog(a2, pack("{\"template\":\"a\"}"));
		assertEquals(2, after.find("pack").getVersion());
		TemplateUpdates updates = new TemplateUpdates(after, QuestRequirementsDb.load(gson));
		assertTrue(updates.hasUpdate(tree, root, 2));
		TemplateUpdates.Plan plan = updates.plan(tree, root);
		assertEquals(1, plan.getAdditions().size());
		assertEquals("quest:" + Quest.ERNEST_THE_CHICKEN.getId(), plan.getAdditions().get(0).getNode().getKey());
	}

	// --- Hjälpare ---

	private TemplateCatalog catalog(String... files)
	{
		List<GoalTemplate> templates = new ArrayList<>();
		templates.add(new BaseLevelTemplate());
		templates.add(new SkillLadderTemplate());
		for (String file : files)
		{
			templates.add(json(file));
		}
		return new TemplateCatalog(templates);
	}

	private JsonTemplate json(String file)
	{
		return JsonTemplate.from(gson.fromJson(file, JsonTemplate.FileSpec.class));
	}

	private static String pack(String children)
	{
		return "{\"id\":\"pack\",\"name\":\"Pack\",\"category\":\"Packages\",\"root\":{\"title\":\"Pack\",\"aggregate\":true,"
			+ "\"children\":[" + children + "]}}";
	}

	private static int count(GoalTree tree, String key)
	{
		int count = 0;
		for (Goal goal : tree.flatten())
		{
			count += key.equals(TemplateKeys.of(goal)) ? 1 : 0;
		}
		return count;
	}

	private static Goal only(GoalTree tree, String key)
	{
		assertEquals(key, 1, count(tree, key));
		for (Goal goal : tree.flatten())
		{
			if (key.equals(TemplateKeys.of(goal)))
			{
				return goal;
			}
		}
		throw new AssertionError();
	}
}
