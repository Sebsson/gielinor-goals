package com.gielinorgoals.template;

import com.gielinorgoals.model.CombatAchievementRequirement;
import com.gielinorgoals.model.CombatLevelRequirement;
import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.SkillRequirement;
import com.gielinorgoals.model.TotalLevelRequirement;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * De genererade templates i milstolpe 11: struktur, valparametrar, asynkront bygge och
 * koppling mot befintliga mål.
 */
public class GeneratedTemplatesTest
{
	private final Gson gson = new Gson();
	private final QuestRequirementsDb db = QuestRequirementsDb.load(gson);
	private final TemplateCatalog catalog = TestCombatAchievements.catalog(gson);
	private final TemplateUpdates updates = new TemplateUpdates(catalog, db);

	// --- Combat achievements ---

	@Test
	public void combatAchievementsHaveOneGoalPerTaskInTheTier()
	{
		GoalTemplate template = catalog.find("combat-achievements");
		assertTrue(template.needsClientData());
		assertEquals(TemplateCategories.BOSSES, template.getCategory());
		assertTrue(template.getTags().containsAll(List.of("ca", "combat achievements", "grandmaster")));

		TemplateNode hard = template.build(Map.of(), Map.of("tier", "Hard"));
		assertEquals("Combat Achievements: Hard", hard.getTitle());
		assertEquals(TestCombatAchievements.PER_TIER, hard.getChildren().size());
		assertEquals(1 + TestCombatAchievements.PER_TIER, hard.size());
		for (TemplateNode task : hard.getChildren())
		{
			CombatAchievementRequirement requirement = (CombatAchievementRequirement) task.getRequirement();
			assertEquals("Hard", requirement.getTierName());
			assertEquals("ca:" + requirement.getTaskId(), task.getKey());
		}
		// Utan val blir det Easy.
		assertEquals("Combat Achievements: Easy", template.build(Map.of(), Map.of()).getTitle());
	}

	@Test
	public void combatAchievementsAskToLogInWithoutData()
	{
		List<TemplateBuilder.Result> results = new ArrayList<>();
		List<Runnable> clientThread = new ArrayList<>();
		TemplateBuilder builder = new TemplateBuilder(clientThread::add, Runnable::run);

		// Inloggad: klart efter klienttråden.
		builder.build(catalog.find("combat-achievements"), Map.of(), Map.of("tier", "Elite"), results::add);
		assertTrue(results.isEmpty());
		clientThread.remove(0).run();
		assertTrue(results.get(0).isReady());
		assertEquals("Combat Achievements: Elite", results.get(0).getNode().getTitle());

		// Utloggad: katalogen ger null.
		builder.build(new CombatAchievementTemplate(CombatAchievementSource.NONE), Map.of(), Map.of(), results::add);
		clientThread.remove(0).run();
		assertFalse(results.get(1).isReady());
		assertEquals(TemplateUnavailableException.LOG_IN, results.get(1).getMessage());

		// Inloggad men enumarna gick inte att läsa: en tom lista.
		builder.build(new CombatAchievementTemplate(List::of), Map.of(), Map.of(), results::add);
		clientThread.remove(0).run();
		assertFalse(results.get(2).isReady());
		assertEquals(TemplateUnavailableException.LOG_IN, results.get(2).getMessage());
	}

	@Test
	public void combatAchievementsLinkExistingTaskGoals()
	{
		GoalTree tree = new GoalTree();
		Goal existing = new Goal("");
		existing.setRequirement(new CombatAchievementRequirement(0, "Easy task 1", "Easy"));
		tree.add(null, existing);

		TemplateInserter.Result result = TemplateInserter.insert(tree,
			catalog.find("combat-achievements").build(Map.of(), Map.of("tier", "Easy")));
		assertEquals(List.of(existing), result.getLinked());
		assertEquals(TestCombatAchievements.PER_TIER, result.getCreated().size());
	}

	// --- Diaries ---

	@Test
	public void allDiariesInATierHaveOneGoalPerArea()
	{
		GoalTemplate template = catalog.find("diary-tier");
		assertEquals(TemplateCategories.DIARIES, template.getCategory());
		TemplateNode hard = template.build(Map.of(), Map.of("tier", "HARD"));
		assertEquals("All Hard diaries", hard.getTitle());
		assertEquals(DiaryArea.values().length, hard.getChildren().size());
		for (int i = 0; i < DiaryArea.values().length; i++)
		{
			assertEquals(new DiaryRequirement(DiaryArea.values()[i], DiaryTier.HARD),
				hard.getChildren().get(i).getRequirement());
		}
		assertEquals("All Easy diaries", template.build(Map.of(), Map.of()).getTitle());
	}

