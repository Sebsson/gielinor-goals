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

import com.gielinorgoals.model.ActivityCounters;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.Requirement;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Översätter mellan målgrafen och det versionerade JSON-formatet
 * {@code {"schemaVersion":7,"rootIds":[...],"goals":[...]}}, där {@code goals} är en
 * platt lista och varje mål pekar ut sina delmål med {@code childIds}.
 */
public class GoalSerializer
{
	static final String GOALS_FIELD = "goals";
	static final String ROOT_IDS_FIELD = "rootIds";
	static final String COUNTERS_FIELD = "counters";
	private static final Type COUNTERS_TYPE = new TypeToken<java.util.LinkedHashMap<String, ActivityCounters.Counter>>()
	{
	}.getType();
	private static final Type ROOT_IDS_TYPE = new TypeToken<List<String>>()
	{
	}.getType();
	private static final Type GOAL_LIST_TYPE = new TypeToken<List<Goal>>()
	{
	}.getType();

	private final Gson gson;
	private final SchemaMigrator migrator;

	public GoalSerializer(Gson gson, SchemaMigrator migrator)
	{
		// Härledd från klientens Gson enligt Plugin Hubs regler, plus vår adapter för krav.
		this.gson = gson.newBuilder()
			.registerTypeAdapter(Requirement.class, new RequirementAdapter())
			.create();
		this.migrator = migrator;
	}

	public String serialize(GoalTree tree)
	{
		return serialize(tree, new ActivityCounters());
	}

	public String serialize(GoalTree tree, ActivityCounters counters)
	{
		JsonObject root = new JsonObject();
		root.addProperty(SchemaMigrator.VERSION_FIELD, SchemaMigrator.CURRENT_VERSION);
		root.add(ROOT_IDS_FIELD, gson.toJsonTree(tree.getRootIds(), ROOT_IDS_TYPE));
		// Varje mål en gång, i visningsordning, så att filen är lätt att läsa.
		root.add(GOALS_FIELD, gson.toJsonTree(tree.flatten(), GOAL_LIST_TYPE));
		if (!counters.asMap().isEmpty())
		{
			root.add(COUNTERS_FIELD, gson.toJsonTree(new java.util.LinkedHashMap<>(counters.asMap()), COUNTERS_TYPE));
		}
		return gson.toJson(root);
	}

	public LoadResult deserialize(@Nullable String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return new LoadResult(LoadResult.Status.OK, new GoalTree(), false, new ActivityCounters());
		}

		try
		{
			// RuneLite levererar en äldre Gson utan JsonParser.parseString.
			JsonElement element = gson.fromJson(json, JsonElement.class);
			if (element == null || !element.isJsonObject())
			{
				return corrupt();
			}

			JsonObject root = element.getAsJsonObject();
			int version = SchemaMigrator.versionOf(root);
			LoadResult.Status status = LoadResult.Status.OK;
			boolean migrated = false;

			if (version > SchemaMigrator.CURRENT_VERSION)
			{
				// Läs så gott det går men skriv aldrig tillbaka över nyare data.
				status = LoadResult.Status.NEWER_VERSION;
			}
			else if (version < SchemaMigrator.CURRENT_VERSION)
			{
				root = migrator.migrate(root);
				migrated = true;
			}

			List<Goal> goals = gson.fromJson(root.get(GOALS_FIELD), GOAL_LIST_TYPE);
			if (goals == null)
			{
				goals = new ArrayList<>();
			}
			List<String> rootIds = gson.fromJson(root.get(ROOT_IDS_FIELD), ROOT_IDS_TYPE);
			sanitize(goals);
			java.util.LinkedHashMap<String, ActivityCounters.Counter> counters =
				gson.fromJson(root.get(COUNTERS_FIELD), COUNTERS_TYPE);
			// load() lagar trasiga kopplingar och gör mål utan förälder till rötter.
			return new LoadResult(status, GoalTree.load(rootIds, goals), migrated,
				new ActivityCounters(counters == null ? new java.util.LinkedHashMap<>() : counters));
		}
		catch (JsonParseException | IllegalStateException | UnsupportedOperationException | NumberFormatException e)
		{
			return corrupt();
		}
	}

	private static LoadResult corrupt()
	{
		return new LoadResult(LoadResult.Status.CORRUPT, new GoalTree(), false, new ActivityCounters());
	}

	// Gson hoppar över konstruktorn, så saknade fält och null-poster rättas till här.
	private static void sanitize(List<Goal> goals)
	{
		Iterator<Goal> iterator = goals.iterator();
		while (iterator.hasNext())
		{
			Goal goal = iterator.next();
			if (goal == null)
			{
				iterator.remove();
				continue;
			}

			if (goal.getId() == null || goal.getId().isEmpty())
			{
				goal.setId(UUID.randomUUID().toString());
			}
			if (goal.getTitle() == null)
			{
				goal.setTitle("");
			}
			if (goal.getChildIds() == null)
			{
				goal.setChildIds(new ArrayList<>());
			}
		}
	}
}
