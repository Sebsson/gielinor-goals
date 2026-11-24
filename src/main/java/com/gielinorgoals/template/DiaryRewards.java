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

import com.gielinorgoals.model.DiaryArea;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Vilket föremål varje diary-område ger på nivå 1-4, ur {@code diary-rewards.json}.
 */
@Slf4j
public final class DiaryRewards
{
	static final String RESOURCE = "diary-rewards.json";

	@Value
	public static class Reward
	{
		DiaryArea area;
		// Föremålets namn utan nivå, t.ex. "Varrock armour".
		String name;
		// Item-id för nivå 1-4, i DiaryTier-ordning.
		List<Integer> items;

		public String itemName(int tierIndex)
		{
			return name + " " + (tierIndex + 1);
		}
	}

	// Fälten fylls av Gson.
	private static final class FileSpec
	{
		List<AreaSpec> areas;
	}

	private static final class AreaSpec
	{
		String area;
		String name;
		List<Integer> items;
	}

	private final List<Reward> rewards;

	private DiaryRewards(List<Reward> rewards)
	{
		this.rewards = Collections.unmodifiableList(rewards);
	}

	public List<Reward> getRewards()
	{
		return rewards;
	}

	@Nullable
	public Reward get(DiaryArea area)
	{
		for (Reward reward : rewards)
		{
			if (reward.getArea() == area)
			{
				return reward;
			}
		}
		return null;
	}

	/**
	 * Läser filen via klassladdarens resurser. Områden med okänt namn eller fel antal
	 * föremål hoppas över.
	 */
	public static DiaryRewards load(Gson gson)
	{
		try (InputStream in = DiaryRewards.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("Missing {}, no diary reward template", RESOURCE);
				return new DiaryRewards(new ArrayList<>());
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return new DiaryRewards(parse(gson.fromJson(reader, FileSpec.class)));
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not read {}", RESOURCE, e);
			return new DiaryRewards(new ArrayList<>());
		}
	}

	private static List<Reward> parse(@Nullable FileSpec spec)
	{
		List<Reward> rewards = new ArrayList<>();
		if (spec == null || spec.areas == null)
		{
			return rewards;
		}
		for (AreaSpec area : spec.areas)
		{
			DiaryArea diaryArea = area == null ? null : areaOf(area.area);
			if (diaryArea == null || area.name == null || area.items == null || area.items.size() != 4
				|| area.items.contains(null))
			{
				log.warn("Skipping invalid diary reward entry in {}", RESOURCE);
				continue;
			}
			rewards.add(new Reward(diaryArea, area.name, List.copyOf(area.items)));
		}
		return rewards;
	}

	@Nullable
	private static DiaryArea areaOf(@Nullable String name)
	{
		for (DiaryArea area : DiaryArea.values())
		{
			if (area.name().equals(name))
			{
				return area;
			}
		}
		return null;
	}
}
