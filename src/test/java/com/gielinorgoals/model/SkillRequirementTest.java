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

	static GameData xp(Skill skill, long xp)
	{
		Map<Skill, Long> map = new EnumMap<>(Skill.class);
		map.put(skill, xp);
		return s -> map.getOrDefault(s, -1L);
	}
}
