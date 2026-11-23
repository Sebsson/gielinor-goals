package com.gielinorgoals.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CompletedLogTest
{
	private static final ZoneId ZONE = ZoneId.of("Europe/Stockholm");

	@Test
	public void midnightSeparatesTodayAndYesterdayAcrossMonthEnd()
	{
		// Tisdag 1 december.
		long now = at(2026, 12, 1, 10, 0, 0);

		assertEquals("Today", group(at(2026, 12, 1, 0, 0, 30), now));
		assertEquals("Yesterday", group(at(2026, 11, 30, 23, 59, 30), now));
		// Söndag i förra veckan och förra månaden.
		assertEquals("November 2026", group(at(2026, 11, 29, 23, 59, 0), now));
	}

	@Test
	public void justAfterMidnightYesterdayIsTheDayBefore()
	{
		// Onsdag 4 november, tio sekunder efter midnatt.
		long now = at(2026, 11, 4, 0, 0, 10);

		assertEquals("Today", group(at(2026, 11, 4, 0, 0, 0), now));
		assertEquals("Yesterday", group(at(2026, 11, 3, 23, 59, 50), now));
		assertEquals("This week", group(at(2026, 11, 2, 12, 0, 0), now));
		// Söndagen före måndag hör till förra veckan men samma månad.
		assertEquals("This month", group(at(2026, 11, 1, 12, 0, 0), now));
		assertEquals("October 2026", group(at(2026, 10, 31, 23, 59, 59), now));
	}

	@Test
	public void weekStartingInPreviousMonthIsThisWeek()
	{
		// Torsdag 3 december; veckan började måndag 30 november.
		long now = at(2026, 12, 3, 12, 0, 0);

		assertEquals("This week", group(at(2026, 11, 30, 8, 0, 0), now));
		assertEquals("November 2026", group(at(2026, 11, 29, 8, 0, 0), now));
	}

	@Test
	public void futureDateCountsAsToday()
	{
		long now = at(2026, 12, 1, 10, 0, 0);
		assertEquals("Today", group(at(2026, 12, 2, 10, 0, 0), now));
	}

	@Test
	public void timeToCompleteCountsWholeDays()
	{
		long created = at(2026, 11, 1, 12, 0, 0);

		assertEquals("Took 3 days", CompletedLog.timeToComplete(goal(created, created + 3 * CompletedLog.DAY_MS + 5_000)));
		assertEquals("Took 1 day", CompletedLog.timeToComplete(goal(created, created + CompletedLog.DAY_MS)));
		assertEquals("Took under a day", CompletedLog.timeToComplete(goal(created, created + 2 * 60 * 60 * 1000)));
	}

	@Test
	public void timeToCompleteNeedsBothExactDates()
	{
		long created = at(2026, 11, 1, 12, 0, 0);

		Goal approx = goal(created, created + CompletedLog.DAY_MS);
		approx.markCompleted(true, created + CompletedLog.DAY_MS, true);
		assertNull(CompletedLog.timeToComplete(approx));

		Goal oldGoal = goal(created, created + CompletedLog.DAY_MS);
		oldGoal.setCreatedAt(null);
		assertNull(CompletedLog.timeToComplete(oldGoal));

		Goal open = new Goal("Open");
		assertNull(CompletedLog.timeToComplete(open));
	}

	@Test
	public void uncompletingResetsDateAndApproxFlag()
	{
		Goal goal = new Goal("A");
		goal.markCompleted(true, 1234L, true);
		assertTrue(goal.isCompletedAtApprox());

		goal.markCompleted(false, 5678L);

		assertFalse(goal.isCompleted());
		assertNull(goal.getCompletedAt());
		assertFalse(goal.isCompletedAtApprox());
	}

	@Test
	public void buildListsAllLevelsGroupedAndSorted()
	{
		long now = at(2026, 12, 3, 12, 0, 0);
		GoalTree tree = new GoalTree();
		Goal gloves = new Goal("Barrows gloves");
		Goal cooking = new Goal("");
		cooking.setRequirement(SkillRequirement.level(Skill.COOKING, 70));
		Goal open = new Goal("Not done");
		Goal undated = new Goal("Old goal");
		tree.add(null, gloves);
		tree.add(gloves.getId(), cooking);
		tree.add(gloves.getId(), open);
		tree.add(null, undated);

		gloves.markCompleted(true, at(2026, 10, 5, 12, 0, 0));
		cooking.markCompleted(true, at(2026, 12, 3, 9, 0, 0));
		undated.setCompleted(true);

		List<CompletedLog.Section> newest = CompletedLog.build(tree, CompletedLog.TypeFilter.ALL, false, true, now, ZONE);
		assertEquals(List.of("Today", "October 2026", "Unknown date"), headers(newest));
		CompletedLog.Entry first = newest.get(0).getEntries().get(0);
		assertEquals("70 Cooking", first.getGoal().getDisplayTitle());
		assertEquals("Barrows gloves", first.getParents());
		assertNull(newest.get(1).getEntries().get(0).getParents());

		List<CompletedLog.Section> oldest = CompletedLog.build(tree, CompletedLog.TypeFilter.ALL, false, false, now, ZONE);
		assertEquals(List.of("October 2026", "Today", "Unknown date"), headers(oldest));
	}

	@Test
	public void buildFiltersByTypeAndRootsOnly()
	{
		long now = at(2026, 12, 3, 12, 0, 0);
		GoalTree tree = new GoalTree();
		Goal gloves = new Goal("Barrows gloves");
		Goal cooking = new Goal("");
		cooking.setRequirement(SkillRequirement.level(Skill.COOKING, 70));
		tree.add(null, gloves);
		tree.add(gloves.getId(), cooking);
		gloves.markCompleted(true, now);
		cooking.markCompleted(true, now);

		assertEquals(List.of("70 Cooking"),
			titles(CompletedLog.build(tree, CompletedLog.TypeFilter.SKILL, false, true, now, ZONE)));
		assertEquals(List.of("Barrows gloves"),
			titles(CompletedLog.build(tree, CompletedLog.TypeFilter.MANUAL, false, true, now, ZONE)));
		assertEquals(List.of("Barrows gloves"),
			titles(CompletedLog.build(tree, CompletedLog.TypeFilter.ALL, true, true, now, ZONE)));
		assertTrue(CompletedLog.build(tree, CompletedLog.TypeFilter.QUEST, false, true, now, ZONE).isEmpty());
	}

	@Test
	public void statsCountAllCompletedGoals()
	{
		long now = at(2026, 12, 3, 12, 0, 0);
		GoalTree tree = new GoalTree();
		Goal exact = new Goal("Fire cape");
		exact.setCreatedAt(at(2026, 11, 29, 12, 0, 0));
		exact.markCompleted(true, at(2026, 12, 1, 12, 0, 0));
		Goal approx = new Goal("Old quest");
		approx.setCreatedAt(at(2026, 1, 1, 12, 0, 0));
		approx.markCompleted(true, at(2026, 12, 2, 12, 0, 0), true);
		Goal lastMonth = new Goal("Barrows gloves");
		lastMonth.setCreatedAt(at(2026, 11, 1, 12, 0, 0));
		lastMonth.markCompleted(true, at(2026, 11, 5, 12, 0, 0));
		Goal noCreatedAt = new Goal("Legacy");
		noCreatedAt.setCreatedAt(null);
		noCreatedAt.markCompleted(true, at(2026, 10, 1, 12, 0, 0));
		Goal undated = new Goal("Undated");
		undated.setCompleted(true);
		Goal open = new Goal("Open");
		for (Goal goal : List.of(exact, approx, lastMonth, noCreatedAt, undated, open))
		{
			tree.add(null, goal);
		}

		CompletedLog.Stats stats = CompletedLog.stats(tree, now, ZONE);

		assertEquals(5, stats.getTotal());
		assertEquals(2, stats.getThisMonth());
		assertEquals("Old quest", stats.getLatest().getTitle());
		// Bara exakta mål med createdAt: 2 dagar och 4 dagar.
		assertEquals(Long.valueOf(3 * CompletedLog.DAY_MS), stats.getAverageMs());
	}

	@Test
	public void statsWithoutDurationsHaveNoAverage()
	{
		GoalTree tree = new GoalTree();
		Goal approx = new Goal("Old quest");
		approx.markCompleted(true, 1000L, true);
		tree.add(null, approx);

		CompletedLog.Stats stats = CompletedLog.stats(tree, 2000L, ZONE);

		assertEquals(1, stats.getTotal());
		assertNull(stats.getAverageMs());
		assertEquals(0, CompletedLog.stats(new GoalTree(), 2000L, ZONE).getTotal());
	}

	@Test
	public void daysAgoCountsCalendarDays()
	{
		long now = at(2026, 12, 3, 0, 0, 10);

		assertEquals("today", CompletedLog.daysAgo(at(2026, 12, 3, 0, 0, 0), now, ZONE));
		assertEquals("yesterday", CompletedLog.daysAgo(at(2026, 12, 2, 23, 59, 59), now, ZONE));
		assertEquals("2 days ago", CompletedLog.daysAgo(at(2026, 12, 1, 0, 0, 0), now, ZONE));
	}

	@Test
	public void formatDurationPicksReadableUnit()
	{
		long hour = 60L * 60 * 1000;
		assertEquals("under an hour", CompletedLog.formatDuration(10 * 60 * 1000));
		assertEquals("1 hour", CompletedLog.formatDuration(hour));
		assertEquals("5 hours", CompletedLog.formatDuration(5 * hour + 59 * 60 * 1000));
		assertEquals("1 day", CompletedLog.formatDuration(CompletedLog.DAY_MS));
		assertEquals("1.5 days", CompletedLog.formatDuration(CompletedLog.DAY_MS * 3 / 2));
		assertEquals("3 days", CompletedLog.formatDuration(3 * CompletedLog.DAY_MS));
		assertEquals("12 days", CompletedLog.formatDuration(12 * CompletedLog.DAY_MS + 3 * hour));
	}

	@Test
	public void formatDateUsesShortMonth()
	{
		assertEquals("12 Nov 2026", CompletedLog.formatDate(at(2026, 11, 12, 23, 30, 0), ZONE));
	}

	private static Goal goal(long createdAt, long completedAt)
	{
		Goal goal = new Goal("Goal");
		goal.setCreatedAt(createdAt);
		goal.markCompleted(true, completedAt);
		return goal;
	}

	private static String group(long completedAt, long now)
	{
		return CompletedLog.groupFor(completedAt, now, ZONE);
	}

	private static long at(int year, int month, int day, int hour, int minute, int second)
	{
		return LocalDateTime.of(year, month, day, hour, minute, second).atZone(ZONE).toInstant().toEpochMilli();
	}

	private static List<String> headers(List<CompletedLog.Section> sections)
	{
		List<String> headers = new ArrayList<>();
		sections.forEach(section -> headers.add(section.getHeader()));
		return headers;
	}

	private static List<String> titles(List<CompletedLog.Section> sections)
	{
		List<String> titles = new ArrayList<>();
		sections.forEach(section -> section.getEntries().forEach(e -> titles.add(e.getGoal().getDisplayTitle())));
		return titles;
	}

	@Test
	public void sharedGoalListsAllParentsOnce()
	{
		long now = at(2026, 12, 3, 12, 0, 0);
		GoalTree tree = new GoalTree();
		Goal gloves = new Goal("Barrows gloves");
		Goal questCape = new Goal("Quest cape");
		Goal rfd = new Goal("Recipe for Disaster");
		tree.add(null, gloves);
		tree.add(null, questCape);
		tree.add(gloves.getId(), rfd);
		tree.link(questCape.getId(), rfd.getId());
		rfd.markCompleted(true, now);

		List<CompletedLog.Section> sections = CompletedLog.build(tree, CompletedLog.TypeFilter.ALL, false, true, now, ZONE);

		assertEquals(1, sections.get(0).getEntries().size());
		assertEquals("Barrows gloves, Quest cape", sections.get(0).getEntries().get(0).getParents());
		assertEquals(1, CompletedLog.stats(tree, now, ZONE).getTotal());
	}

	@Test
	public void levelsAndQuestPointsShareOneFilter()
	{
		long now = at(2026, 12, 3, 12, 0, 0);
		GoalTree tree = new GoalTree();
		Goal total = new Goal("");
		total.setRequirement(new TotalLevelRequirement(1500));
		Goal combat = new Goal("");
		combat.setRequirement(new CombatLevelRequirement(100));
		Goal qp = new Goal("");
		qp.setRequirement(new QuestPointsRequirement(150));
		Goal quest = new Goal("A quest");
		quest.setRequirement(new QuestRequirement(1, "A quest"));
		for (Goal goal : List.of(total, combat, qp, quest))
		{
			tree.add(null, goal);
			goal.markCompleted(true, now);
		}

		assertEquals(List.of("Total level 1500", "Combat level 100", "150 quest points"),
			titles(CompletedLog.build(tree, CompletedLog.TypeFilter.ACCOUNT, false, false, now, ZONE)));
		assertEquals(List.of("A quest"),
			titles(CompletedLog.build(tree, CompletedLog.TypeFilter.QUEST, false, false, now, ZONE)));
	}
}
