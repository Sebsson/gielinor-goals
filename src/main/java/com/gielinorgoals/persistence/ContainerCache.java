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
package com.gielinorgoals.persistence;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * Sparat innehåll i ett förråd som bara kan läsas när det är öppet, t.ex. banken.
 * Föremåls-id är redan omräknade till grundföremål (inte noterade, inte placeholders).
 */
@Getter
public class ContainerCache
{
	private long savedAt;
	private Map<Integer, Long> items;

	// För Gson.
	ContainerCache()
	{
	}

	public ContainerCache(long savedAt, Map<Integer, Long> items)
	{
		this.savedAt = savedAt;
		this.items = Collections.unmodifiableMap(new HashMap<>(items));
	}

	public String toJson(Gson gson)
	{
		return gson.toJson(this);
	}

	/**
	 * @return null om datan saknas eller inte går att tolka; cachen byggs då om nästa gång förrådet öppnas
	 */
	@Nullable
	public static ContainerCache fromJson(Gson gson, @Nullable String json)
	{
		if (json == null || json.isEmpty())
		{
			return null;
		}

		try
		{
			ContainerCache cache = gson.fromJson(json, ContainerCache.class);
			if (cache == null || cache.items == null)
			{
				return null;
			}
			return new ContainerCache(cache.savedAt, cache.items);
		}
		catch (JsonParseException | NumberFormatException e)
		{
			return null;
		}
	}
}
