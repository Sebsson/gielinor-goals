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
package com.gielinorgoals.model;

import javax.annotation.Nullable;
import net.runelite.api.Skill;

/**
 * Tolkar en målnyckel (se TemplateKeys) som ett krav, så att {@code doneWhen} kan uppfyllas
 * direkt från spelläget även när det inte finns något mål med nyckeln, t.ex. "quest:47" för
 * ett quest som redan är klart.
 */
public final class KeyRequirements
{
	private KeyRequirements()
	{
	}

	/**
	 * @return kravet, eller null för nycklar som inte går att läsa ur spelet (t.ex. "group:...")
	 */
	@Nullable
	public static Requirement parse(@Nullable String key)
	{
		if (key == null)
		{
			return null;
		}
		String[] parts = key.split(":");
		try
		{
			switch (parts[0])
			{
				case "quest":
					return parts.length == 2 ? new QuestRequirement(Integer.parseInt(parts[1]), "") : null;
				case "diary":
					return parts.length == 3 ? new DiaryRequirement(DiaryArea.valueOf(parts[1]), DiaryTier.valueOf(parts[2]))
						: null;
				case "item":
					if (parts.length == 2 || parts.length == 3)
					{
						long quantity = parts.length == 3 ? Long.parseLong(parts[2]) : 1;
						// Med varianter, så att t.ex. en laddad version också räknas.
						return new ItemRequirement(Integer.parseInt(parts[1]), "", quantity, true);
					}
					return null;
				case "skill":
					return parts.length == 3 ? SkillRequirement.level(Skill.valueOf(parts[1]), Integer.parseInt(parts[2]))
						: null;
				default:
					return null;
			}
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}
}
