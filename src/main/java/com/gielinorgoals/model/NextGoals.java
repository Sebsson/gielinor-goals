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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * Väljer vilka mål som är "nästa" att jobba på: ej klara mål utan ej klara delmål,
 * i listans ordning. En förälder visas alltså inte förrän dess delmål är klara, eftersom
 * det är delmålen man faktiskt gör först.
 */
public final class NextGoals
{
	/**
	 * Oföränderlig rad för overlayn, så att den kan ritas på klienttråden.
	 */
	@Value
	public static class Entry
	{
		String title;
		// Null för manuella mål eller när spelets data inte är känd.
		@Nullable
		String progressLabel;
		double fraction;
	}

	private NextGoals()
	{
	}

	public static List<Entry> select(GoalTree tree, Function<String, GoalProgress> progressById, int limit)
	{
		List<Entry> entries = new ArrayList<>();
		collect(tree.getRoots(), progressById, limit, entries);
		return Collections.unmodifiableList(entries);
	}

	private static void collect(List<Goal> goals, Function<String, GoalProgress> progressById, int limit,
		List<Entry> entries)
	{
		for (Goal goal : goals)
		{
			if (entries.size() >= limit)
			{
				return;
			}
			if (goal.isCompleted())
			{
				continue;
			}

			int before = entries.size();
			collect(goal.getChildren(), progressById, limit, entries);
			// Inga ej klara delmål hittades, så målet självt är nästa steg.
			if (entries.size() == before && !hasIncompleteChild(goal))
			{
				GoalProgress progress = progressById.apply(goal.getId());
				entries.add(new Entry(goal.getDisplayTitle(),
					progress == null ? null : progress.getLabel(),
					progress == null ? 0.0 : progress.fraction()));
			}
		}
	}

	private static boolean hasIncompleteChild(Goal goal)
	{
		for (Goal child : goal.getChildren())
		{
			if (!child.isCompleted())
			{
				return true;
			}
		}
		return false;
	}
}
