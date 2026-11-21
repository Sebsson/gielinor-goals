package com.gielinorgoals.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

public class SkillRequirementTest
{
	@Test
	public void levelGoalIsCompleteExactlyAtLevelXp()
	{
		SkillRequirement requirement = SkillRequirement.level(Skill.AGILITY, 70);
		int xpFor70 = Experience.getXpForLevel(70);

		GoalProgress below = requirement.progress(xp(Skill.AGILITY, xpFor70 - 1));
		assertFalse(below.isComplete());
		assertEquals("69/70", below.getLabel());

		GoalProgress at = requirement.progress(xp(Skill.AGILITY, xpFor70));
		assertTrue(at.isComplete());
		assertEquals("70/70", at.getLabel());
		assertEquals(1.0, at.fraction(), 0.0);
	}

	@Test
	public void virtualLevelsUseXpBeyond99()
	{
		SkillRequirement requirement = SkillRequirement.level(Skill.FISHING, 105);

		assertFalse(requirement.progress(xp(Skill.FISHING, Experience.getXpForLevel(99))).isComplete());
		assertTrue(requirement.progress(xp(Skill.FISHING, Experience.getXpForLevel(105))).isComplete());
	}

	@Test
	public void xpGoalComparesRawXp()
	{
		SkillRequirement requirement = SkillRequirement.xp(Skill.MINING, 1_000_000);

		GoalProgress half = requirement.progress(xp(Skill.MINING, 500_000));
		assertFalse(half.isComplete());
		assertEquals(0.5, half.fraction(), 0.0001);
		assertEquals("500K/1M", half.getLabel());

		assertTrue(requirement.progress(xp(Skill.MINING, 1_000_000)).isComplete());
	}

	@Test
	public void unknownXpGivesNoProgress()
	{
		assertNull(SkillRequirement.level(Skill.SLAYER, 50).progress(GameData.EMPTY));
	}

	@Test
	public void describeGivesReadableTitle()
	{
		assertEquals("70 Agility", SkillRequirement.level(Skill.AGILITY, 70).describe());
		assertEquals("13,034,431 Agility XP", SkillRequirement.xp(Skill.AGILITY, 13_034_431).describe());
	}

	@Test(expected = IllegalArgumentException.class)
	public void levelAboveVirtualMaxIsRejected()
	{
		SkillRequirement.level(Skill.AGILITY, 127);
	}

	@Test(expected = IllegalArgumentException.class)
	public void xpAboveMaxIsRejected()
	{
		SkillRequirement.xp(Skill.AGILITY, 200_000_001L);
	}

	@Test
	public void xpGainCountsOnlyXpAfterStart()
	{
		SkillRequirement requirement = SkillRequirement.xpGain(Skill.MINING, 1_000_000, 500_000);

		GoalProgress halfway = requirement.progress(xp(Skill.MINING, 1_250_000));
		assertEquals(250_000, halfway.getCurrent());
		assertEquals(500_000, halfway.getTarget());
		assertEquals("250K/500K gained", halfway.getLabel());
		assertFalse(halfway.isComplete());

		assertTrue(requirement.progress(xp(Skill.MINING, 1_500_000)).isComplete());
		assertEquals(1_500_000, requirement.targetXp());
	}

	@Test
	public void xpGainNeverShowsNegativeProgress()
	{
		// T.ex. om startpunkten sattes på ett annat konto med mer XP.
		GoalProgress progress = SkillRequirement.xpGain(Skill.MINING, 1_000_000, 500_000)
			.progress(xp(Skill.MINING, 900_000));

		assertEquals(0, progress.getCurrent());
	}

	@Test(expected = IllegalArgumentException.class)
	public void xpGainPastMaxXpIsRejected()
	{
		SkillRequirement.xpGain(Skill.MINING, 199_999_999L, 2);
	}

	@Test
	public void sameGainFromDifferentStartIsNotEqual()
	{
		assertFalse(SkillRequirement.xpGain(Skill.MINING, 0, 500).equals(SkillRequirement.xpGain(Skill.MINING, 10, 500)));
		assertFalse(SkillRequirement.xpGain(Skill.MINING, 0, 500).equals(SkillRequirement.xp(Skill.MINING, 500)));
	}

	@Test
	public void describeProgressShowsExactNumbers()
	{
		SkillRequirement level = SkillRequirement.level(Skill.AGILITY, 70);
		assertEquals("Level 60 / 70\n273,742 / 737,627 XP\n463,885 XP left",
			level.describeProgress(level.progress(xp(Skill.AGILITY, 273_742))));

		SkillRequirement total = SkillRequirement.xp(Skill.AGILITY, 13_034_431);
		assertEquals("1,000,000 / 13,034,431 XP\n12,034,431 XP left",
			total.describeProgress(total.progress(xp(Skill.AGILITY, 1_000_000))));

		SkillRequirement gain = SkillRequirement.xpGain(Skill.MINING, 1_000_000, 500_000);
		assertEquals("Gained 250,000 / 500,000 XP\n250,000 XP left\nStarted at 1,000,000 XP, done at 1,500,000 XP",
			gain.describeProgress(gain.progress(xp(Skill.MINING, 1_250_000))));
	}

	static GameData xp(Skill skill, long xp)
	{
		Map<Skill, Long> map = new EnumMap<>(Skill.class);
		map.put(skill, xp);
		return s -> map.getOrDefault(s, -1L);
	}
}
