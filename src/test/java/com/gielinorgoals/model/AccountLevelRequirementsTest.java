package com.gielinorgoals.model;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.util.QuantityFormatter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AccountLevelRequirementsTest
{
	private static final int XP_99 = Experience.getXpForLevel(99);

	@Test
	public void newAccountHasMinimumTotalAndCombatLevel()
	{
		GameData data = levels(1, Map.of(Skill.HITPOINTS, 10));

		assertEquals(AccountLevels.MIN_TOTAL_LEVEL, AccountLevels.totalLevel(data));
		// Alla skills på 1 utom Hitpoints på 10.
		assertEquals(AccountLevels.SKILLS.size() - 1 + 10, AccountLevels.totalLevel(data));
		assertEquals(3, AccountLevels.combatLevel(data));
	}

	@Test
	public void maxedAccountHasMaximumTotalAndCombatLevel()
	{
		GameData data = levels(99, Map.of());

		assertEquals(AccountLevels.MAX_TOTAL_LEVEL, AccountLevels.totalLevel(data));
		assertEquals(AccountLevels.SKILLS.size() * 99, AccountLevels.totalLevel(data));
		assertEquals(126, AccountLevels.combatLevel(data));
	}

	@Test
	public void xpAboveLevel99CountsAsRealLevel99()
	{
		Map<Skill, Long> xp = xpFor(1, Map.of(Skill.HITPOINTS, 10));
		xp.put(Skill.MINING, (long) Experience.MAX_SKILL_XP);
		xp.put(Skill.ATTACK, (long) Experience.getXpForLevel(120));

		GameData data = data(xp);

		// Mining 200M och Attack virtuell 120 räknas båda som 99.
		assertEquals(AccountLevels.MIN_TOTAL_LEVEL + 98 + 98, AccountLevels.totalLevel(data));
	}

	@Test
	public void combatLevelUsesTheGameFormulaWithSkillsInTheRightOrder()
	{
		Map<Skill, Integer> overrides = new EnumMap<>(Skill.class);
		overrides.put(Skill.ATTACK, 60);
		overrides.put(Skill.STRENGTH, 70);
		overrides.put(Skill.DEFENCE, 50);
		overrides.put(Skill.HITPOINTS, 65);
		overrides.put(Skill.MAGIC, 55);
		overrides.put(Skill.RANGED, 60);
		overrides.put(Skill.PRAYER, 43);

		// Bas 0,25 × (50 + 65 + 21) = 34, melee 0,325 × 130 = 42,25, alltså 76.
		assertEquals(76, AccountLevels.combatLevel(levels(1, overrides)));
	}

	@Test
	public void unknownSkillGivesUnknownProgress()
	{
		Map<Skill, Long> xp = xpFor(99, Map.of());
		xp.remove(Skill.SAILING);
		xp.remove(Skill.PRAYER);
		GameData data = data(xp);

		assertNull(new TotalLevelRequirement(1500).progress(data));
		assertNull(new CombatLevelRequirement(100).progress(data));
		assertNull(new QuestPointsRequirement(100).progress(data));
		assertNull(new TotalLevelRequirement(1500).progress(GameData.EMPTY));
	}

	@Test
	public void progressIsCurrentOverTarget()
	{
		GameData data = levels(50, Map.of());
		int total = AccountLevels.SKILLS.size() * 50;

		GoalProgress below = new TotalLevelRequirement(total + 1).progress(data);
		assertEquals(total, below.getCurrent());
		assertEquals(total + 1, below.getTarget());
		assertFalse(below.isComplete());
		assertEquals(QuantityFormatter.formatNumber(total) + "/" + QuantityFormatter.formatNumber(total + 1),
			below.getLabel());

		assertTrue(new TotalLevelRequirement(total).progress(data).isComplete());
	}

	@Test
	public void questPointsComeFromGameData()
	{
		GameData data = questPoints(149);

		GoalProgress progress = new QuestPointsRequirement(150).progress(data);
		assertEquals("149/150", progress.getLabel());
		assertFalse(progress.isComplete());
		assertTrue(new QuestPointsRequirement(149).progress(data).isComplete());
		assertEquals("Quest points 149 / 150\n1 quest point left",
			new QuestPointsRequirement(150).describeProgress(progress));
	}

	@Test
	public void unknownDataNeverChangesCompletion()
	{
		GoalTree tree = new GoalTree();
		Goal open = new Goal("");
		open.setRequirement(new CombatLevelRequirement(3));
		Goal done = new Goal("");
		done.setRequirement(new QuestPointsRequirement(300));
		done.markCompleted(true, 5L);
		tree.add(null, open);
		tree.add(null, done);

		GoalEvaluator.Result result = GoalEvaluator.evaluate(tree, GameData.EMPTY, 1000L);

		assertFalse(open.isCompleted());
		assertTrue(done.isCompleted());
		assertEquals(Long.valueOf(5L), done.getCompletedAt());
		assertTrue(result.getNewlyCompleted().isEmpty());
		assertTrue(result.getProgress().isEmpty());
	}

	@Test
	public void describeGivesReadableTitles()
	{
		assertEquals("Total level 1500", new TotalLevelRequirement(1500).describe());
		assertEquals("Combat level 100", new CombatLevelRequirement(100).describe());
		assertEquals("150 quest points", new QuestPointsRequirement(150).describe());
	}

	@Test
	public void boundsAreEnforced()
	{
		assertThrows(() -> new TotalLevelRequirement(AccountLevels.MIN_TOTAL_LEVEL - 1));
		assertThrows(() -> new TotalLevelRequirement(AccountLevels.MAX_TOTAL_LEVEL + 1));
		assertThrows(() -> new CombatLevelRequirement(2));
		assertThrows(() -> new CombatLevelRequirement(127));
		assertThrows(() -> new QuestPointsRequirement(0));
		assertThrows(() -> new QuestPointsRequirement(QuestPointsRequirement.MAX + 1));
		assertEquals(3, CombatLevelRequirement.MIN);
		assertEquals(126, CombatLevelRequirement.MAX);
	}

	private static void assertThrows(Runnable runnable)
	{
		try
		{
			runnable.run();
		}
		catch (IllegalArgumentException e)
		{
			return;
		}
		throw new AssertionError("Expected IllegalArgumentException");
	}

	// Alla riktiga skills på samma nivå, med valfria undantag.
	private static GameData levels(int level, Map<Skill, Integer> overrides)
	{
		return data(xpFor(level, overrides));
	}

	private static Map<Skill, Long> xpFor(int level, Map<Skill, Integer> overrides)
	{
		Map<Skill, Long> xp = new EnumMap<>(Skill.class);
		for (Skill skill : AccountLevels.SKILLS)
		{
			int skillLevel = overrides.getOrDefault(skill, level);
			xp.put(skill, (long) (skillLevel == 99 ? XP_99 : Experience.getXpForLevel(skillLevel)));
		}
		return xp;
	}

	private static GameData data(Map<Skill, Long> xp)
	{
		return skill -> xp.getOrDefault(skill, -1L);
	}

	private static GameData questPoints(int points)
	{
		return new GameData()
		{
			@Override
			public long getXp(Skill skill)
			{
				return -1;
			}

			@Override
			public int getQuestPoints()
			{
				return points;
			}
		};
	}
}
