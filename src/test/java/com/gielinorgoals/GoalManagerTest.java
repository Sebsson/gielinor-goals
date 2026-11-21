package com.gielinorgoals;

import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.SkillRequirement;
import com.gielinorgoals.tracking.InitialSync;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class GoalManagerTest
{
	private static final String PROFILE = "profile";

	private long now = 1_000_000L;
	private final Map<String, String> stored = new HashMap<>();
	private final List<List<Goal>> announced = new ArrayList<>();
	private InitialSync initialSync;
	private GoalManager manager;

	@Before
	public void setUp()
	{
		initialSync = new InitialSync(() -> now);
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
			}
		}, new Gson(), initialSync);
		manager.setCompletionListener(announced::add);
		manager.loadProfile(PROFILE);
	}

	@Test
	public void goalMetInFirstSyncAfterLoginGetsApproxDate()
	{
		Goal goal = addCookingGoal();

		initialSync.start();
		manager.updateGameData(PROFILE, cookingXp(1_000_000));

		assertTrue(goal.isCompleted());
		assertTrue(goal.isCompletedAtApprox());
	}

	@Test
	public void goalMetAfterFirstSyncGetsExactDate()
	{
		Goal goal = addCookingGoal();
		manager.updateGameData(PROFILE, cookingXp(0));

		initialSync.start();
		now += InitialSync.WINDOW_MS;
		manager.updateGameData(PROFILE, cookingXp(1_000_000));

		assertTrue(goal.isCompleted());
		assertFalse(goal.isCompletedAtApprox());
		assertEquals(1, announced.size());
	}

	@Test
	public void goalAlreadyMetWhenAddedGetsApproxDate()
	{
		manager.updateGameData(PROFILE, cookingXp(1_000_000));

		Goal goal = addCookingGoal();

		assertTrue(goal.isCompleted());
		assertTrue(goal.isCompletedAtApprox());
		assertTrue(announced.isEmpty());
	}

	@Test
	public void goalMetWhileOfflineIsApproxAfterProfileLoad()
	{
		addCookingGoal();
		manager.updateGameData(PROFILE, cookingXp(1));
		manager.unload();

		// Samma profil laddas igen när datan redan visar att målet är nått.
		manager.updateGameData(PROFILE, cookingXp(1_000_000));
		manager.loadProfile(PROFILE);

		Goal reloaded = manager.getTree().getRoots().get(0);
		assertTrue(reloaded.isCompleted());
		assertTrue(reloaded.isCompletedAtApprox());
	}

	@Test
	public void manualTickIsExactAndUntickResetsDate()
	{
		manager.addGoal(null, "Fire cape", null, null);
		Goal goal = manager.getTree().getRoots().get(0);

		manager.setCompleted(goal.getId(), true);
		assertFalse(goal.isCompletedAtApprox());
		assertNotNull(goal.getCompletedAt());

		goal.setCompletedAtApprox(true);
		manager.setCompleted(goal.getId(), false);
		assertNull(goal.getCompletedAt());
		assertFalse(goal.isCompletedAtApprox());
	}

	@Test
	public void newGoalsGetCreatedAtAndSurviveReload()
	{
		manager.addGoal(null, "Fire cape", null, null);
		Long createdAt = manager.getTree().getRoots().get(0).getCreatedAt();

		manager.loadProfile(PROFILE);

		assertEquals(createdAt, manager.getTree().getRoots().get(0).getCreatedAt());
	}

	@Test
	public void revealExpandsCollapsedAncestors()
	{
		manager.addGoal(null, "Parent", null, null);
		Goal parent = manager.getTree().getRoots().get(0);
		manager.addGoal(parent.getId(), "Child", null, null);
		Goal child = manager.getTree().getChildren(parent).get(0);
		manager.toggleExpanded(parent.getId());
		assertFalse(parent.isExpanded());

		assertTrue(manager.reveal(child.getId()));

		assertTrue(parent.isExpanded());
		assertFalse(manager.reveal("missing"));
	}

	private Goal addCookingGoal()
	{
		manager.addGoal(null, "", null, SkillRequirement.level(Skill.COOKING, 70));
		return manager.getTree().getRoots().get(manager.getTree().getRoots().size() - 1);
	}

	private static GameData cookingXp(long xp)
	{
		return skill -> skill == Skill.COOKING ? xp : 0;
	}

	@Test
	public void showAsCounterCollapsesAndIsSaved()
	{
		manager.addGoal(null, "Quest cape", null, null);
		Goal cape = manager.getTree().getRoots().get(0);
		assertTrue(cape.isExpanded());

		manager.setAggregate(cape.getId(), true);

		assertTrue(cape.isAggregate());
		assertFalse(cape.isExpanded());
		manager.loadProfile(PROFILE);
		assertTrue(manager.getTree().getRoots().get(0).isAggregate());
	}

	@Test
	public void tickingLastSubGoalCompletesCounterWithPopupAndExactDate()
	{
		manager.addGoal(null, "Quest cape", null, null, true);
		Goal cape = manager.getTree().getRoots().get(0);
		manager.addGoal(cape.getId(), "Quest 1", null, null);
		manager.addGoal(cape.getId(), "Quest 2", null, null);
		List<Goal> quests = manager.getTree().getChildren(cape);

		manager.setCompleted(quests.get(0).getId(), true);
		assertFalse(cape.isCompleted());
		assertEquals("1/2", manager.getProgress(cape.getId()).getLabel());

		// Utan speldata, eftersom räknaren bara beror på delmålen.
		manager.setCompleted(quests.get(1).getId(), true);

		assertTrue(cape.isCompleted());
		assertFalse(cape.isCompletedAtApprox());
		assertEquals(List.of(List.of(cape)), announced);
	}

	@Test
	public void linkAndUnlinkAreSavedAndCyclesRefused()
	{
		manager.addGoal(null, "Barrows gloves", null, null);
		manager.addGoal(null, "Quest cape", null, null);
		Goal gloves = manager.getTree().getRoots().get(0);
		Goal cape = manager.getTree().getRoots().get(1);
		manager.addGoal(gloves.getId(), "Recipe for Disaster", null, null);
		Goal rfd = manager.getTree().getChildren(gloves).get(0);

		manager.link(cape.getId(), rfd.getId());
		manager.link(rfd.getId(), gloves.getId());
		manager.loadProfile(PROFILE);

		assertEquals(2, manager.getTree().getParents(rfd.getId()).size());
		assertTrue(manager.getTree().getChildren(manager.getTree().find(rfd.getId())).isEmpty());

		manager.unlink(gloves.getId(), rfd.getId());
		manager.loadProfile(PROFILE);
		assertEquals(List.of(cape.getId()),
			List.of(manager.getTree().getParents(rfd.getId()).get(0).getId()));
	}
}
