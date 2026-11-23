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

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Quest;

/**
 * Quests som inte räknas till Quest point cape: miniquests och delar av andra quests
 * (Recipe for Disasters delar). Listan ligger i resources, med källa, eftersom API:t
 * inte kan skilja ut dem.
 */
@Slf4j
public final class QuestCapeExclusions
{
	static final String RESOURCE = "quest-cape-exclusions.json";

	// Fälten fylls av Gson.
	private static final class Data
	{
		List<String> miniquests;
		List<String> subquests;
	}

	private final Set<Quest> miniquests;
	private final Set<Quest> subquests;

	private QuestCapeExclusions(Set<Quest> miniquests, Set<Quest> subquests)
	{
		this.miniquests = Collections.unmodifiableSet(miniquests);
		this.subquests = Collections.unmodifiableSet(subquests);
	}

	/**
	 * Ingen exkludering alls; för tester som jämför med och utan listan.
	 */
	static QuestCapeExclusions none()
	{
		return new QuestCapeExclusions(new HashSet<>(), new HashSet<>());
	}

	/**
	 * Läser listan via klassladdarens resurser.
	 */
	public static QuestCapeExclusions load(Gson gson)
	{
		try (InputStream in = QuestCapeExclusions.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("Missing {}, quest cape will include miniquests", RESOURCE);
				return new QuestCapeExclusions(new HashSet<>(), new HashSet<>());
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				Data data = gson.fromJson(reader, Data.class);
				return new QuestCapeExclusions(toQuests(data.miniquests), toQuests(data.subquests));
			}
		}
		catch (IOException e)
		{
			log.warn("Could not read {}", RESOURCE, e);
			return new QuestCapeExclusions(new HashSet<>(), new HashSet<>());
		}
	}

	// Namn som inte finns i enumen (t.ex. efter ett namnbyte i RuneLite) hoppas över.
	private static Set<Quest> toQuests(List<String> names)
	{
		Set<Quest> quests = new HashSet<>();
		if (names == null)
		{
			return quests;
		}
		for (String name : names)
		{
			try
			{
				quests.add(Quest.valueOf(name));
			}
			catch (IllegalArgumentException e)
			{
				log.debug("Unknown quest in {}: {}", RESOURCE, name);
			}
		}
		return quests;
	}

	public Set<Quest> getMiniquests()
	{
		return miniquests;
	}

	public Set<Quest> getSubquests()
	{
		return subquests;
	}

	public boolean countsForCape(Quest quest)
	{
		return !miniquests.contains(quest) && !subquests.contains(quest);
	}
}
