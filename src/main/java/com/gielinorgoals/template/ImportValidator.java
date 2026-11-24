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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Map;
import java.util.Set;

/**
 * Kontrollerar strukturen i importerad JSON innan den tolkas: bara kända fält, rätt typer,
 * rimliga längder, djup och antal mål. Importerad data är opålitlig, så ett fel någonstans
 * stoppar hela importen; inget läggs till till hälften.
 */
final class ImportValidator
{
	static final int MAX_DEPTH = 64;
	static final int MAX_NODES = 10_000;
	static final int MAX_TITLE_LENGTH = 200;
	static final int MAX_NOTE_LENGTH = 2000;
	static final int MAX_TEXT_LENGTH = 200;
	static final int MAX_DONE_WHEN = 20;

	private static final Set<String> ENVELOPE = Set.of("formatVersion", "kind", "template");
	private static final Set<String> FILE = Set.of("formatVersion", "expandQuestChains", "id", "verifiedBy",
		"verifiedAt", "name", "description", "category", "tags", "root");
	private static final Set<String> NODE = Set.of("id", "ref", "title", "aggregate", "wikiPage", "note",
		"expandChain", "dropSource", "killCount", "icon", "doneWhen", "quest", "skill", "level", "xp",
		"combatAchievement", "questPoints", "totalLevel", "combatLevel", "diary", "item", "children");
	private static final Map<String, Set<String>> NESTED = Map.of(
		"diary", Set.of("area", "tier"),
		"item", Set.of("id", "name", "quantity", "includeVariants", "variants"),
		"icon", Set.of("item", "activity", "sideIcon"),
		"dropSource", Set.of("hiscore", "npc", "manual", "count", "rate", "perDrop"),
		"killCount", Set.of("hiscore", "npc", "manual", "count", "rate", "perDrop"),
		"combatAchievement", Set.of("id", "name", "tier"));
	private static final Set<String> NODE_BOOLEANS = Set.of("aggregate", "expandChain");
	private static final Set<String> NODE_NUMBERS = Set.of("level", "xp", "questPoints", "totalLevel", "combatLevel");

	private int nodes;

	private ImportValidator()
	{
	}

	/**
	 * @throws IllegalArgumentException med vad som var fel och var
	 */
	static void validateEnvelope(JsonElement element)
	{
		JsonObject envelope = object(element, "The share string");
		checkKeys(envelope, ENVELOPE, "the share string");
		JsonElement template = envelope.get("template");
		if (template == null)
		{
			throw new IllegalArgumentException("The share string has no goals in it");
		}
		new ImportValidator().validateFile(object(template, "The template"));
	}

	private void validateFile(JsonObject file)
	{
		checkKeys(file, FILE, "the template");
		text(file, "name", CustomTemplates.MAX_NAME_LENGTH, "the template's name");
		text(file, "description", CustomTemplates.MAX_DESCRIPTION_LENGTH, "the template's description");
		for (String key : new String[]{"id", "verifiedBy", "verifiedAt", "category"})
		{
			text(file, key, MAX_TEXT_LENGTH, "the template's " + key);
		}
		JsonElement tags = file.get("tags");
		if (tags != null)
		{
			JsonArray array = array(tags, "The tags");
			if (array.size() > CustomTemplates.MAX_TAGS)
			{
				throw new IllegalArgumentException("The template has more than " + CustomTemplates.MAX_TAGS + " tags");
			}
			for (JsonElement tag : array)
			{
				string(tag, CustomTemplates.MAX_TAG_LENGTH, "A tag");
			}
		}
		JsonElement root = file.get("root");
		if (root == null)
		{
			throw new IllegalArgumentException("The template has no goals in it");
		}
		validateNode(object(root, "A goal"), 1);
	}