	@Test
	public void diaryRewardItemIdsMatchGameval()
	{
		Map<DiaryArea, List<Integer>> expected = new HashMap<>();
		expected.put(DiaryArea.ARDOUGNE, List.of(ItemID.ARDY_CAPE_EASY, ItemID.ARDY_CAPE_MEDIUM, ItemID.ARDY_CAPE_HARD,
			ItemID.ARDY_CAPE_ELITE));
		expected.put(DiaryArea.DESERT, List.of(ItemID.DESERT_AMULET_EASY, ItemID.DESERT_AMULET_MEDIUM,
			ItemID.DESERT_AMULET_HARD, ItemID.DESERT_AMULET_ELITE));
		expected.put(DiaryArea.FALADOR, List.of(ItemID.FALADOR_SHIELD_EASY, ItemID.FALADOR_SHIELD_MEDIUM,
			ItemID.FALADOR_SHIELD_HARD, ItemID.FALADOR_SHIELD_ELITE));
		expected.put(DiaryArea.FREMENNIK, List.of(ItemID.FREMENNIK_BOOTS_EASY, ItemID.FREMENNIK_BOOTS_MEDIUM,
			ItemID.FREMENNIK_BOOTS_HARD, ItemID.FREMENNIK_BOOTS_ELITE));
		expected.put(DiaryArea.KANDARIN, List.of(ItemID.SEERS_HEADBAND_EASY, ItemID.SEERS_HEADBAND_MEDIUM,
			ItemID.SEERS_HEADBAND_HARD, ItemID.SEERS_HEADBAND_ELITE));
		expected.put(DiaryArea.KARAMJA, List.of(ItemID.ATJUN_GLOVES_EASY, ItemID.ATJUN_GLOVES_MED,
			ItemID.ATJUN_GLOVES_HARD, ItemID.ATJUN_GLOVES_ELITE));
		expected.put(DiaryArea.KOUREND, List.of(ItemID.ZEAH_BLESSING_EASY, ItemID.ZEAH_BLESSING_MEDIUM,
			ItemID.ZEAH_BLESSING_HARD, ItemID.ZEAH_BLESSING_ELITE));
		expected.put(DiaryArea.LUMBRIDGE, List.of(ItemID.LUMBRIDGE_RING_EASY, ItemID.LUMBRIDGE_RING_MEDIUM,
			ItemID.LUMBRIDGE_RING_HARD, ItemID.LUMBRIDGE_RING_ELITE));
		expected.put(DiaryArea.MORYTANIA, List.of(ItemID.MORYTANIA_LEGS_EASY, ItemID.MORYTANIA_LEGS_MEDIUM,
			ItemID.MORYTANIA_LEGS_HARD, ItemID.MORYTANIA_LEGS_ELITE));
		expected.put(DiaryArea.VARROCK, List.of(ItemID.VARROCK_ARMOUR_EASY, ItemID.VARROCK_ARMOUR_MEDIUM,
			ItemID.VARROCK_ARMOUR_HARD, ItemID.VARROCK_ARMOUR_ELITE));
		expected.put(DiaryArea.WESTERN, List.of(ItemID.WESTERN_BANNER_EASY, ItemID.WESTERN_BANNER_MEDIUM,
			ItemID.WESTERN_BANNER_HARD, ItemID.WESTERN_BANNER_ELITE));
		expected.put(DiaryArea.WILDERNESS, List.of(ItemID.WILDERNESS_SWORD_EASY, ItemID.WILDERNESS_SWORD_MEDIUM,
			ItemID.WILDERNESS_SWORD_HARD, ItemID.WILDERNESS_SWORD_ELITE));

		DiaryRewards rewards = DiaryRewards.load(gson);
		assertEquals(DiaryArea.values().length, rewards.getRewards().size());
		for (DiaryArea area : DiaryArea.values())
		{
			assertNotNull(area.name(), rewards.get(area));
			assertEquals(area.name(), expected.get(area), rewards.get(area).getItems());
		}
	}

