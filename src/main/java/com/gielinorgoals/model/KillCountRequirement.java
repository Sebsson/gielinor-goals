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

import java.util.Locale;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;
import net.runelite.client.util.QuantityFormatter;

/**
 * Nå ett visst antal av en aktivitet: kill count för en boss, clue-nivå eller minigame
 * från hiscores, kills av en vanlig NPC, eller en räknare som användaren räknar själv.
 * Nuvarande värde ligger inte i målet utan i {@link ActivityCounters}, så att flera mål
 * på samma aktivitet delar räknare.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class KillCountRequirement extends Requirement
{
	public enum ActivityType
	{
		// Identifierare är namnet på en HiscoreSkill (boss, clue-nivå eller minigame).
		HISCORE,
		// Identifierare är NPC:ns namn; räknas upp av loot-eventet.
		NPC,
		// Identifierare är räknarens namn; användaren räknar själv.
		MANUAL
	}

	public static final int MAX_TARGET = 10_000_000;

	private ActivityType activityType;
	private String activityId;
	private String activityName;
	private int target;

	// För Gson.
	KillCountRequirement()
	{
	}

	private KillCountRequirement(ActivityType activityType, String activityId, String activityName, int target)
	{
		if (target < 1 || target > MAX_TARGET)
		{
			throw new IllegalArgumentException("Target must be between 1 and " + MAX_TARGET);
		}
		this.activityType = activityType;
		this.activityId = activityId;
		this.activityName = activityName;
		this.target = target;
	}

	public static KillCountRequirement hiscore(HiscoreSkill skill, int target)
	{
		return new KillCountRequirement(ActivityType.HISCORE, skill.name(), skill.getName(), target);
	}

	public static KillCountRequirement npc(String npcName, int target)
	{
		String name = npcName.trim();
		if (name.isEmpty())
		{
			throw new IllegalArgumentException("Enter the NPC's name");
		}
		return new KillCountRequirement(ActivityType.NPC, name.toLowerCase(Locale.ROOT), name, target);
	}

	public static KillCountRequirement manual(String counterName, int target)
	{
		String name = counterName.trim();
		if (name.isEmpty())
		{
			throw new IllegalArgumentException("Give the counter a name");
		}
		return new KillCountRequirement(ActivityType.MANUAL, name.toLowerCase(Locale.ROOT), name, target);
	}

	/**
	 * Nyckeln i räknarlagret, t.ex. "hiscore:ZULRAH" eller "npc:lizardman shaman".
	 */
	public String counterKey()
	{
		return counterKey(activityType, activityId);
	}

	public static String counterKey(ActivityType type, String id)
	{
		return type.name().toLowerCase(Locale.ROOT) + ":" + (type == ActivityType.HISCORE ? id : id.toLowerCase(Locale.ROOT));
	}

	@Nullable
	public HiscoreSkill hiscoreSkill()
	{
		if (activityType != ActivityType.HISCORE)
		{
			return null;
		}
		try
		{
			return HiscoreSkill.valueOf(activityId);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	@Override
	public RequirementType getType()
	{
		return RequirementType.KILL_COUNT;
	}

	@Override
	public String describe()
	{
		String amount = QuantityFormatter.formatNumber(target);
		switch (activityType)
		{
			case NPC:
				return activityName + ": " + amount + " kills";
			case MANUAL:
				return activityName + ": " + amount;
			default:
				return describeHiscore(amount);
		}
	}

	// T.ex. "150 Zulrah kills", "500 hard clues", "200 Barrows chests", "Rifts closed: 500".
	private String describeHiscore(String amount)
	{
		HiscoreSkill skill = hiscoreSkill();
		if (skill != null && skill.name().startsWith("CLUE_SCROLL_"))
		{
			String tier = skill.name().substring("CLUE_SCROLL_".length()).toLowerCase(Locale.ROOT);
			return amount + (tier.equals("all") ? " clues" : " " + tier + " clues");
		}
		if (skill != null && skill.getType() == HiscoreSkillType.BOSS)
		{
			if (activityName.endsWith("Chests"))
			{
				return amount + " " + activityName.substring(0, activityName.length() - "Chests".length()) + "chests";
			}
			return amount + " " + activityName + " kills";
		}
		return activityName + ": " + amount;
	}

	@Override
	public boolean isValid()
	{
		if (activityType == null || activityId == null || activityId.isEmpty() || activityName == null
			|| target < 1 || target > MAX_TARGET)
		{
			return false;
		}
		return activityType != ActivityType.HISCORE || hiscoreSkill() != null;
	}

	@Nullable
	@Override
	public GoalProgress progress(GameData data)
	{
		// Okänt nuvarande värde ger okänd progress, så att målet aldrig bockas av på saknad data.
		long current = data.getActivityCount(counterKey());
		if (current < 0)
		{
			return null;
		}
		// T.ex. "1,240 / 3,000 kc"; egna räknare har ingen enhet.
		String label = QuantityFormatter.formatNumber(current) + " / " + QuantityFormatter.formatNumber(target)
			+ (activityType == ActivityType.MANUAL ? "" : " kc");
		return new GoalProgress(current, target, current >= target, label);
	}

	@Override
	public String describeProgress(GoalProgress progress)
	{
		String numbers = QuantityFormatter.formatNumber(progress.getCurrent()) + " / "
			+ QuantityFormatter.formatNumber(progress.getTarget()) + (activityType == ActivityType.MANUAL ? "" : " kc");
		if (progress.isComplete())
		{
			return numbers + "\nDone";
		}
		return numbers + "\n" + QuantityFormatter.formatNumber(progress.getTarget() - progress.getCurrent()) + " left";
	}

	// Visningsnamnet kan skilja (t.ex. NPC-namnets skiftläge); typ, id och mål avgör.
	@Override
	public boolean isSameGoal(@Nullable Requirement other)
	{
		if (!(other instanceof KillCountRequirement))
		{
			return false;
		}
		KillCountRequirement kc = (KillCountRequirement) other;
		return kc.activityType == activityType && kc.target == target && counterKey().equals(kc.counterKey());
	}
}
