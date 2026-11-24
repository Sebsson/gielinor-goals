package com.gielinorgoals.template;

import com.gielinorgoals.model.DropSource;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.SkillRequirement;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.client.hiscore.HiscoreSkill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * Egna templates: export av en delgraf, lagring, gränser och uppdatering.
 */
public class CustomTemplatesTest
{
	private final Gson gson = new Gson();

	@Test
	public void sharedGoalsStaySharedAndOuterLinksAreLeftOut()
	{
		GoalTree tree = new GoalTree();
		Goal root = add(tree, null, manual("Raid prep"));
		Goal a = add(tree, root.getId(), manual("Gear"));
		Goal b = add(tree, root.getId(), manual("Supplies"));
		Goal shared = add(tree, a.getId(), manual("Bank trip"));
		tree.link(b.getId(), shared.getId());
		Goal rope = add(tree, shared.getId(), item(954, "Rope"));
		// Ett mål utanför delgrafen som också har det delade målet som delmål.
		Goal outside = add(tree, null, manual("Other plan"));
		tree.link(outside.getId(), shared.getId());
		Goal outsideItem = add(tree, outside.getId(), item(995, "Coins"));
		rope.setDoneWhen(new ArrayList<>(List.of("item:954", "item:995")));

		TemplateExport.Result export = TemplateExport.export(tree, root, "Raid prep", "", List.of());
		assertEquals(5, export.getNodeCount());
		assertEquals(5, export.getKeys().size());
		assertFalse(export.getKeys().containsKey(outside.getId()));
		assertFalse(export.getKeys().containsKey(outsideItem.getId()));

		GoalTree fresh = new GoalTree();
		Goal inserted = TemplateInserter.insert(fresh, export.preview()).getRoot();
		assertEquals(5, fresh.flatten().size());
		Goal bank = only(fresh, "group:bank trip");
		assertEquals(2, fresh.getParents(bank.getId()).size());
		// doneWhen till ett mål utanför delgrafen följer inte med.
		assertEquals(List.of("item:954"), only(fresh, "item:954").getDoneWhen());
		assertEquals("Raid prep", inserted.getTitle());
	}

	@Test
	public void profileDataIsNotSaved()
	{
		GoalTree tree = new GoalTree();
		Goal root = add(tree, null, manual("Pets"));
		Goal kc = new Goal("");
		kc.setRequirement(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 500));
		tree.add(root.getId(), kc);
		Goal item = item(12921, "Pet snakeling");
		item.setDropSource(DropSource.hiscore(HiscoreSkill.ZULRAH, 4000));
		item.setObtainedAtKc(321L);
		tree.add(root.getId(), item);
		Goal xpGain = new Goal("");
		xpGain.setRequirement(SkillRequirement.xpGain(Skill.MAGIC, 1_000_000, 500_000));
		tree.add(root.getId(), xpGain);
		for (Goal goal : tree.flatten())
		{
			goal.markCompleted(true, 1234L, true);
			goal.setCreatedAt(1000L);
		}

		TemplateExport.Result export = TemplateExport.export(tree, root, "Pets", "", List.of());
		String json = gson.toJson(export.getSpec());
		for (String field : List.of("completed", "completedAt", "createdAt", "obtainedAtKc", "startXp", "1234",
			"1000000", "expanded"))
		{
			assertFalse(field + " in " + json, json.contains(field));
		}
		assertEquals(1, export.getConvertedXpGains());

