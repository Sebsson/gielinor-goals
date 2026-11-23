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
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Läser de handplockade templaterna i resources/com/gielinorgoals/template/handpicked
 * via klassladdaren. Vilka filer som finns står i index.json, eftersom en resurskatalog
 * inte går att lista.
 */
@Slf4j
final class JsonTemplates
{
	static final String DIRECTORY = "handpicked/";
	static final String INDEX = DIRECTORY + "index.json";

	// Fälten fylls av Gson.
	private static final class Index
	{
		List<String> templates;
	}

	private JsonTemplates()
	{
	}

	static List<GoalTemplate> load(Gson gson)
	{
		List<GoalTemplate> templates = new ArrayList<>();
		Index index = read(gson, INDEX, Index.class);
		if (index == null || index.templates == null)
		{
			return templates;
		}

		for (String file : index.templates)
		{
			JsonTemplate.FileSpec spec = read(gson, DIRECTORY + file, JsonTemplate.FileSpec.class);
			try
			{
				templates.add(JsonTemplate.from(spec));
			}
			catch (IllegalArgumentException e)
			{
				// En trasig fil hoppas över helt, så att användaren aldrig får en halv template.
				log.warn("Skipping template {}: {}", file, e.getMessage());
			}
		}
		return templates;
	}

	private static <T> T read(Gson gson, String resource, Class<T> type)
	{
		try (InputStream in = JsonTemplates.class.getResourceAsStream(resource))
		{
			if (in == null)
			{
				log.warn("Missing template resource {}", resource);
				return null;
			}
			try (Reader reader = new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
			{
				return gson.fromJson(reader, type);
			}
		}
		catch (IOException | JsonParseException e)
		{
			log.warn("Could not read template resource {}", resource, e);
			return null;
		}
	}
}
