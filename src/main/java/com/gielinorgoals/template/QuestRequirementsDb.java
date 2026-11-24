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
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Quest;

/**
 * Questkrav från OSRS Wiki, incheckade som en statisk fil i resources och lästa en gång
 * via klassladdaren. Pluginet hämtar ingenting vid körning. Ett quest som saknas i filen
 * (t.ex. ett nytt quest efter en RuneLite-uppdatering) ger bara {@code null}, aldrig ett fel.
 */
@Slf4j
@Singleton
public class QuestRequirementsDb
{
	static final String RESOURCE = "/com/gielinorgoals/quest-requirements.json";

	// Fälten fylls av Gson och motsvarar filens format.
	@Getter
	public static final class Ref
	{
		private int id;
		private String name;
	}

	@Getter
	public static final class Partial
	{
		private int id;
		private String name;
		// Wikins text, t.ex. "Started Legends' Quest".
		private String text;
	}

	@Getter
	public static final class SkillReq
	{
		// Namnet på en Skill, t.ex. "COOKING".
		private String skill;
		private int level;
		private boolean boostable;
	}

	@Getter
	public static final class Entry
	{
		private int id;
		private String name;
		private List<Ref> quests;
		private List<Partial> partial;
		private List<SkillReq> skills;
		@Nullable
		private Integer questPoints;
		@Nullable
		private String note;

		public List<Ref> getQuests()
		{
			return quests == null ? Collections.emptyList() : quests;
		}

		public List<Partial> getPartial()
		{
			return partial == null ? Collections.emptyList() : partial;
		}

		public List<SkillReq> getSkills()
		{
			return skills == null ? Collections.emptyList() : skills;
		}
	}

	/**
	 * En quest-serie från wikin, t.ex. "Elf", med questen i wikins kronologiska ordning.
	 * Ett quest kan finnas i flera serier.
	 */
	@Getter
	public static final class Series
	{
		private String name;
		private List<Ref> quests;

		public List<Ref> getQuests()
		{
			return quests == null ? Collections.emptyList() : quests;
		}
	}

	private static final class FileSpec
	{
		List<Entry> quests;
		List<Series> series;
	}

	private final Map<Integer, Entry> byId;
	@Getter
	private final List<Series> series;

	@Inject
	QuestRequirementsDb(Gson gson)
	{
		this(read(gson));
	}

	private QuestRequirementsDb(FileSpec spec)
	{
		this(spec.quests == null ? Collections.emptyList() : spec.quests,
			spec.series == null ? Collections.emptyList() : spec.series);
	}

	QuestRequirementsDb(List<Entry> entries)
	{
		this(entries, Collections.emptyList());
	}

	QuestRequirementsDb(List<Entry> entries, List<Series> series)
	{
		this.series = Collections.unmodifiableList(new ArrayList<>(series));
		Map<Integer, Entry> map = new HashMap<>();
		for (Entry entry : entries)
		{
			if (entry != null)
			{
				map.put(entry.id, entry);
			}
		}
		this.byId = Collections.unmodifiableMap(map);
		List<String> missing = missingQuests();
		if (!missing.isEmpty())
		{
			log.debug("Quests without requirement data: {}", missing);
		}
	}

	public static QuestRequirementsDb load(Gson gson)
	{
		return new QuestRequirementsDb(read(gson));
	}

	private static FileSpec read(Gson gson)
	{
		try (InputStream in = QuestRequirementsDb.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("Missing {}, quest chains are unavailable", RESOURCE);
				return new FileSpec();
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				FileSpec spec = gson.fromJson(reader, FileSpec.class);
				return spec == null ? new FileSpec() : spec;
			}
		}
		catch (IOException | JsonParseException e)
		{
			log.warn("Could not read {}", RESOURCE, e);
			return new FileSpec();
		}
	}

	/**
	 * @return kraven för questet, eller null om det inte finns i filen
	 */
	@Nullable
	public Entry get(int questId)
	{
		return byId.get(questId);
	}

	public Map<Integer, Entry> all()
	{
		return byId;
	}

	/**
	 * Serien med det namnet, eller null.
	 */
	@Nullable
	public Series series(String name)
	{
		for (Series candidate : series)
		{
			if (candidate.getName().equals(name))
			{
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Quests i RuneLites {@link Quest} som inte finns i filen, för loggning och tester.
	 */
	public List<String> missingQuests()
	{
		List<String> missing = new ArrayList<>();
		for (Quest quest : Quest.values())
		{
			if (!byId.containsKey(quest.getId()))
			{
				missing.add(quest.getName());
			}
		}
		return missing;
	}
}