	@Test
	public void diaryRewardsAreAChainFromTierFourDown()
	{
		GoalTemplate template = catalog.find("diary-rewards");
		TemplateNode root = template.build(Map.of(), Map.of("area", "VARROCK"));
		assertEquals("Varrock armour 1-4", root.getTitle());
		// Roten, 4 föremål och 4 diaries.
		assertEquals(9, root.size());

		TemplateNode level = root.getChildren().get(0);
		int[] items = {ItemID.VARROCK_ARMOUR_ELITE, ItemID.VARROCK_ARMOUR_HARD, ItemID.VARROCK_ARMOUR_MEDIUM,
			ItemID.VARROCK_ARMOUR_EASY};
		DiaryTier[] tiers = {DiaryTier.ELITE, DiaryTier.HARD, DiaryTier.MEDIUM, DiaryTier.EASY};
		String above = null;
		for (int i = 0; i < items.length; i++)
		{
			ItemRequirement item = (ItemRequirement) level.getRequirement();
			assertEquals(items[i], item.getItemId());
			assertTrue(item.isIncludeVariants());
			assertEquals("Varrock armour " + (4 - i), item.getItemName());
			assertEquals(new DiaryRequirement(DiaryArea.VARROCK, tiers[i]), level.getChildren().get(0).getRequirement());
			// Den lägre nivån är klar när man har nästa.
			assertEquals(above == null ? null : List.of(above), level.getDoneWhen());
			above = level.getKey();
			level = i < items.length - 1 ? level.getChildren().get(1) : null;
		}
	}

	@Test
	public void diaryRewardsLinkTheArdougneCloakTemplate()
	{
		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, catalog.find("ardougne-cloak").build(Map.of()));

