package com.gielinorgoals;

import com.gielinorgoals.template.QuestRequirementsDb;
import com.gielinorgoals.template.TemplateUpdates;
import com.gielinorgoals.template.TemplateCatalog;
import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.template.QuestCapeExclusions;
import com.gielinorgoals.template.QuestCapeTemplate;
import com.gielinorgoals.template.TemplateInserter;
import com.gielinorgoals.template.TemplateNode;
import com.gielinorgoals.tracking.InitialSync;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class GoalManagerTemplateTest
{
	private static final String PROFILE = "profile";

	private final List<List<Goal>> announced = new ArrayList<>();
	private final Map<String, String> stored = new java.util.HashMap<>();
	private int goalSaves;
	private int rebuilds;
	private GoalManager manager;

	@Before
	public void setUp()
	{
		manager = new GoalManager(new GoalManager.Storage()
		{
			@Override
			public String get(String profileKey, String key)
			{
				return stored.get(profileKey + "." + key);
			}

			@Override
			public void set(String profileKey, String key, String value)
			{
				stored.put(profileKey + "." + key, value);
				if (key.equals("goals"))
				{
					goalSaves++;
				}
			}
		}, new Gson(), new InitialSync(() -> 0L));
		manager.setCompletionListener(announced::add);
		manager.setListener(() -> rebuilds++);
		manager.loadProfile(PROFILE);
	}

	@Test
	public void finishedQuestsAreCompletedQuietlyWithApproxDates()
	{
		TemplateNode cape = new QuestCapeTemplate(QuestCapeExclusions.load(new Gson())).build(Map.of());
		Set<Integer> finished = new HashSet<>();
		// Alla quests utom de tre första är klara, som för en spelare med många quests.
		for (int i = 3; i < cape.getChildren().size(); i++)
		{
			finished.add(questId(cape.getChildren().get(i).getRequirement()));
		}
		manager.updateGameData(PROFILE, quests(finished));
		goalSaves = 0;
		rebuilds = 0;

		TemplateInserter.Result result = manager.insertTemplate(cape);

		assertEquals(finished.size(), result.countCompleted());
		for (Goal goal : result.getCreated())
		{
			if (goal.getRequirement() != null && finished.contains(questId(goal.getRequirement())))
			{
				assertTrue(goal.isCompleted());
				assertTrue(goal.isCompletedAtApprox());
			}
		}
		assertFalse(result.getRoot().isCompleted());
		// Ingen popup eller chattmeddelande, och allt i en sparning och en ombyggnad.
		assertTrue(announced.isEmpty());
		assertEquals(1, goalSaves);
		assertEquals(1, rebuilds);
	}

	@Test
	public void templateThatIsAlreadyDoneCompletesQuietly()
	{
		TemplateNode cape = TemplateNode.aggregate("Two quests")
			.add(TemplateNode.requirement(new QuestRequirement(17, "Cook's Assistant")))
			.add(TemplateNode.requirement(new QuestRequirement(25, "Demon Slayer")));
		manager.updateGameData(PROFILE, quests(Set.of(17, 25)));

		TemplateInserter.Result result = manager.insertTemplate(cape);

		// Även räknarmålet blir klart, tyst och med ungefärligt datum.
		assertTrue(result.getRoot().isCompleted());
		assertTrue(result.getRoot().isCompletedAtApprox());
		assertTrue(announced.isEmpty());
	}

	@Test
	public void questCompletedAfterInsertGivesNormalPopup()
	{
		TemplateNode cape = TemplateNode.aggregate("Two quests")
			.add(TemplateNode.requirement(new QuestRequirement(17, "Cook's Assistant")))
			.add(TemplateNode.requirement(new QuestRequirement(25, "Demon Slayer")));
		manager.updateGameData(PROFILE, quests(Set.of(17)));
		manager.insertTemplate(cape);

		manager.updateGameData(PROFILE, quests(Set.of(17, 25)));

		// Demon Slayer och kappan klarades nu: en vanlig batch med exakt datum.
		assertEquals(1, announced.size());
		assertEquals(2, announced.get(0).size());
		assertFalse(announced.get(0).get(0).isCompletedAtApprox());
	}

	@Test
	public void goalsReadForTheFirstTimeAfterInsertCompleteQuietly()
	{
		// Diaries läses först när de bevakas, alltså ticket efter insättningen.
		TemplateNode easy = new com.gielinorgoals.template.DiaryTierTemplate().build(Map.of(), Map.of());
		Goal root = manager.insertTemplate(easy).getRoot();
		assertFalse(root.isCompleted());

		Map<Integer, Integer> varbits = new java.util.HashMap<>();
		for (DiaryArea area : DiaryArea.values())
		{
			varbits.put(area.varbitFor(DiaryTier.EASY), area.completeValueFor(DiaryTier.EASY));
		}
		manager.updateGameData(PROFILE, varbits(varbits));

		// Alla var klara redan innan: tyst, med ungefärligt datum, även för roten.
		assertTrue(announced.isEmpty());
		assertTrue(root.isCompleted());
		assertTrue(root.isCompletedAtApprox());
		for (Goal child : manager.getTree().getChildren(root))
		{
			assertTrue(child.isCompletedAtApprox());
		}
	}

	@Test
	public void goalWithKnownDataThatCompletesLaterIsAnnounced()
	{
		manager.insertTemplate(new com.gielinorgoals.template.DiaryTierTemplate().build(Map.of(), Map.of()));
		Map<Integer, Integer> varbits = new java.util.HashMap<>();
		for (DiaryArea area : DiaryArea.values())
		{
			varbits.put(area.varbitFor(DiaryTier.EASY), 0);
		}
		// Varrock var redan klar vid första läsningen, de andra inte.
		varbits.put(DiaryArea.VARROCK.varbitFor(DiaryTier.EASY), 1);
		manager.updateGameData(PROFILE, varbits(varbits));
		assertTrue(announced.isEmpty());

		varbits.put(DiaryArea.FALADOR.varbitFor(DiaryTier.EASY), 1);
		manager.updateGameData(PROFILE, varbits(varbits));

		// Falador klarades nu: en vanlig popup med exakt datum.
		assertEquals(1, announced.size());
		assertEquals(1, announced.get(0).size());
		assertEquals(new DiaryRequirement(DiaryArea.FALADOR, DiaryTier.EASY), announced.get(0).get(0).getRequirement());
		assertFalse(announced.get(0).get(0).isCompletedAtApprox());
	}

	private static GameData varbits(Map<Integer, Integer> values)
	{
		Map<Integer, Integer> copy = new java.util.HashMap<>(values);
		return new GameData()
		{
			@Override
			public long getXp(Skill skill)
			{
				return -1;
			}

			@Override
			public int getVarbit(int varbitId)
			{
				return copy.getOrDefault(varbitId, -1);
			}
		};
	}

	@Test
	public void savedGoalIsLinkedToItsCustomTemplate()
	{
		Gson gson = new Gson();
		com.gielinorgoals.template.CustomTemplates custom = com.gielinorgoals.template.CustomTemplates.inMemory(gson);
		TemplateCatalog catalog = new TemplateCatalog(gson, QuestRequirementsDb.load(gson),
			com.gielinorgoals.template.CombatAchievementSource.NONE, custom);
		TemplateUpdates updates = new TemplateUpdates(catalog, QuestRequirementsDb.load(gson));
		manager.addGoal(null, "Plan", null, null);
		Goal root = manager.getTree().getRoots().get(0);
		manager.addGoal(root.getId(), "Buy rope", null, null);

		com.gielinorgoals.template.TemplateExport.Result export = com.gielinorgoals.template.TemplateExport.export(
			manager.getTree(), root, "Plan", "", List.of());
		String id = custom.add(export);
		manager.adoptTemplate(root.getId(), id, export.getKeys());

		assertEquals(id, root.getTemplateId());
		Goal rope = manager.getTree().getChildren(root).get(0);
		assertEquals("group:buy rope", rope.getTemplateNodeKey());
		// Direkt efter sparningen finns inget att uppdatera.
		assertFalse(updates.hasUpdate(manager.getTree(), root, manager.getRevision()));

		// Spara över: samma id; ett nytt delmål i templaten syns som en uppdatering.
		manager.addGoal(root.getId(), "Buy coins", null, null);
		com.gielinorgoals.template.TemplateExport.Result again = com.gielinorgoals.template.TemplateExport.export(
			manager.getTree(), root, "Plan", "", List.of());
		custom.replace(id, again);
		manager.adoptTemplate(root.getId(), id, again.getKeys());
		assertEquals(id, root.getTemplateId());
		assertEquals(1, custom.getTemplates().size());
		assertFalse(updates.hasUpdate(manager.getTree(), root, manager.getRevision()));

		// Sparad på nytt efter att delmålet togs bort: uppdateringen föreslår det igen.
		Goal coins = manager.getTree().getChildren(root).get(1);
		manager.removeGoal(coins.getId());
		assertTrue(updates.hasUpdate(manager.getTree(), root, manager.getRevision()));
	}

	@Test
	public void insertedTemplateSurvivesReload()
	{
		manager.insertTemplate(new QuestCapeTemplate(QuestCapeExclusions.load(new Gson())).build(Map.of()));
		int size = manager.getTree().size();

		manager.loadProfile(PROFILE);

		assertEquals(size, manager.getTree().size());
		assertEquals("Quest cape", manager.getTree().getRoots().get(0).getTitle());
		assertTrue(manager.getTree().getRoots().get(0).isAggregate());
	}

	private static int questId(@Nullable Requirement requirement)
	{
		return ((QuestRequirement) requirement).getQuestId();
	}

	private static GameData quests(Set<Integer> finished)
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
				return finished.contains(questId) ? QuestState.FINISHED : QuestState.NOT_STARTED;
			}
		};
	}

	@Test
	public void updateFromTemplateRestoresDeletedGoalsInOneSave()
	{
		TemplateCatalog catalog = new TemplateCatalog(new Gson());
		TemplateUpdates updates = new TemplateUpdates(catalog, QuestRequirementsDb.load(new Gson()));
		TemplateNode node = catalog.find("base-level").build(Map.of("level", 60));
		node.setTemplateId("base-level");
		node.setTemplateParams(Map.of("level", 60));
		Goal root = manager.insertTemplate(node).getRoot();
		Goal attack = manager.getTree().getChildren(root).get(0);
		manager.removeGoal(attack.getId());
		int size = manager.getTree().size();

		TemplateUpdates.Plan plan = updates.plan(manager.getTree(), root);
		assertEquals(1, plan.getAdditions().size());
		goalSaves = 0;
		rebuilds = 0;
		manager.applyTemplateUpdate(root.getId(), plan.getAdditions(), List.of(), List.of());

		assertEquals(size + 1, manager.getTree().size());
		assertTrue(updates.plan(manager.getTree(), root).isEmpty());
		assertEquals(1, goalSaves);
		assertEquals(1, rebuilds);
		assertTrue(announced.isEmpty());
	}

	@Test
	public void declinedAdditionsAreNotSuggestedAgain()
	{
		TemplateCatalog catalog = new TemplateCatalog(new Gson());
		TemplateUpdates updates = new TemplateUpdates(catalog, QuestRequirementsDb.load(new Gson()));
		TemplateNode node = catalog.find("base-level").build(Map.of());
		node.setTemplateId("base-level");
		node.setTemplateParams(Map.of());
		Goal root = manager.insertTemplate(node).getRoot();
		Goal attack = manager.getTree().getChildren(root).get(0);
		manager.removeGoal(attack.getId());

		TemplateUpdates.Plan plan = updates.plan(manager.getTree(), root);
		manager.applyTemplateUpdate(root.getId(), List.of(), List.of(),
			List.of(plan.getAdditions().get(0).getNode().getKey()));

		assertTrue(updates.plan(manager.getTree(), root).isEmpty());
	}

	@Test
	public void removedNodeSharedWithAnotherGoalIsOnlyUnlinked()
	{
		manager.addGoal(null, "Elsewhere", null, null);
		Goal elsewhere = manager.getTree().getRoots().get(0);
		TemplateCatalog catalog = new TemplateCatalog(new Gson());
		TemplateNode node = catalog.find("base-level").build(Map.of());
		node.setTemplateId("base-level");
		node.setTemplateParams(Map.of());
		Goal root = manager.insertTemplate(node).getRoot();
		Goal attack = manager.getTree().getChildren(root).get(0);
		Goal defence = manager.getTree().getChildren(root).get(1);
		manager.link(elsewhere.getId(), attack.getId());

		manager.applyTemplateUpdate(root.getId(), List.of(), List.of(attack.getId(), defence.getId()), List.of());

		// Attack finns kvar under det andra målet; Defence fanns bara i templaten och tas bort.
		assertEquals(List.of(elsewhere), manager.getTree().getParents(attack.getId()));
		assertEquals(null, manager.getTree().find(defence.getId()));
	}
}
