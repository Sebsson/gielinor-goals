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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.util.QuantityFormatter;

/**
 * Kontots nivåer räknade ur XP i spelläget: total level och combat level med riktiga
 * nivåer (max 99, inte virtuella och inte boostade). Gränserna räknas från
 * {@link Skill}, så att de stämmer även när spelet får fler skills.
 */
final class AccountLevels
{
	static final List<Skill> SKILLS;

	static
	{
		List<Skill> skills = new ArrayList<>();
		for (Skill skill : Skill.values())
		{
			if (isRealSkill(skill))
			{
				skills.add(skill);
			}
		}
		SKILLS = Collections.unmodifiableList(skills);
	}

	// Ett nytt konto har level 1 i allt utom Hitpoints, som börjar på 10.
	static final int MIN_TOTAL_LEVEL = SKILLS.size() - 1 + 10;
	static final int MAX_TOTAL_LEVEL = SKILLS.size() * Experience.MAX_REAL_LEVEL;
	static final int MIN_COMBAT_LEVEL = 3;
	static final int MAX_COMBAT_LEVEL = Experience.MAX_COMBAT_LEVEL;

	private AccountLevels()
	{
	}

	// Skill.OVERALL är markerad som deprecated men måste ändå uteslutas här.
	@SuppressWarnings("deprecation")
	private static boolean isRealSkill(Skill skill)
	{
		return skill != Skill.OVERALL;
	}

	/**
	 * @return riktig nivå (1–99), eller -1 om skillens XP inte är känd
	 */
	static int realLevel(GameData data, Skill skill)
	{
		long xp = data.getXp(skill);
		if (xp < 0)
		{
			return -1;
		}
		return Math.min(Experience.MAX_REAL_LEVEL, Experience.getLevelForXp((int) Math.min(xp, Experience.MAX_SKILL_XP)));
	}

	/**
	 * @return total level, eller -1 om någon skill inte är känd än
	 */
	static int totalLevel(GameData data)
	{
		int total = 0;
		for (Skill skill : SKILLS)
		{
			int level = realLevel(data, skill);
			if (level < 0)
			{
				return -1;
			}
			total += level;
		}
		return total;
	}

	/**
	 * @return combat level, eller -1 om någon av combat-skillerna inte är känd än
	 */
	static int combatLevel(GameData data)
	{
		int attack = realLevel(data, Skill.ATTACK);
		int strength = realLevel(data, Skill.STRENGTH);
		int defence = realLevel(data, Skill.DEFENCE);
		int hitpoints = realLevel(data, Skill.HITPOINTS);
		int magic = realLevel(data, Skill.MAGIC);
		int ranged = realLevel(data, Skill.RANGED);
		int prayer = realLevel(data, Skill.PRAYER);
		if (attack < 0 || strength < 0 || defence < 0 || hitpoints < 0 || magic < 0 || ranged < 0 || prayer < 0)
		{
			return -1;
		}
		return Experience.getCombatLevel(attack, strength, defence, hitpoints, magic, ranged, prayer);
	}

	/**
	 * Progress "nuvarande/mål", klar när nuvarande når målet.
	 */
	static GoalProgress progress(long current, long target)
	{
		String label = QuantityFormatter.formatNumber(current) + "/" + QuantityFormatter.formatNumber(target);
		return new GoalProgress(current, target, current >= target, label);
	}

	/**
	 * Hover-text, t.ex. "Total level 1,234 / 1,500" och "266 levels left".
	 */
	static String describe(String prefix, GoalProgress progress, String singular, String plural)
	{
		String numbers = prefix + QuantityFormatter.formatNumber(progress.getCurrent()) + " / "
			+ QuantityFormatter.formatNumber(progress.getTarget());
		if (progress.isComplete())
		{
			return numbers + "\nDone";
		}
		long left = progress.getTarget() - progress.getCurrent();
		return numbers + "\n" + QuantityFormatter.formatNumber(left) + " " + (left == 1 ? singular : plural) + " left";
	}
}