		TemplateInserter.Result result = TemplateInserter.insert(tree,
			catalog.find("diary-rewards").build(Map.of(), Map.of("area", "ARDOUGNE")));
		// Cloak 4 finns redan och kopplas in; inga dubbletter av cloaks eller diaries.
		assertEquals(1, result.getLinked().size());
		for (int id : new int[]{ItemID.ARDY_CAPE_EASY, ItemID.ARDY_CAPE_MEDIUM, ItemID.ARDY_CAPE_HARD,
			ItemID.ARDY_CAPE_ELITE})
		{
			assertEquals(1, count(tree, "item:" + id));
		}
		assertEquals(1, count(tree, "diary:ARDOUGNE:ELITE"));
	}

	// --- Skills ---

	@Test
	public void skillLadderIsAChainFrom99Down()
	{
		GoalTemplate template = catalog.find("skill-ladder");
		TemplateNode root = template.build(Map.of(), Map.of("skill", "MINING"));
		assertEquals("Mining 60-99", root.getTitle());
		assertEquals(6, root.size());
		TemplateNode step = root.getChildren().get(0);
		for (int level : new int[]{99, 90, 80, 70, 60})
		{
			assertEquals(SkillRequirement.level(Skill.MINING, level), step.getRequirement());
			step = step.getChildren().isEmpty() ? null : step.getChildren().get(0);
		}
		assertNull(step);
		assertEquals("Attack 60-99", template.build(Map.of(), Map.of()).getTitle());
	}

	@Test
	public void skillLadderLinksAnExistingGoal()
	{
		GoalTree tree = new GoalTree();
		Goal attack70 = new Goal("");
		attack70.setRequirement(SkillRequirement.level(Skill.ATTACK, 70));
		tree.add(null, attack70);

		TemplateInserter.Result result = TemplateInserter.insert(tree,
			catalog.find("skill-ladder").build(Map.of(), Map.of("skill", "ATTACK")));
		assertEquals(List.of(attack70), result.getLinked());
		assertEquals(1, count(tree, "skill:ATTACK:70"));
		Goal attack80 = find(tree, "skill:ATTACK:80");
		assertTrue(attack80.getChildIds().contains(attack70.getId()));
		// Det befintliga målet är inte längre ett eget rotmål.
		assertFalse(tree.getRootIds().contains(attack70.getId()));
	}

	@Test
	public void skillLadderKeepsItsSkillOnUpdate()
	{
		GoalTemplate template = catalog.find("skill-ladder");
		TemplateNode node = template.build(Map.of(), Map.of("skill", "FISHING"));
		node.setTemplateId(template.getId());
		node.setTemplateParams(Map.of());
		node.setTemplateChoices(Map.of("skill", "FISHING"));
		GoalTree tree = new GoalTree();
		Goal root = TemplateInserter.insert(tree, node).getRoot();

		assertEquals(Map.of("skill", "FISHING"), root.getTemplateChoices());
		assertEquals("Fishing 60-99", updates.rebuild(root).getTitle());
		assertTrue(updates.plan(tree, root).isEmpty());
	}

	@Test
	public void totalAndCombatLevelAreSingleGoals()
	{
		GoalTemplate total = catalog.find("total-level");
		GoalTemplate combat = catalog.find("combat-level");
		assertEquals(500, total.getParameters().get(0).getMin());
		assertEquals(TotalLevelRequirement.MAX, total.getParameters().get(0).getMax());
		assertEquals(3, combat.getParameters().get(0).getMin());
		assertEquals(126, combat.getParameters().get(0).getMax());

		TemplateNode node = total.build(Map.of("level", 1750));
		assertEquals(1, node.size());
		assertEquals(new TotalLevelRequirement(1750), node.getRequirement());
		assertEquals(new CombatLevelRequirement(126), combat.build(Map.of("level", 200)).getRequirement());
		assertEquals(new CombatLevelRequirement(100), combat.build(Map.of()).getRequirement());
	}

	@Test
	public void totalLevelLinksAnExistingGoalInsteadOfADuplicate()
	{
		GoalTree tree = new GoalTree();
		Goal existing = new Goal("");
		existing.setRequirement(new TotalLevelRequirement(1500));
		tree.add(null, existing);

		TemplateNode node = catalog.find("total-level").build(Map.of("level", 1500));
		TemplatePlanner.Plan plan = TemplatePlanner.plan(tree, node, com.gielinorgoals.model.GameData.EMPTY);
		assertEquals(TemplatePlanner.Status.LINKED, plan.statusOf(node));

		TemplateInserter.Result result = TemplateInserter.insert(tree, node);
		assertSame(existing, result.getRoot());
		assertTrue(result.getCreated().isEmpty());
		assertEquals(1, tree.flatten().size());
	}

	@Test
	public void maxCapeIsBase99WithItsOwnRoot()
	{
		TemplateNode max = catalog.find("max-cape").build(Map.of());
		TemplateNode base = catalog.find("base-level").build(Map.of("level", 99));
		assertEquals("Max cape", max.getTitle());
		assertEquals(ItemID.SKILLCAPE_MAX, (int) max.getIcon().getItemId());
		assertEquals(keys(base), keys(max));
		assertEquals(TemplateCategories.CAPES, catalog.find("max-cape").getCategory());

		// Efter en Base 99 kopplas alla skills in i stället för att dubbleras.
		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, base);
		TemplateInserter.Result result = TemplateInserter.insert(tree, max);
		assertEquals(base.getChildren().size(), result.getLinked().size());
		assertEquals(1, result.getCreated().size());
	}

	// --- Quests ---

	@Test
	public void questSeriesListsTheSeriesInOrderWithChains()
	{
		GoalTemplate template = catalog.find("quest-series");
		assertEquals(TemplateCategories.QUESTS, template.getCategory());
		assertTrue(template.defaultExpandQuestChains());
		assertEquals(db.getSeries().size(), template.getParameters().get(0).getOptions().size());

		TemplateNode elf = template.build(Map.of(), Map.of("series", "Elf"));
		assertEquals("Elf quest series", elf.getTitle());
		List<String> names = new ArrayList<>();
		for (TemplateNode quest : elf.getChildren())
		{
			assertTrue(quest.isExpandChain());
			names.add(((QuestRequirement) quest.getRequirement()).getQuestName());
		}
		List<String> expected = new ArrayList<>();
		db.series("Elf").getQuests().forEach(quest -> expected.add(quest.getName()));
		assertEquals(expected, names);

		// Med kedjor (utfällda på plats) får seriens quests sina förkrav som delmål.
		int before = elf.size();
		QuestChains.expand(elf, db);
		assertTrue(elf.size() > before);
	}

	// --- Hjälpare ---

	private static List<String> keys(TemplateNode root)
	{
		List<String> keys = new ArrayList<>();
		root.getChildren().forEach(child -> keys.add(child.getKey()));
		return keys;
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

	private static Goal find(GoalTree tree, String key)
	{
		for (Goal goal : tree.flatten())
		{
			if (key.equals(TemplateKeys.of(goal)))
			{
				return goal;
			}
		}
		throw new AssertionError("No goal " + key);
	}
}
