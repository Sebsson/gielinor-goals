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
package com.gielinorgoals.tracking;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;
import net.runelite.client.util.Text;

/**
 * Läser spelets egna räknemeddelanden i chatten och mappar dem till {@link HiscoreSkill}:
 * <ul>
 * <li>"Your Zulrah kill count is: 1,234."</li>
 * <li>"Your Barrows chest count is: 87."</li>
 * <li>"Your completion count for Tombs of Amascut: Expert Mode is: 12."</li>
 * <li>"Your completed Chambers of Xeric count is: 40."</li>
 * <li>"Your subdued Wintertodt count is: 500."</li>
 * <li>"You have completed 312 hard Treasure Trails."</li>
 * </ul>
 * Färgtaggar och tusentalsavgränsare tas bort. Namn som inte finns i hiscores ignoreras.
 */
public final class KillCountMessages
{
	@Value
	public static class Count
	{
		HiscoreSkill skill;
		long count;
	}

	// "Your completion count for Tombs of Amascut: Expert Mode is: 12." har inget "count" efter namnet.
	private static final Pattern COMPLETION_COUNT_FOR = Pattern.compile(
		"Your completion count for (?<name>.+?) is: (?<count>\\d[\\d,]*)",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern KILL_COUNT = Pattern.compile(
		"Your (?:subdued |completed )?(?<name>.+?) "
			+ "(?:(?:kill|harvest|completion|success) )?count is: (?<count>\\d[\\d,]*)",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern CLUE = Pattern.compile(
		"You have completed (?<count>\\d[\\d,]*) (?<tier>beginner|easy|medium|hard|elite|master) Treasure Trails?",
		Pattern.CASE_INSENSITIVE);

	// Normaliserat namn (små bokstäver, bara bokstäver, siffror och mellanslag) → aktivitet.
	private static final Map<String, HiscoreSkill> BY_NAME = new HashMap<>();

	static
	{
		for (HiscoreSkill skill : HiscoreSkill.values())
		{
			if (skill.getType() == HiscoreSkillType.BOSS || skill.getType() == HiscoreSkillType.ACTIVITY)
			{
				BY_NAME.put(normalize(skill.getName()), skill);
			}
		}
	}

	private KillCountMessages()
	{
	}

	/**
	 * @return räknaren meddelandet anger, eller null om det inte är ett känt räknemeddelande
	 */
	@Nullable
	public static Count parse(String message)
	{
		String text = Text.removeTags(message);

		Matcher clue = CLUE.matcher(text);
		if (clue.find())
		{
			HiscoreSkill skill = HiscoreSkill.valueOf("CLUE_SCROLL_" + clue.group("tier").toUpperCase(Locale.ROOT));
			return new Count(skill, number(clue.group("count")));
		}

		Matcher completion = COMPLETION_COUNT_FOR.matcher(text);
		if (completion.find())
		{
			HiscoreSkill skill = lookup(completion.group("name"));
			return skill == null ? null : new Count(skill, number(completion.group("count")));
		}

		Matcher kill = KILL_COUNT.matcher(text);
		if (kill.find())
		{
			HiscoreSkill skill = lookup(kill.group("name"));
			return skill == null ? null : new Count(skill, number(kill.group("count")));
		}
		return null;
	}

	/**
	 * Spelets namn skiljer ibland från hiscores: "Barrows chest" mot "Barrows Chests",
	 * "Gauntlet" mot "The Gauntlet", "Royal Titan" mot "The Royal Titans".
	 */
	@Nullable
	static HiscoreSkill lookup(String name)
	{
		String base = normalize(name);
		for (String candidate : new String[]{base, base + "s", "the " + base, "the " + base + "s"})
		{
			HiscoreSkill skill = BY_NAME.get(candidate);
			if (skill != null)
			{
				return skill;
			}
		}
		return null;
	}

	static String normalize(String name)
	{
		return name.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9 ]", "")
			.replaceAll(" +", " ")
			.trim();
	}

	private static long number(String text)
	{
		return Long.parseLong(text.replace(",", ""));
	}
}
