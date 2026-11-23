package com.gielinorgoals.model;

import net.runelite.client.hiscore.HiscoreSkill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DryMeterTest
{
	private static final int TANZANITE_FANG = 12922;

	@Test
	public void chanceFollowsTheFormula()
	{
		// 1 − (1 − 1/5000)^6200 ≈ 1 − e^(−1.24) ≈ 0.7106
		assertEquals(0.7106, DropSource.chanceByNow(6200, 5000), 0.0005);
		// 1/2 två gånger: 1 − 0.25
		assertEquals(0.75, DropSource.chanceByNow(2, 2), 1e-12);
		assertEquals(1.6, DropSource.rateMultiple(8000, 5000), 1e-12);
	}

	@Test
	public void rowAndTooltipTexts()
	{
		Goal goal = itemGoal(DropSource.hiscore(HiscoreSkill.ZULRAH, 5000));

		assertEquals("6,200 kc · 1.2× rate", DryMeter.rowText(goal, 6200));
		assertEquals("Chance to have received it by now: 71%", DryMeter.chanceText(goal, 6200));
		assertEquals("1/5,000 from Zulrah", goal.getDropSource().describe());
	}

	@Test
	public void zeroKillCount()
	{
		Goal goal = itemGoal(DropSource.npc("Lizardman shaman", 5000));

		assertEquals(0.0, DropSource.chanceByNow(0, 5000), 0.0);
		assertEquals("0 kc · 0.0× rate", DryMeter.rowText(goal, 0));
		assertEquals("Chance to have received it by now: 0%", DryMeter.chanceText(goal, 0));
	}

	@Test
	public void enormousKillCountNeverLooksCertain()
	{
		Goal goal = itemGoal(DropSource.hiscore(HiscoreSkill.ZULRAH, 5000));

		double chance = DropSource.chanceByNow(10_000_000, 5000);
		assertTrue(chance > 0.999 && chance <= 1.0);
		assertEquals("Chance to have received it by now: >99%", DryMeter.chanceText(goal, 10_000_000));
		assertEquals("10,000,000 kc · 2000.0× rate", DryMeter.rowText(goal, 10_000_000));
	}

	@Test
	public void tinyChanceIsNotZeroAndGuaranteedDropIsHundred()
	{
		assertEquals("<1%", DryMeter.formatPercent(DropSource.chanceByNow(1, 5000), false));
		Goal always = itemGoal(DropSource.manual("Chest", 1));
		assertEquals("Chance to have received it by now: 100%", DryMeter.chanceText(always, 1));
	}

	@Test
	public void hiddenWhenCompletedOrCountUnknown()
	{
		Goal goal = itemGoal(DropSource.hiscore(HiscoreSkill.ZULRAH, 5000));
		assertNull(DryMeter.rowText(goal, -1));
		assertNull(DryMeter.chanceText(goal, -1));

		goal.markCompleted(true, 1L);
		goal.setObtainedAtKc(1240L);
		assertNull(DryMeter.rowText(goal, 1240));
		assertNull(DryMeter.chanceText(goal, 1240));
		assertEquals("Got it at 1,240 kc", DryMeter.completedText(goal));
	}

	@Test
	public void uncompletingForgetsTheKillCount()
	{
		Goal goal = itemGoal(DropSource.hiscore(HiscoreSkill.ZULRAH, 5000));
		goal.markCompleted(true, 1L);
		goal.setObtainedAtKc(10L);

		goal.markCompleted(false, 0L);

		assertNull(goal.getObtainedAtKc());
		assertNull(DryMeter.completedText(goal));
	}

	@Test
	public void onlyItemGoalsWithValidSourcesCount()
	{
		Goal manual = new Goal("Not an item");
		manual.setDropSource(DropSource.hiscore(HiscoreSkill.ZULRAH, 5000));
		assertNull(DryMeter.sourceOf(manual));
		assertNull(DryMeter.rowText(manual, 100));

		assertNull(DryMeter.sourceOf(itemGoal(null)));
	}

	private static Goal itemGoal(DropSource source)
	{
		Goal goal = new Goal("");
		goal.setRequirement(new ItemRequirement(TANZANITE_FANG, "Tanzanite fang", 1, false));
		goal.setDropSource(source);
		return goal;
	}
}
