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

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

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
	 */
	public static final int CURRENT_VERSION = 6;
	static final String VERSION_FIELD = "schemaVersion";

	// Nyckel = versionen steget uppgraderar från.
	private final Map<Integer, UnaryOperator<JsonObject>> steps;

	public SchemaMigrator()
	{
		this(Map.of(
			1, SchemaMigrator::noStructuralChange,
			2, SchemaMigrator::noStructuralChange,
			3, SchemaMigrator::noStructuralChange,
			4, SchemaMigrator::noStructuralChange,
			5, SchemaMigrator::noStructuralChange));
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
