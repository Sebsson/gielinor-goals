package com.gielinorgoals.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.client.hiscore.HiscoreSkill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class KillCountTest
{
	private static final String ZULRAH = "hiscore:ZULRAH";

	@Test
	public void describeGivesReadableTitles()
	{
		assertEquals("150 Zulrah kills", KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 150).describe());
		assertEquals("500 hard clues", KillCountRequirement.hiscore(HiscoreSkill.CLUE_SCROLL_HARD, 500).describe());
		assertEquals("1,000 clues", KillCountRequirement.hiscore(HiscoreSkill.CLUE_SCROLL_ALL, 1000).describe());
		assertEquals("200 Barrows chests", KillCountRequirement.hiscore(HiscoreSkill.BARROWS_CHESTS, 200).describe());
		assertEquals("Rifts closed: 500", KillCountRequirement.hiscore(HiscoreSkill.RIFTS_CLOSED, 500).describe());
		assertEquals("Lizardman shaman: 3,000 kills", KillCountRequirement.npc("Lizardman shaman", 3000).describe());
		assertEquals("Birdhouse runs: 50", KillCountRequirement.manual("Birdhouse runs", 50).describe());
	}

	@Test
	public void counterKeysAreSharedAndCaseInsensitiveForNames()
	{
		assertEquals(ZULRAH, KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 1).counterKey());
		assertEquals("npc:lizardman shaman", KillCountRequirement.npc("Lizardman Shaman", 1).counterKey());
		assertEquals(KillCountRequirement.npc("lizardman shaman", 1).counterKey(),
			KillCountRequirement.counterKey(KillCountRequirement.ActivityType.NPC, "LIZARDMAN SHAMAN"));
		assertEquals("manual:birdhouse runs", KillCountRequirement.manual("Birdhouse runs", 1).counterKey());
	}

	@Test
	public void progressComesFromTheCounterAndUnknownIsUnknown()
	{
		KillCountRequirement goal = KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 150);
		ActivityCounters counters = new ActivityCounters();

		assertNull(goal.progress(new CountedGameData(GameData.EMPTY, counters)));

		counters.applyChat(ZULRAH, 149, 1L);
		GoalProgress progress = goal.progress(new CountedGameData(GameData.EMPTY, counters));
		assertEquals("149/150", progress.getLabel());
		assertFalse(progress.isComplete());
		assertEquals("149 / 150 kc\n1 left", goal.describeProgress(progress));
	}

	@Test
	public void sameGoalIgnoresNameCase()
	{
		assertTrue(KillCountRequirement.npc("Lizardman shaman", 100).isSameGoal(KillCountRequirement.npc("lizardman SHAMAN", 100)));
		assertFalse(KillCountRequirement.npc("Lizardman shaman", 100).isSameGoal(KillCountRequirement.npc("Lizardman shaman", 200)));
		assertFalse(KillCountRequirement.npc("Zulrah", 100).isSameGoal(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 100)));
	}

	@Test
	public void chatAlwaysWinsOverHiscores()
	{
		ActivityCounters counters = new ActivityCounters();

		assertTrue(counters.applyHiscore(ZULRAH, 100, 1L));
		assertEquals(ActivityCounters.Source.HISCORES, counters.get(ZULRAH).getSource());
		// Chatten är absolut, även när den är lägre än hiscores.
		assertTrue(counters.applyChat(ZULRAH, 98, 2L));
		assertEquals(98, counters.value(ZULRAH));
		assertEquals(ActivityCounters.Source.CHAT, counters.get(ZULRAH).getSource());
		assertEquals(2L, counters.get(ZULRAH).getUpdatedAt());
	}

	@Test
	public void lowerOrUnrankedHiscoreIsIgnored()
	{
		ActivityCounters counters = new ActivityCounters();
		counters.applyChat(ZULRAH, 120, 1L);

		assertFalse(counters.applyHiscore(ZULRAH, 110, 2L));
		assertFalse(counters.applyHiscore(ZULRAH, 120, 2L));
		assertFalse(counters.applyHiscore(ZULRAH, -1, 2L));
		assertEquals(120, counters.value(ZULRAH));

		// Högre hiscore-värde (t.ex. kills gjorda på en annan dator) används.
		assertTrue(counters.applyHiscore(ZULRAH, 130, 3L));
		assertEquals(130, counters.value(ZULRAH));
		// Ej rankad betyder okänt, inte 0.
		assertFalse(counters.applyHiscore("hiscore:VORKATH", -1, 3L));
		assertEquals(-1, counters.value("hiscore:VORKATH"));
	}

	@Test
	public void lootCountsUpFromZeroOrFromUserValue()
	{
		ActivityCounters counters = new ActivityCounters();
		counters.addLoot("npc:lizardman shaman", 1L);
		assertEquals(1, counters.value("npc:lizardman shaman"));
		assertEquals(ActivityCounters.Source.LOOT, counters.get("npc:lizardman shaman").getSource());

		counters.setManual("npc:cave kraken", 250, 1L);
		counters.addLoot("npc:cave kraken", 2L);
		assertEquals(251, counters.value("npc:cave kraken"));
	}

	@Test
	public void manualCounterNeverGoesBelowZero()
	{
		ActivityCounters counters = new ActivityCounters();
		counters.adjustManual("manual:runs", 1, 1L);
		counters.adjustManual("manual:runs", 1, 1L);
		counters.adjustManual("manual:runs", -5, 1L);
		assertEquals(0, counters.value("manual:runs"));
	}

	@Test
	public void brokenSavedEntriesAreDropped()
	{
		Map<String, ActivityCounters.Counter> raw = new LinkedHashMap<>();
		raw.put(ZULRAH, new ActivityCounters.Counter(5, ActivityCounters.Source.CHAT, 1L));
		raw.put("hiscore:VORKATH", new ActivityCounters.Counter(5, null, 1L));
		raw.put("npc:x", null);

		ActivityCounters counters = new ActivityCounters(raw);

		assertEquals(1, counters.asMap().size());
	}

	@Test
	public void twoGoalsOnTheSameBossShareOneCounter()
	{
		GoalTree tree = new GoalTree();
		Goal first = new Goal("");
		first.setRequirement(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 100));
		Goal second = new Goal("");
		second.setRequirement(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 500));
		tree.add(null, first);
		tree.add(null, second);
		ActivityCounters counters = new ActivityCounters();
		counters.applyChat(ZULRAH, 120, 1L);

		GoalEvaluator.Result result = GoalEvaluator.evaluate(tree, new CountedGameData(GameData.EMPTY, counters), 10L);

		assertTrue(first.isCompleted());
		assertFalse(second.isCompleted());
		assertEquals("120/500", result.getProgress().get(second.getId()).getLabel());
	}

	@Test
	public void unknownCountNeverChangesCompletion()
	{
		GoalTree tree = new GoalTree();
		Goal open = new Goal("");
		open.setRequirement(KillCountRequirement.npc("Cave kraken", 1));
		Goal done = new Goal("");
		done.setRequirement(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 1));
		done.markCompleted(true, 5L);
		tree.add(null, open);
		tree.add(null, done);

		GoalEvaluator.Result result = GoalEvaluator.evaluate(tree,
			new CountedGameData(GameData.EMPTY, new ActivityCounters(new HashMap<>())), 10L);

		assertFalse(open.isCompleted());
		assertTrue(done.isCompleted());
		assertTrue(result.getNewlyCompleted().isEmpty());
	}

	@Test
	public void invalidRequirementsAreRejected()
	{
		assertTrue(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 1).isValid());
		try
		{
			KillCountRequirement.npc("  ", 10);
			throw new AssertionError("Expected an empty NPC name to be rejected");
		}
		catch (IllegalArgumentException e)
		{
			// Väntat.
		}
		try
		{
			KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 0);
			throw new AssertionError("Expected target 0 to be rejected");
		}
		catch (IllegalArgumentException e)
		{
			// Väntat.
		}
	}
}