		GoalTree fresh = new GoalTree();
		TemplateInserter.insert(fresh, export.preview());
		for (Goal goal : fresh.flatten())
		{
			assertFalse(goal.isCompleted());
			assertNull(goal.getObtainedAtKc());
		}
		// KC-målet och drop-källan följer med, men inte räknarvärdet.
		assertEquals(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 500), only(fresh, "kc:hiscore:ZULRAH:500")
			.getRequirement());
		assertEquals(4000, only(fresh, "item:12921").getDropSource().getRate());
		// XP räknat från en start blir ett manuellt mål med samma text.
		assertNull(only(fresh, "group:" + xpGain.getRequirement().describe().toLowerCase(java.util.Locale.ROOT))
			.getRequirement());
	}

	@Test
	public void questChainsAreKeptAndNotExpandedAgain()
	{
		GoalTree tree = new GoalTree();
		Goal root = add(tree, null, manual("Quests"));
		Goal ds = add(tree, root.getId(), quest(Quest.DRAGON_SLAYER_I));
		add(tree, ds.getId(), quest(Quest.RUNE_MYSTERIES));
		Goal leaf = add(tree, root.getId(), quest(Quest.COOKS_ASSISTANT));

		JsonTemplate.FileSpec spec = TemplateExport.export(tree, root, "Quests", "", List.of()).getSpec();
		assertEquals(Boolean.FALSE, spec.expandQuestChains);
		assertEquals(Boolean.FALSE, spec.root.children.get(0).expandChain);
		assertNull(spec.root.children.get(1).expandChain);
		assertFalse(JsonTemplate.from(spec).defaultExpandQuestChains());
		assertNotNull(leaf);
	}

	@Test
	public void savingOverKeepsTheIdAndSavingAsNewGivesANewOne()
	{
		CustomTemplates custom = CustomTemplates.inMemory(gson);
		GoalTree tree = new GoalTree();
		Goal root = add(tree, null, manual("Plan"));
		add(tree, root.getId(), item(954, "Rope"));

		String first = custom.add(TemplateExport.export(tree, root, "Plan", "", List.of()));
		assertTrue(first.startsWith(CustomTemplates.ID_PREFIX));
		assertEquals(TemplateCategories.MINE, custom.find(first).getCategory());

		add(tree, root.getId(), item(995, "Coins"));
		custom.replace(first, TemplateExport.export(tree, root, "Plan v2", "", List.of()));
		assertEquals(1, custom.getTemplates().size());
		assertEquals("Plan v2", custom.find(first).getName());
		assertEquals(3, custom.find(first).build(Map.of()).size());

		String second = custom.add(TemplateExport.export(tree, root, "Plan copy", "", List.of()));
		assertFalse(first.equals(second));
		assertEquals(2, custom.getTemplates().size());

		String duplicate = custom.duplicate(first);
		assertEquals("Plan v2 (copy)", custom.find(duplicate).getName());
		custom.rename(duplicate, "Renamed", "Desc", List.of("tag"));
		assertEquals(List.of("tag"), custom.find(duplicate).getTags());
		custom.delete(duplicate);
		assertNull(custom.find(duplicate));
	}

	@Test
	public void limitsGiveClearErrors()
	{
		CustomTemplates custom = CustomTemplates.inMemory(gson);
		GoalTree tree = new GoalTree();
		Goal root = add(tree, null, manual("Small"));

		expectError(() -> custom.add(TemplateExport.export(tree, root, " ", "", List.of())), "name");
		for (int i = 0; i < CustomTemplates.MAX_TEMPLATES; i++)
		{
			custom.add(TemplateExport.export(tree, root, "Small " + i, "", List.of()));
		}
		expectError(() -> custom.add(TemplateExport.export(tree, root, "One too many", "", List.of())), "Delete one");

		CustomTemplates other = CustomTemplates.inMemory(gson);
		GoalTree big = new GoalTree();
		Goal bigRoot = add(big, null, manual("Big"));
		for (int i = 0; i < CustomTemplates.MAX_NODES; i++)
		{
			add(big, bigRoot.getId(), manual("Step " + i));
		}
		expectError(() -> other.add(TemplateExport.export(big, bigRoot, "Big", "", List.of())), "the most is");

		// Under nodgränsen men stor tillsammans: totala storleken.
		GoalTree wordy = new GoalTree();
		Goal wordyRoot = add(wordy, null, manual("Wordy"));
		String note = "x".repeat(600);
		for (int i = 0; i < 1500; i++)
		{
			Goal step = add(wordy, wordyRoot.getId(), manual("Step " + i));
			step.setNote(note);
		}
		CustomTemplates third = CustomTemplates.inMemory(gson);
		third.add(TemplateExport.export(wordy, wordyRoot, "Wordy", "", List.of()));
		expectError(() -> third.add(TemplateExport.export(wordy, wordyRoot, "Wordy 2", "", List.of())), "space");
		assertEquals(1, third.getTemplates().size());
	}

	@Test
	public void templatesAreSharedBetweenProfilesThroughGlobalStorage()
	{
		String[] stored = {null};
		CustomTemplates.Storage storage = new CustomTemplates.Storage()
		{
			@Override
			public String get()
			{
				return stored[0];
			}

			@Override
			public void set(String value)
			{
				stored[0] = value;
			}
		};
		GoalTree tree = new GoalTree();
		Goal root = add(tree, null, manual("Plan"));
		add(tree, root.getId(), item(954, "Rope"));
		String id = new CustomTemplates(storage, gson).add(TemplateExport.export(tree, root, "Plan", "", List.of()));

		// Inläst på nytt (t.ex. efter omstart eller på ett annat konto) med samma lagring.
		CustomTemplates again = new CustomTemplates(storage, gson);
		assertEquals("Plan", again.find(id).getName());
		assertEquals(2, again.find(id).build(Map.of()).size());
	}

	@Test
	public void brokenStorageIsKeptAsBackup()
	{
		String[] backup = {null};
		CustomTemplates custom = new CustomTemplates(new CustomTemplates.Storage()
		{
			@Override
			public String get()
			{
				return "{not json";
			}

			@Override
			public void set(String value)
			{
			}

			@Override
			public void setBackup(String value)
			{
				backup[0] = value;
			}
		}, gson);
		assertTrue(custom.getTemplates().isEmpty());
		assertEquals("{not json", backup[0]);
	}

	@Test
	public void updateFromAChangedCustomTemplateShowsAdditions()
	{
		CustomTemplates custom = CustomTemplates.inMemory(gson);
		TemplateCatalog catalog = new TemplateCatalog(gson, QuestRequirementsDb.load(gson), CombatAchievementSource.NONE,
			custom);
		TemplateUpdates updates = new TemplateUpdates(catalog, QuestRequirementsDb.load(gson));

		GoalTree source = new GoalTree();
		Goal sourceRoot = add(source, null, manual("Plan"));
		add(source, sourceRoot.getId(), item(954, "Rope"));
		String id = custom.add(TemplateExport.export(source, sourceRoot, "Plan", "", List.of()));

		TemplateNode node = catalog.find(id).build(Map.of());
		node.setTemplateId(id);
		node.setTemplateParams(Map.of(TemplateUpdates.QUEST_CHAINS_PARAM, 0));
		GoalTree tree = new GoalTree();
		Goal root = TemplateInserter.insert(tree, node).getRoot();
		assertFalse(updates.hasUpdate(tree, root, 1));

		add(source, sourceRoot.getId(), item(995, "Coins"));
		custom.replace(id, TemplateExport.export(source, sourceRoot, "Plan", "", List.of()));
		TemplateUpdates.Plan plan = updates.plan(tree, root);
		assertEquals(1, plan.getAdditions().size());
		assertEquals("item:995", plan.getAdditions().get(0).getNode().getKey());
		assertTrue(updates.hasUpdate(tree, root, 2));

		// En borttagen template: inget märke, ingen plan, inget fel.
		custom.delete(id);
		assertNull(catalog.find(id));
		assertNull(updates.plan(tree, root));
		assertFalse(updates.hasUpdate(tree, root, 3));
	}

	@Test
	public void customTemplatesComeFirstInTheirOwnCategory()
	{
		CustomTemplates custom = CustomTemplates.inMemory(gson);
		TemplateCatalog catalog = new TemplateCatalog(gson, QuestRequirementsDb.load(gson), CombatAchievementSource.NONE,
			custom);
		GoalTree tree = new GoalTree();
		Goal root = add(tree, null, manual("Mine"));
		custom.add(TemplateExport.export(tree, root, "Mine", "", List.of()));

		Map<String, List<GoalTemplate>> grouped = TemplateCategories.group(catalog.getTemplates(), "", true);
		assertEquals(TemplateCategories.MINE, grouped.keySet().iterator().next());
		assertEquals("Mine", grouped.get(TemplateCategories.MINE).get(0).getName());
	}

	@Test
	public void refsAndFormatVersionAreValidated()
	{
		expectInvalid("{\"name\":\"T\",\"root\":{\"id\":\"a\",\"title\":\"A\",\"children\":[{\"ref\":\"a\"}]}}", "cycle");
		expectInvalid("{\"name\":\"T\",\"root\":{\"title\":\"A\",\"children\":[{\"ref\":\"b\"},"
			+ "{\"id\":\"b\",\"title\":\"B\"}]}}", "earlier node");
		expectInvalid("{\"name\":\"T\",\"root\":{\"title\":\"A\",\"children\":[{\"id\":\"b\",\"title\":\"B\"},"
			+ "{\"id\":\"b\",\"title\":\"C\"}]}}", "used twice");
		expectInvalid("{\"name\":\"T\",\"root\":{\"title\":\"A\",\"children\":[{\"id\":\"b\",\"title\":\"B\"},"
			+ "{\"ref\":\"b\",\"title\":\"C\"}]}}", "anything else");
		expectInvalid("{\"formatVersion\":99,\"name\":\"T\",\"root\":{\"title\":\"A\"}}", "newer version");

		JsonTemplate ok = JsonTemplate.from(gson.fromJson("{\"formatVersion\":1,\"name\":\"T\",\"root\":{\"title\":\"A\","
			+ "\"children\":[{\"title\":\"B\",\"children\":[{\"id\":\"s\",\"title\":\"S\"}]},{\"title\":\"C\","
			+ "\"children\":[{\"ref\":\"s\"}]}]}}", JsonTemplate.FileSpec.class));
		TemplateNode built = ok.build(Map.of());
		TemplateNode ref = built.getChildren().get(1).getChildren().get(0);
		assertEquals("s", ref.getRef());
		assertEquals("S", ref.getTitle());
		assertEquals("group:s", ref.getKey());
	}

	private void expectInvalid(String json, String message)
	{
		try
		{
			JsonTemplate.from(gson.fromJson(json, JsonTemplate.FileSpec.class));
			fail("Expected an error with " + message);
		}
		catch (IllegalArgumentException e)
		{
			assertTrue(e.getMessage(), e.getMessage().contains(message));
		}
	}

	private static void expectError(Runnable action, String message)
	{
		try
		{
			action.run();
			fail("Expected an error with " + message);
		}
		catch (CustomTemplates.TemplateException e)
		{
			assertTrue(e.getMessage(), e.getMessage().contains(message));
		}
	}

	private static Goal add(GoalTree tree, String parentId, Goal goal)
	{
		tree.add(parentId, goal);
		return goal;
	}

	private static Goal manual(String title)
	{
		return new Goal(title);
	}

	private static Goal item(int id, String name)
	{
		Goal goal = new Goal("");
		goal.setRequirement(new ItemRequirement(id, name, 1, false));
		return goal;
	}

	private static Goal quest(Quest quest)
	{
		Goal goal = new Goal("");
		goal.setRequirement(new QuestRequirement(quest.getId(), quest.getName()));
		return goal;
	}

	private static Goal only(GoalTree tree, String key)
	{
		Goal found = null;
		for (Goal goal : tree.flatten())
		{
			if (key.equals(TemplateKeys.of(goal)))
			{
				assertNull("two goals with " + key, found);
				found = goal;
			}
		}
		assertNotNull("no goal with " + key, found);
		return found;
	}
}
