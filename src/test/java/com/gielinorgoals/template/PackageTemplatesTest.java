package com.gielinorgoals.template;

import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * De fyra paketen i milstolpe 14, del 2.
 */
public class PackageTemplatesTest
{
	private static final List<String> PACKAGES = List.of("package-early-iron", "package-mid-iron", "package-late-iron",
		"package-all-capes");

	private final Gson gson = new Gson();
	private final TemplateCatalog catalog = new TemplateCatalog(gson);
	private final QuestRequirementsDb db = QuestRequirementsDb.load(gson);

	@Test
	public void allFourLoadInTheirCategory()
	{
		assertEquals(List.of(), catalog.getLoadErrors());
		for (String id : PACKAGES)
		{
			GoalTemplate template = catalog.find(id);
			assertNotNull(id, template);
			assertEquals(TemplateCategories.PACKAGES, template.getCategory());
			// Bara referenser: varje barn till roten är en del-template med en anteckning.
			TemplateNode root = template.build(Map.of());
			for (TemplateNode part : root.getChildren())
			{
				assertNotNull(id + ": " + part.getTitle(), part.getTemplateId());
				assertNotNull(id + ": " + part.getTitle(), part.getNote());
			}
		}
		assertEquals(13, catalog.find("package-early-iron").build(Map.of()).getChildren().size());
		assertEquals(10, catalog.find("package-mid-iron").build(Map.of()).getChildren().size());
		assertEquals(5, catalog.find("package-late-iron").build(Map.of()).getChildren().size());
		assertEquals(10, catalog.find("package-all-capes").build(Map.of()).getChildren().size());
		assertEquals("Base 70", catalog.find("package-early-iron").build(Map.of()).getChildren().get(12).getTitle());
		assertEquals("Base 90", catalog.find("package-late-iron").build(Map.of()).getChildren().get(4).getTitle());
	}

	@Test
	public void uniqueGoalsPerPackage()
	{
		Map<String, Integer> expected = new HashMap<>();
		expected.put("package-early-iron", EARLY);
		expected.put("package-mid-iron", MID);
		expected.put("package-late-iron", LATE);
		expected.put("package-all-capes", CAPES);
		for (String id : PACKAGES)
		{
			GoalTree tree = new GoalTree();
			TemplateInserter.insert(tree, QuestChains.expand(catalog.find(id).build(Map.of()), db, tree));
			assertNoDuplicates(id, tree);
			assertEquals(id, (int) expected.get(id), tree.flatten().size());
		}
	}

	// Unika mål per paket, med quest-kedjorna utfällda per del-template som i dialogen.
	private static final int EARLY = 211;
	private static final int MID = 163;
	private static final int LATE = 69;
	private static final int CAPES = 376;

	@Test
	public void partsAlreadyInTheProfileAreLinkedNotCopied()
	{
		GoalTree tree = new GoalTree();
		Goal gloves = insertStandalone(tree, "barrows-gloves");
		Goal fire = insertStandalone(tree, "fire-cape");
		Goal questCape = insertStandalone(tree, "quest-cape");

		TemplateInserter.Result early = TemplateInserter.insert(tree,
			QuestChains.expand(catalog.find("package-early-iron").build(Map.of()), db, tree));
		assertTrue(early.getLinked().contains(gloves));
		TemplateInserter.Result capes = TemplateInserter.insert(tree,
			QuestChains.expand(catalog.find("package-all-capes").build(Map.of()), db, tree));
		assertTrue(capes.getLinked().contains(fire));
		assertTrue(capes.getLinked().contains(questCape));
		TemplateInserter.insert(tree, QuestChains.expand(catalog.find("package-mid-iron").build(Map.of()), db, tree));
		TemplateInserter.insert(tree, QuestChains.expand(catalog.find("package-late-iron").build(Map.of()), db, tree));

		assertNoDuplicates("all packages", tree);
		// Fire cape finns i Mid ironman och All capes men är en och samma insättning.
		assertEquals(1, templateRoots(tree, "fire-cape"));
		assertEquals(1, templateRoots(tree, "infernal-cape"));
		assertEquals(1, templateRoots(tree, "dizanas-quiver"));
		assertEquals(2, tree.getParents(fire.getId()).size());
	}

	private Goal insertStandalone(GoalTree tree, String id)
	{
		TemplateNode node = catalog.find(id).build(Map.of());
		node.setTemplateId(id);
		node.setTemplateParams(Map.of(TemplateUpdates.QUEST_CHAINS_PARAM, 0));
		return TemplateInserter.insert(tree, node).getRoot();
	}

	// Ingen nyckel för ett mål med krav förekommer två gånger, och varje template-rot finns en gång.
	private static void assertNoDuplicates(String what, GoalTree tree)
	{
		Map<String, Integer> counts = new HashMap<>();
		for (Goal goal : tree.flatten())
		{
			if (goal.getRequirement() != null)
			{
				counts.merge(TemplateKeys.of(goal.getRequirement()), 1, Integer::sum);
			}
			if (goal.getTemplateId() != null)
			{
				counts.merge("template:" + goal.getTemplateId() + goal.getTemplateParams() + goal.getTemplateChoices(), 1,
					Integer::sum);
			}
		}
		counts.forEach((key, count) -> assertEquals(what + ": " + key, 1, (int) count));
	}

	private static int templateRoots(GoalTree tree, String id)
	{
		int count = 0;
		for (Goal goal : tree.flatten())
		{
			count += id.equals(goal.getTemplateId()) ? 1 : 0;
		}
		return count;
	}
}
