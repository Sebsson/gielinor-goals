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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.util.QuantityFormatter;

/**
 * Nå en viss level (även virtuell, upp till 126) eller en viss mängd XP i en skill.
 * Levelmål jämförs mot XP-gränsen för leveln, så virtuella levels fungerar likadant.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class SkillRequirement extends Requirement
{
	public enum Mode
	{
		LEVEL,
		XP
	}

	public static final int MIN_LEVEL = 2;
	public static final int MAX_LEVEL = Experience.MAX_VIRT_LEVEL;
	public static final long MAX_XP = Experience.MAX_SKILL_XP;

	private Skill skill;
	private Mode mode;
	private long target;

	// För Gson.
	SkillRequirement()
	{
	}

	private SkillRequirement(Skill skill, Mode mode, long target)
	{
		this.skill = skill;
		this.mode = mode;
		this.target = target;
	}

	public static SkillRequirement level(Skill skill, int level)
	{
		if (level < MIN_LEVEL || level > MAX_LEVEL)
		{
			throw new IllegalArgumentException("Level must be between " + MIN_LEVEL + " and " + MAX_LEVEL);
		}
		return new SkillRequirement(skill, Mode.LEVEL, level);
	}

	public static SkillRequirement xp(Skill skill, long xp)
	{
		if (xp < 1 || xp > MAX_XP)
		{
			throw new IllegalArgumentException("XP must be between 1 and " + MAX_XP);
		}
		return new SkillRequirement(skill, Mode.XP, xp);
	}

	@Override
	public RequirementType getType()
	{
		return RequirementType.SKILL;
	}

	@Override
	public String describe()
	{
		if (mode == Mode.LEVEL)
		{
			return target + " " + skill.getName();
		}
		return QuantityFormatter.formatNumber(target) + " " + skill.getName() + " XP";
	}

	// Skill.OVERALL är markerad som deprecated men måste ändå uteslutas här.
	@SuppressWarnings("deprecation")
	@Override
	public boolean isValid()
	{
		if (skill == null || skill == Skill.OVERALL || mode == null)
		{
			return false;
		}
		return mode == Mode.LEVEL
			? target >= MIN_LEVEL && target <= MAX_LEVEL
			: target >= 1 && target <= MAX_XP;
	}

	/**
	 * XP som krävs för att målet ska vara klart.
	 */
	public long targetXp()
	{
		return mode == Mode.LEVEL ? Experience.getXpForLevel((int) target) : target;
	}

	@Nullable
	@Override
	public GoalProgress progress(GameData data)
	{
		long xp = data.getXp(skill);
		if (xp < 0)
		{
			return null;
		}

		boolean complete = xp >= targetXp();
		if (mode == Mode.LEVEL)
		{
			int level = Experience.getLevelForXp((int) Math.min(xp, MAX_XP));
			String label = level + "/" + target;
			return new GoalProgress(xp, targetXp(), complete, label);
		}

		String label = QuantityFormatter.quantityToStackSize(xp) + "/" + QuantityFormatter.quantityToStackSize(target);
		return new GoalProgress(xp, target, complete, label);
	}
}
