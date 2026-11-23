package com.gielinorgoals;

import com.gielinorgoals.model.ActivityCounters;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.tracking.InitialSync;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.hiscore.HiscoreSkill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class GoalManagerKillCountTest
{
	private static final String PROFILE = "profile";
	private static final String ZULRAH = "hiscore:ZULRAH";

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
	public void chatCountCompletesGoalWithPopup()
	{
		Goal goal = addGoal(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 100));

		manager.recordChatCount(PROFILE, ZULRAH, 99);
		assertFalse(goal.isCompleted());
		assertEquals("99/100", manager.getProgress(goal.getId()).getLabel());

		manager.recordChatCount(PROFILE, ZULRAH, 100);
		assertTrue(goal.isCompleted());
		assertFalse(goal.isCompletedAtApprox());
		assertEquals(List.of(List.of(goal)), announced);
	}

	@Test
	public void twoGoalsOnSameBossShareTheCounter()
	{
		Goal first = addGoal(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 50));
		Goal second = addGoal(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 200));

		manager.recordChatCount(PROFILE, ZULRAH, 120);

		assertTrue(first.isCompleted());
		assertEquals("120/200", manager.getProgress(second.getId()).getLabel());
		assertEquals(1, manager.getCounters().asMap().size());
	}

	@Test
	public void hiscoresFillUnknownButNeverLowerAChatValue()
	{
		Goal goal = addGoal(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 500));

		manager.recordHiscores(PROFILE, Map.of(ZULRAH, 300L, "hiscore:VORKATH", 40L));
		assertEquals("300/500", manager.getProgress(goal.getId()).getLabel());

		manager.recordChatCount(PROFILE, ZULRAH, 320);
		manager.recordHiscores(PROFILE, Map.of(ZULRAH, 310L));
		assertEquals("320/500", manager.getProgress(goal.getId()).getLabel());
		assertEquals(ActivityCounters.Source.CHAT, manager.getCounters().get(ZULRAH).getSource());
	}

	@Test
	public void npcLootCountsOnlyWatchedNpcsIgnoringCase()
	{
		Goal goal = addGoal(KillCountRequirement.npc("Lizardman shaman", 2));

		manager.recordNpcKill(PROFILE, "Goblin");
		manager.recordNpcKill(PROFILE, "LIZARDMAN SHAMAN");
		assertEquals("1/2", manager.getProgress(goal.getId()).getLabel());
		manager.recordNpcKill(PROFILE, "Lizardman shaman");

		assertTrue(goal.isCompleted());
		// Ingen räknare skapas för NPC:er utan mål.
		assertEquals(-1, manager.getCounters().value("npc:goblin"));
	}

	@Test
	public void eventsForAnotherProfileAreIgnored()
	{
		Goal goal = addGoal(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 1));

		manager.recordChatCount("other", ZULRAH, 5);

		assertFalse(goal.isCompleted());
		assertEquals(-1, manager.getCounters().value(ZULRAH));
	}

	@Test
	public void countsInFirstSyncAreQuietAndApprox()
	{
		Goal goal = addGoal(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 10));
		initialSync.start();

		manager.recordHiscores(PROFILE, Map.of(ZULRAH, 50L));

		assertTrue(goal.isCompleted());
		assertTrue(goal.isCompletedAtApprox());
	}

	@Test
	public void manualCounterWorksWithoutGameDataAndIsSaved()
	{
		Goal goal = addGoal(KillCountRequirement.manual("Birdhouse runs", 3));
		String key = ((KillCountRequirement) goal.getRequirement()).counterKey();

		manager.adjustCounter(key, 1);
		manager.adjustCounter(key, 1);
		manager.setCounter(key, 2);
		assertFalse(goal.isCompleted());
		manager.adjustCounter(key, 1);
		assertTrue(goal.isCompleted());

		manager.loadProfile(PROFILE);
		assertEquals(3, manager.getCounters().value(key));
		assertEquals(ActivityCounters.Source.MANUAL, manager.getCounters().get(key).getSource());
	}

	private Goal addGoal(KillCountRequirement requirement)
	{
		manager.addGoal(null, "", null, requirement);
		List<Goal> roots = manager.getTree().getRoots();
		return roots.get(roots.size() - 1);
	}
}
