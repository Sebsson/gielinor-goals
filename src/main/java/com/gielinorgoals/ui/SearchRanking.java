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
package com.gielinorgoals.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Gemensam sökordning för listor i dialogen: exakta träffar först, sedan namn som
 * börjar med söktexten, sedan övriga träffar. Ordningen inom varje grupp behålls.
 */
final class SearchRanking
{
	private SearchRanking()
	{
	}

	static <T> List<T> rank(List<T> items, Function<T, String> name, String query, int max)
	{
		String needle = query.trim().toLowerCase(Locale.ROOT);
		List<T> exact = new ArrayList<>();
		List<T> prefix = new ArrayList<>();
		List<T> contains = new ArrayList<>();
		if (needle.isEmpty())
		{
			// Tom sökning visar hela listan (begränsad), t.ex. alla quests som återstår.
			return items.size() > max ? new ArrayList<>(items.subList(0, max)) : new ArrayList<>(items);
		}

		for (T item : items)
		{
			String text = name.apply(item).toLowerCase(Locale.ROOT);
			if (text.equals(needle))
			{
				exact.add(item);
			}
			else if (text.startsWith(needle))
			{
				prefix.add(item);
			}
			else if (text.contains(needle))
			{
				contains.add(item);
			}
		}

		List<T> results = new ArrayList<>(exact);
		results.addAll(prefix);
		results.addAll(contains);
		return results.size() > max ? new ArrayList<>(results.subList(0, max)) : results;
	}
}