	private void validateNode(JsonObject node, int depth)
	{
		if (depth > MAX_DEPTH)
		{
			throw new IllegalArgumentException("The goals are nested more than " + MAX_DEPTH + " levels deep");
		}
		if (++nodes > MAX_NODES)
		{
			throw new IllegalArgumentException("There are more than " + MAX_NODES + " goals");
		}
		String where = where(node);
		checkKeys(node, NODE, where);
		text(node, "title", MAX_TITLE_LENGTH, "the title of " + where);
		text(node, "note", MAX_NOTE_LENGTH, "the note of " + where);
		for (String key : new String[]{"id", "ref", "wikiPage", "quest", "skill"})
		{
			text(node, key, MAX_TEXT_LENGTH, key + " in " + where);
		}
		for (String key : NODE_BOOLEANS)
		{
			if (node.has(key))
			{
				bool(node.get(key), key + " in " + where);
			}
		}
		for (String key : NODE_NUMBERS)
		{
			if (node.has(key))
			{
				number(node.get(key), key + " in " + where);
			}
		}
		for (Map.Entry<String, Set<String>> nested : NESTED.entrySet())
		{
			JsonElement value = node.get(nested.getKey());
			if (value != null)
			{
				validateNested(object(value, nested.getKey() + " in " + where), nested.getKey(), nested.getValue(),
					where);
			}
		}
		JsonElement doneWhen = node.get("doneWhen");
		if (doneWhen != null)
		{
			JsonArray keys = array(doneWhen, "doneWhen in " + where);
			if (keys.size() > MAX_DONE_WHEN)
			{
				throw new IllegalArgumentException("doneWhen in " + where + " has more than " + MAX_DONE_WHEN + " keys");
			}
			for (JsonElement key : keys)
			{
				string(key, MAX_TEXT_LENGTH, "A doneWhen key in " + where);
			}
		}
		JsonElement children = node.get("children");
		if (children != null)
		{
			for (JsonElement child : array(children, "children in " + where))
			{
				validateNode(object(child, "A sub-goal of " + where), depth + 1);
			}
		}
	}

	private static void validateNested(JsonObject value, String name, Set<String> allowed, String where)
	{
		checkKeys(value, allowed, name + " in " + where);
		for (Map.Entry<String, JsonElement> entry : value.entrySet())
		{
			String what = name + "." + entry.getKey() + " in " + where;
			JsonElement field = entry.getValue();
			if (field.isJsonPrimitive() && field.getAsJsonPrimitive().isBoolean())
			{
				continue;
			}
			if (field.isJsonPrimitive() && field.getAsJsonPrimitive().isNumber())
			{
				number(field, what);
				continue;
			}
			string(field, MAX_TEXT_LENGTH, what);
		}
	}

	// Var noden är, för felmeddelanden: titeln eller kravet om det finns.
	private static String where(JsonObject node)
	{
		for (String key : new String[]{"title", "quest", "skill", "ref"})
		{
			JsonElement value = node.get(key);
			if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())
			{
				String text = value.getAsString().trim();
				if (!text.isEmpty())
				{
					return "the goal \"" + (text.length() > 40 ? text.substring(0, 40) + "..." : text) + "\"";
				}
			}
		}
		return "a goal";
	}

	private static void checkKeys(JsonObject object, Set<String> allowed, String where)
	{
		for (String key : object.keySet())
		{
			if (!allowed.contains(key))
			{
				String shown = key.length() > 40 ? key.substring(0, 40) + "..." : key;
				throw new IllegalArgumentException("Unknown field \"" + shown + "\" in " + where);
			}
		}
	}

	private static void text(JsonObject object, String key, int max, String what)
	{
		JsonElement value = object.get(key);
		if (value != null)
		{
			string(value, max, Character.toUpperCase(what.charAt(0)) + what.substring(1));
		}
	}

	private static void string(JsonElement value, int max, String what)
	{
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
		{
			throw new IllegalArgumentException(what + " should be text");
		}
		if (value.getAsString().length() > max)
		{
			throw new IllegalArgumentException(what + " is longer than " + max + " characters");
		}
	}

	private static void bool(JsonElement value, String what)
	{
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
		{
			throw new IllegalArgumentException(what + " should be true or false");
		}
	}

	private static void number(JsonElement value, String what)
	{
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
		{
			throw new IllegalArgumentException(what + " should be a number");
		}
		JsonPrimitive primitive = value.getAsJsonPrimitive();
		double number = primitive.getAsDouble();
		if (number != Math.rint(number) || Math.abs(number) > 9.0e15)
		{
			throw new IllegalArgumentException(what + " should be a whole number");
		}
	}

	private static JsonObject object(JsonElement value, String what)
	{
		if (value == null || !value.isJsonObject())
		{
			throw new IllegalArgumentException(what + " is not in the right format");
		}
		return value.getAsJsonObject();
	}

	private static JsonArray array(JsonElement value, String what)
	{
		if (!value.isJsonArray())
		{
			throw new IllegalArgumentException(what + " should be a list");
		}
		return value.getAsJsonArray();
	}
}
