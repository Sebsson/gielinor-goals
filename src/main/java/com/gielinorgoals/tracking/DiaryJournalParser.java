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
package com.gielinorgoals.tracking;

import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryJournal;
import com.gielinorgoals.model.DiaryTier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.runelite.client.util.Text;

/**
 * Tolkar textraderna i en diary-journal (interface JOURNALSCROLL) till uppgifter per nivå.
 *
 * Journalen är text från servern, redan radbruten: en rubrik per nivå ("Easy", "Medium" ...),
 * sedan en uppgift per rad där långa uppgifter fortsätter på nästa rad. Klara uppgifter är
 * överstrukna med {@code <str>}. Varje uppgift slutar med punkt, så en rad utan avslutande
 * skiljetecken fortsätter på nästa rad. En klar nivå avslutas dessutom med en ej överstruken
 * anteckning ("If I ever lose my ... I can speak to ..."), som tas bort.
 *
 * Som säkerhet används bara nivåer där antalet hittade uppgifter stämmer med spelets
 * totalsiffra; annars kan t.ex. en layoutändring ge fel delmål.
 */
public final class DiaryJournalParser
{
	private static final Pattern TIER_HEADER = Pattern.compile("^(easy|medium|hard|elite)( tasks)?:?$");
	// RuneLites inbyggda Diary Requirements-plugin lägger till krav i den här formen.
	private static final String RUNELITE_REQUIREMENT_MARKER = "<col=ffffff>(";

	private DiaryJournalParser()
	{
	}

	/**
	 * @param title journalens rubrikrad, t.ex. "Varrock Area Tasks"
	 * @param lines radernas råtext (med taggar) i ordning
	 * @return null om rubriken inte är en diary-journal
	 */
	@Nullable
	public static DiaryJournal parse(String title, List<String> lines)
	{
		DiaryArea area = DiaryArea.fromJournalTitle(Text.removeTags(title));
		if (area == null)
		{
			return null;
		}

		Map<DiaryTier, List<DiaryJournal.Task>> found = new EnumMap<>(DiaryTier.class);
		DiaryTier tier = null;
		StringBuilder current = null;
		boolean currentDone = false;
		String previousLine = null;

		for (String raw : lines)
		{
			String line = stripRuneLiteRequirements(raw == null ? "" : raw);
			String clean = Text.removeTags(line).trim();
			DiaryTier header = tierHeader(clean);

			if (header != null || clean.isEmpty())
			{
				addTask(found, tier, current, currentDone);
				current = null;
				previousLine = null;
				if (header != null)
				{
					tier = header;
				}
				continue;
			}
			if (tier == null)
			{
				continue;
			}

			boolean continuation = current != null && previousLine != null && !endsSentence(previousLine);
			if (continuation)
			{
				current.append(' ').append(clean);
			}
			else
			{
				addTask(found, tier, current, currentDone);
				current = new StringBuilder(clean);
				// Överstrykningen kan ligga efter en färgtagg, t.ex. "<col=000080><str>...".
				currentDone = line.toLowerCase(Locale.ROOT).contains("<str>");
			}
			previousLine = clean;
		}
		addTask(found, tier, current, currentDone);

		Map<DiaryTier, List<DiaryJournal.Task>> accepted = new EnumMap<>(DiaryTier.class);
		Map<DiaryTier, Integer> mismatched = new EnumMap<>(DiaryTier.class);
		found.forEach((foundTier, tasks) ->
		{
			dropCompletionNote(tasks, area.taskTotalFor(foundTier));
			if (tasks.size() == area.taskTotalFor(foundTier))
			{
				accepted.put(foundTier, Collections.unmodifiableList(tasks));
			}
			else
			{
				mismatched.put(foundTier, tasks.size());
			}
		});
		return new DiaryJournal(area, Collections.unmodifiableMap(accepted), Collections.unmodifiableMap(mismatched));
	}

	@Nullable
	static DiaryTier tierHeader(String clean)
	{
		Matcher matcher = TIER_HEADER.matcher(clean.toLowerCase(Locale.ROOT));
		if (!matcher.matches())
		{
			return null;
		}
		return DiaryTier.valueOf(matcher.group(1).toUpperCase(Locale.ROOT));
	}

	private static void addTask(Map<DiaryTier, List<DiaryJournal.Task>> found, @Nullable DiaryTier tier,
		@Nullable StringBuilder text, boolean done)
	{
		if (tier == null || text == null || text.length() == 0)
		{
			return;
		}
		found.computeIfAbsent(tier, t -> new ArrayList<>()).add(new DiaryJournal.Task(text.toString(), done));
	}

	private static String stripRuneLiteRequirements(String line)
	{
		int marker = line.indexOf(RUNELITE_REQUIREMENT_MARKER);
		return marker < 0 ? line : line.substring(0, marker);
	}

	private static boolean endsSentence(String clean)
	{
		char last = clean.charAt(clean.length() - 1);
		return last == '.' || last == '!' || last == '?';
	}

	/**
	 * En klar nivå avslutas med en anteckning om belöningen. Den är inte överstruken medan
	 * alla riktiga uppgifter är det, så om det finns exakt en rad för mycket och den ser ut
	 * så, tas den bort.
	 */
	private static void dropCompletionNote(List<DiaryJournal.Task> tasks, int total)
	{
		if (tasks.size() != total + 1 || tasks.get(tasks.size() - 1).isDone())
		{
			return;
		}
		for (int i = 0; i < total; i++)
		{
			if (!tasks.get(i).isDone())
			{
				return;
			}
		}
		tasks.remove(tasks.size() - 1);
	}
}
