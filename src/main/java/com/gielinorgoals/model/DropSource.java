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

/**
 * Var ett föremål droppar och hur ofta: en KC-aktivitet (samma tre slag som
 * {@link KillCountRequirement}) och en drop rate 1/{@link #rate}. Sitter på ett item-mål
 * och ger torrhetsmätaren "1,240 kc · 1.6× rate". Räknaren delas med KC-mål på samma
 * aktivitet. Fälten serialiseras med Gson och får inte bytas namn.
 */
@Getter
@EqualsAndHashCode
public class DropSource
{
	public static final int MAX_RATE = 100_000_000;

	private KillCountRequirement.ActivityType activityType;
	private String activityId;
	private String activityName;
	// Nämnaren i drop rate, t.ex. 5000 för 1/5000.
	private int rate;

	// För Gson.
	DropSource()
	{
	}

	private DropSource(KillCountRequirement.ActivityType activityType, String activityId, String activityName, int rate)
	{
		if (rate < 1 || rate > MAX_RATE)
		{
			throw new IllegalArgumentException("Drop rate must be between 1/1 and 1/" + MAX_RATE);
		}
		this.activityType = activityType;
		this.activityId = activityId;
		this.activityName = activityName;
		this.rate = rate;
	}

	public static DropSource hiscore(HiscoreSkill skill, int rate)
	{
		return new DropSource(KillCountRequirement.ActivityType.HISCORE, skill.name(), skill.getName(), rate);
	}

	public static DropSource npc(String npcName, int rate)
	{
		String name = requireName(npcName, "Enter the NPC's name");
		return new DropSource(KillCountRequirement.ActivityType.NPC, name.toLowerCase(Locale.ROOT), name, rate);
	}

	public static DropSource manual(String counterName, int rate)
	{
		String name = requireName(counterName, "Give the counter a name");
		return new DropSource(KillCountRequirement.ActivityType.MANUAL, name.toLowerCase(Locale.ROOT), name, rate);
	}

	private static String requireName(String name, String message)
	{
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty())
		{
			throw new IllegalArgumentException(message);
		}
		return trimmed;
	}

	/**
	 * Samma nyckel som ett KC-mål på aktiviteten, t.ex. "hiscore:ZULRAH".
	 */
	public String counterKey()
	{
		return KillCountRequirement.counterKey(activityType, activityId);
	}

	@Nullable
	public HiscoreSkill hiscoreSkill()
	{
		if (activityType != KillCountRequirement.ActivityType.HISCORE)
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

	public boolean isValid()
	{
		if (activityType == null || activityId == null || activityId.isEmpty() || activityName == null
			|| rate < 1 || rate > MAX_RATE)
		{
			return false;
		}
		return activityType != KillCountRequirement.ActivityType.HISCORE || hiscoreSkill() != null;
	}

	/**
	 * Chansen att ha fått minst en drop efter {@code kc} försök: 1 − (1 − 1/rate)^kc.
	 * Räknas med log1p/expm1 så att stora rates och stora kc inte tappar precision.
	 */
	public static double chanceByNow(long kc, int rate)
	{
		if (kc <= 0 || rate < 1)
		{
			return 0;
		}
		if (rate == 1)
		{
			return 1;
		}
		return -Math.expm1(kc * Math.log1p(-1.0 / rate));
	}

	/**
	 * Hur många gånger raten man har gjort, t.ex. 1.6 för 8 000 kc på 1/5000.
	 */
	public static double rateMultiple(long kc, int rate)
	{
		return rate < 1 ? 0 : Math.max(0, kc) / (double) rate;
	}

	/**
	 * T.ex. "1/5,000 from Zulrah".
	 */
	public String describe()
	{
		return "1/" + net.runelite.client.util.QuantityFormatter.formatNumber(rate) + " from " + activityName;
	}
}
