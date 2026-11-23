package com.gielinorgoals.tracking;

import java.util.HashMap;
import java.util.Map;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.Skill;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class HiscoreFetcherTest
{
	@Test
	public void onlyRankedBossesAndActivitiesBecomeCounters()
	{
		Map<HiscoreSkill, Skill> skills = new HashMap<>();
		skills.put(HiscoreSkill.ZULRAH, new Skill(1000, 312, -1));
		skills.put(HiscoreSkill.CLUE_SCROLL_HARD, new Skill(5000, 80, -1));
		// Ej rankad: okänt, inte 0.
		skills.put(HiscoreSkill.VORKATH, new Skill(-1, -1, -1));
		// Skills och totalen är inga räknare.
		skills.put(HiscoreSkill.ATTACK, new Skill(10, 99, 13_034_431));
		skills.put(HiscoreSkill.OVERALL, new Skill(10, 2000, 100_000_000));

		Map<String, Long> counters = HiscoreFetcher.toCounters(new HiscoreResult("player", skills));

		assertEquals(Map.of("hiscore:ZULRAH", 312L, "hiscore:CLUE_SCROLL_HARD", 80L), counters);
	}
}
