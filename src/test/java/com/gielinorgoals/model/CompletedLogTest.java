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
}
