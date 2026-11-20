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

import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.RequirementType;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/**
 * Sparar krav polymorft med ett "type"-fält, t.ex. {"type":"SKILL","skill":"AGILITY",...}.
 * Okända eller trasiga krav blir null, så att målet finns kvar som ett manuellt mål.
 */
class RequirementAdapter implements JsonSerializer<Requirement>, JsonDeserializer<Requirement>
{
	private static final String TYPE_FIELD = "type";

	@Override
	public JsonElement serialize(Requirement src, Type typeOfSrc, JsonSerializationContext context)
	{
		// Konkret klass, så att Gson inte går tillbaka in i den här adaptern.
		JsonObject json = context.serialize(src, src.getClass()).getAsJsonObject();
		json.addProperty(TYPE_FIELD, src.getType().name());
		return json;
	}

	@Override
	public Requirement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
	{
		if (!json.isJsonObject())
		{
			return null;
		}

		JsonObject object = json.getAsJsonObject();
		if (!object.has(TYPE_FIELD))
		{
			return null;
		}

		RequirementType type;
		try
		{
			type = RequirementType.valueOf(object.get(TYPE_FIELD).getAsString());
		}
		catch (IllegalArgumentException | UnsupportedOperationException | IllegalStateException e)
		{
			return null;
		}

		try
		{
			Requirement requirement = context.deserialize(object, type.getImplementation());
			return requirement != null && requirement.isValid() ? requirement : null;
		}
		catch (JsonParseException e)
		{
			return null;
		}
	}
}
