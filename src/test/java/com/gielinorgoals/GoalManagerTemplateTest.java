package com.gielinorgoals;

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
}
