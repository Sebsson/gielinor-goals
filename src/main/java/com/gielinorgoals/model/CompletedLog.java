/*
 * Copyright (c) 2026, Sebas
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gielinorgoals.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Completed-flikens innehåll: alla klara mål (alla nivåer) som en platt lista,
 * filtrerad, sorterad och grupperad under datumrubriker. Ren logik utan Swing,
 * så att gruppering och beräkningar går att testa.
 */
public final class CompletedLog
{
	static final long DAY_MS = 24L * 60 * 60 * 1000;
	static final String UNKNOWN_DATE = "Unknown date";
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

	@Getter
	@RequiredArgsConstructor
	public enum TypeFilter
	{
		ALL("All types"),
		SKILL("Skills"),
		QUEST("Quests"),
		DIARY("Diaries"),
		ITEM("Items"),
		COMBAT_ACHIEVEMENT("Combat achievements"),
		MANUAL("Manual goals");

		private final String label;

		boolean matches(Goal goal)
		{
			Requirement requirement = goal.getRequirement();
			switch (this)
			{
				case ALL:
					return true;
				case MANUAL:
					return requirement == null;
				default:
					return requirement != null && requirement.getType().name().equals(name());
			}
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@Value
	public static class Entry
	{
		Goal goal;
		// Föräldramålens namn, kommaseparerade; null för rotmål.
		@Nullable
		String parents;
	}

	@Value
	public static class Section
	{
		String header;
		List<Entry> entries;
	}

	private CompletedLog()
	{
	}

	/**
	 * @param newestFirst sortering; mål utan datum hamnar alltid sist
	 */
	public static List<Section> build(GoalTree tree, TypeFilter type, boolean rootsOnly, boolean newestFirst,
		long now, ZoneId zone)
	{
		List<Entry> dated = new ArrayList<>();
		List<Entry> undated = new ArrayList<>();
		for (Goal goal : tree.flatten())
		{
			if (!goal.isCompleted() || !type.matches(goal))
			{
				continue;
			}

			List<Goal> parents = tree.getParents(goal.getId());
			if (rootsOnly && !parents.isEmpty())
			{
				continue;
			}

			Entry entry = new Entry(goal, parents.isEmpty() ? null : joinTitles(parents));
			(goal.getCompletedAt() == null ? undated : dated).add(entry);
		}

		// Stabil sortering, så mål med samma tid behåller trädets ordning.
		Comparator<Entry> byTime = Comparator.comparingLong(e -> e.getGoal().getCompletedAt());
		dated.sort(newestFirst ? byTime.reversed() : byTime);

		// Grupperna följer tiden, så samma rubrik ligger alltid i en följd.
		List<Section> sections = new ArrayList<>();
		String currentHeader = null;
		List<Entry> current = null;
		for (Entry entry : dated)
		{
			String header = groupFor(entry.getGoal().getCompletedAt(), now, zone);
			if (!header.equals(currentHeader))
			{
				currentHeader = header;
				current = new ArrayList<>();
				sections.add(new Section(header, current));
			}
			current.add(entry);
		}
		if (!undated.isEmpty())
		{
			sections.add(new Section(UNKNOWN_DATE, undated));
		}
		return sections;
	}

	/**
	 * Rubriken för ett datum: Today, Yesterday, This week (måndag–söndag), This month,
	 * annars månad och år, t.ex. "October 2026". Kollas i den ordningen, så att t.ex.
	 * gårdagen i förra månaden ändå hamnar under Yesterday.
	 */
	public static String groupFor(long completedAt, long now, ZoneId zone)
	{
		LocalDate date = Instant.ofEpochMilli(completedAt).atZone(zone).toLocalDate();
		LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();

		// Ett datum i framtiden (fel klocka) räknas som idag.
		if (!date.isBefore(today))
		{
			return "Today";
		}
		if (date.equals(today.minusDays(1)))
		{
			return "Yesterday";
		}
		if (!date.isBefore(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))))
		{
			return "This week";
		}
		YearMonth month = YearMonth.from(date);
		if (month.equals(YearMonth.from(today)))
		{
			return "This month";
		}
		return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
	}

	/**
	 * T.ex. "12 Nov 2026" i angiven tidszon.
	 */
	public static String formatDate(long millis, ZoneId zone)
	{
		return Instant.ofEpochMilli(millis).atZone(zone).format(DATE);
	}

	/**
	 * Hur lång tid målet tog, t.ex. "Took 3 days". Bara när både skapad- och klartid
	 * finns och klartiden är exakt.
	 *
	 * @return texten, eller null om den inte ska visas
	 */
	@Nullable
	public static String timeToComplete(Goal goal)
	{
		Long createdAt = goal.getCreatedAt();
		Long completedAt = goal.getCompletedAt();
		if (!goal.isCompleted() || goal.isCompletedAtApprox() || createdAt == null || completedAt == null)
		{
			return null;
		}

		long days = Math.max(0, completedAt - createdAt) / DAY_MS;
		if (days < 1)
		{
			return "Took under a day";
		}
		return "Took " + days + (days == 1 ? " day" : " days");
	}

	private static String joinTitles(List<Goal> goals)
	{
		StringBuilder text = new StringBuilder();
		for (Goal goal : goals)
		{
			if (text.length() > 0)
			{
				text.append(", ");
			}
			text.append(goal.getDisplayTitle());
		}
		return text.toString();
	}
}
