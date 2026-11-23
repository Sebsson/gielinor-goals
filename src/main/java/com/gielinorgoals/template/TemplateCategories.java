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
package com.gielinorgoals.template;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Kategorierna i template-dialogen och hur de sorteras och filtreras. Ren logik utan
 * Swing, så att ordning och sökning går att testa.
 */
public final class TemplateCategories
{
	public static final String CAPES = "Capes";
	public static final String IRONMAN_EARLY = "Ironman · Early";
	public static final String IRONMAN_MID = "Ironman · Mid";
	public static final String IRONMAN_LATE = "Ironman · Late";
	public static final String BOSSES = "Bosses";
	public static final String SKILLS = "Skills";
	// För templates med en kategori som inte finns i listan, t.ex. en stavfelad JSON-fil.
	public static final String OTHER = "Other";

	private static final List<String> ORDER = List.of(CAPES, IRONMAN_EARLY, IRONMAN_MID, IRONMAN_LATE, BOSSES, SKILLS);
	private static final List<String> IRONMAN = List.of(IRONMAN_EARLY, IRONMAN_MID, IRONMAN_LATE);

	private TemplateCategories()
	{
	}

	/**
	 * Templates per kategori i visningsordning, filtrerade på namn och taggar. För en
	 * ironman kommer ironman-kategorierna först; allt visas ändå för alla.
	 *
	 * @param query söktext; tom visar allt
	 * @return kategori → templates, utan tomma kategorier
	 */
	public static Map<String, List<GoalTemplate>> group(List<GoalTemplate> templates, String query, boolean ironman)
	{
		List<String> order = new ArrayList<>();
		if (ironman)
		{
			order.addAll(IRONMAN);
		}
		for (String category : ORDER)
		{
			if (!order.contains(category))
			{
				order.add(category);
			}
		}
		order.add(OTHER);

		Map<String, List<GoalTemplate>> grouped = new LinkedHashMap<>();
		for (String category : order)
		{
			grouped.put(category, new ArrayList<>());
		}
		String needle = query.trim().toLowerCase(Locale.ROOT);
		for (GoalTemplate template : templates)
		{
			if (!matches(template, needle))
			{
				continue;
			}
			String category = grouped.containsKey(template.getCategory()) ? template.getCategory() : OTHER;
			grouped.get(category).add(template);
		}
		grouped.values().removeIf(List::isEmpty);
		return grouped;
	}

	static boolean matches(GoalTemplate template, String needle)
	{
		if (needle.isEmpty() || template.getName().toLowerCase(Locale.ROOT).contains(needle))
		{
			return true;
		}
		for (String tag : template.getTags())
		{
			if (tag.toLowerCase(Locale.ROOT).contains(needle))
			{
				return true;
			}
		}
		return false;
	}
}
