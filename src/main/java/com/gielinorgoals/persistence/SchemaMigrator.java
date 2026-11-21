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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;

/**
 * Uppgraderar sparad JSON steg för steg till aktuell schemaversion.
 * En ny version läggs till genom att höja {@link #CURRENT_VERSION} och registrera
 * ett steg från föregående version, till exempel {@code steps.put(1, SchemaMigrator::v1ToV2)}.
 */
public class SchemaMigrator
{
	/*
	 * Versionshistorik:
	 * 1 – manuella mål (M1)
	 * 2 – valfritt fält "requirement" på mål (M2). Höjd så att äldre versioner
	 *     blir skrivskyddade i stället för att tyst kasta bort kraven.
	 * 3 – kravtypen ITEM (M3). Samma skäl: en äldre version känner inte igen typen.
	 * 4 – kravtyperna QUEST, DIARY och COMBAT_ACHIEVEMENT (M4).
	 * 5 – skill-läget XP_GAIN med fältet startXp. En äldre version känner inte igen
	 *     läget och skulle annars göra målet manuellt.
	 * 6 – valfria fält createdAt och completedAtApprox på mål (Completed-fliken).
	 *     Gamla mål får null/false, inga datum hittas på.
	 * 7 – från träd till graf: "goals" blir en platt lista där varje mål har
	 *     "childIds" i stället för nästlade "children", plus en ordnad "rootIds".
	 *     Så kan ett delmål ha flera föräldrar men lagras en gång.
	 */
	public static final int CURRENT_VERSION = 7;
	static final String VERSION_FIELD = "schemaVersion";
	private static final String GOALS_FIELD = GoalSerializer.GOALS_FIELD;
	private static final String ROOT_IDS_FIELD = GoalSerializer.ROOT_IDS_FIELD;
	private static final String ID_FIELD = "id";
	private static final String CHILDREN_FIELD = "children";
	private static final String CHILD_IDS_FIELD = "childIds";

	// Nyckel = versionen steget uppgraderar från.
	private final Map<Integer, UnaryOperator<JsonObject>> steps;

	public SchemaMigrator()
	{
		this(Map.of(
			1, SchemaMigrator::noStructuralChange,
			2, SchemaMigrator::noStructuralChange,
			3, SchemaMigrator::noStructuralChange,
			4, SchemaMigrator::noStructuralChange,
			5, SchemaMigrator::noStructuralChange,
			6, SchemaMigrator::v6ToV7));
	}

	/**
	 * Plattar ut det nästlade trädet. Ordningen behålls: rootIds i samma ordning som
	 * rotmålen, childIds i samma ordning som barnen, och målen listas förälder före barn.
	 * Alla andra fält följer med orörda. Mål utan id får ett, eftersom kopplingarna går via id.
	 */
	static JsonObject v6ToV7(JsonObject root)
	{
		JsonArray flat = new JsonArray();
		JsonArray rootIds = new JsonArray();
		JsonElement goals = root.get(GOALS_FIELD);
		if (goals != null && !goals.isJsonNull() && !goals.isJsonArray())
		{
			// Trasig data ska rapporteras som korrupt (och sparas undan), inte bli en tom lista.
			throw new IllegalStateException("goals is not a list");
		}
		if (goals != null && goals.isJsonArray())
		{
			for (JsonElement goal : goals.getAsJsonArray())
			{
				String id = flatten(goal, flat);
				if (id != null)
				{
					rootIds.add(id);
				}
			}
		}
		root.add(GOALS_FIELD, flat);
		root.add(ROOT_IDS_FIELD, rootIds);
		return root;
	}

	@Nullable
	private static String flatten(JsonElement element, JsonArray flat)
	{
		if (element == null || !element.isJsonObject())
		{
			return null;
		}
		JsonObject goal = element.getAsJsonObject();
		JsonElement idElement = goal.get(ID_FIELD);
		if (idElement == null || !idElement.isJsonPrimitive() || idElement.getAsString().isEmpty())
		{
			goal.addProperty(ID_FIELD, UUID.randomUUID().toString());
		}
		String id = goal.get(ID_FIELD).getAsString();

		JsonElement children = goal.remove(CHILDREN_FIELD);
		JsonArray childIds = new JsonArray();
		flat.add(goal);
		if (children != null && children.isJsonArray())
		{
			for (JsonElement child : children.getAsJsonArray())
			{
				String childId = flatten(child, flat);
				if (childId != null)
				{
					childIds.add(childId);
				}
			}
		}
		goal.add(CHILD_IDS_FIELD, childIds);
		return id;
	}

	// För versioner som bara lägger till valfria fält eller nya kravtyper.
	private static JsonObject noStructuralChange(JsonObject root)
	{
		return root;
	}

	// Separat konstruktor så att tester kan injicera egna steg.
	SchemaMigrator(Map<Integer, UnaryOperator<JsonObject>> steps)
	{
		this.steps = new TreeMap<>(steps);
	}

	public static int versionOf(JsonObject root)
	{
		return root.has(VERSION_FIELD) ? root.get(VERSION_FIELD).getAsInt() : 0;
	}

	/**
	 * @param targetVersion versionen datan ska uppgraderas till
	 * @throws IllegalStateException om ett steg saknas
	 */
	public JsonObject migrate(JsonObject root, int targetVersion)
	{
		int version = versionOf(root);
		while (version < targetVersion)
		{
			UnaryOperator<JsonObject> step = steps.get(version);
			if (step == null)
			{
				throw new IllegalStateException("No migration from schema version " + version);
			}

			root = step.apply(root);
			version++;
			root.addProperty(VERSION_FIELD, version);
		}
		return root;
	}

	public JsonObject migrate(JsonObject root)
	{
		return migrate(root, CURRENT_VERSION);
	}
}
