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
	// Antal föremål per drop, t.ex. 3 Hueycoatl hides; 1 för de flesta.
	private int perDrop = 1;
	// Raten är en uppskattning, t.ex. en raid där chansen beror på poäng. Null i äldre data
	// och för exakta rates.
	@Nullable
	private Boolean approximate;
	// Varför raten är ungefärlig; visas i hover-texten.
	@Nullable
	private String approximateNote;

	public static final String DEFAULT_APPROXIMATE_NOTE = "The chance depends on how the activity goes, so this rate"
		+ " is an estimate.";
	// Samma gräns som texter i delade templates.
	public static final int MAX_NOTE_LENGTH = 200;

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

	public static final int MAX_PER_DROP = 100_000;

	/**
	 * Samma källa med ett annat antal föremål per drop.
	 */
	public DropSource withPerDrop(int perDrop)
	{
		if (perDrop < 1 || perDrop > MAX_PER_DROP)
		{
			throw new IllegalArgumentException("perDrop must be between 1 and " + MAX_PER_DROP);
		}
		DropSource copy = copy();
		copy.perDrop = perDrop;
		return copy;
	}

	/**
	 * Samma källa med en ungefärlig rate. Torrhetsmätaren visar då "≈" och hover-texten
	 * förklarar varför med {@code note} (eller en standardtext om den är tom).
	 */
	public DropSource withApproximate(@Nullable String note)
	{
		String trimmed = note == null ? null : note.trim();
		if (trimmed != null && trimmed.length() > MAX_NOTE_LENGTH)
		{
			throw new IllegalArgumentException("The approximate note can be at most " + MAX_NOTE_LENGTH + " characters");
		}
		DropSource copy = copy();
		copy.approximate = true;
		copy.approximateNote = trimmed == null || trimmed.isEmpty() ? null : trimmed;
		return copy;
	}

	private DropSource copy()
	{
		DropSource copy = new DropSource(activityType, activityId, activityName, rate);
		copy.perDrop = perDrop;
		copy.approximate = approximate;
		copy.approximateNote = approximateNote;
		return copy;
	}

	public boolean isApproximate()
	{
		return Boolean.TRUE.equals(approximate);
	}

	/**
	 * Förklaringen till en ungefärlig rate, eller null för en exakt.
	 */
	@Nullable
	public String getApproximateNote()
	{
		if (!isApproximate())
		{
			return null;
		}
		return approximateNote == null ? DEFAULT_APPROXIMATE_NOTE : approximateNote;
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

	// Äldre sparad data utan fältet räknas som 1 per drop.
	public int getPerDrop()
	{
		return Math.max(1, perDrop);
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
	 * Chansen att ha fått minst {@code drops} drops efter {@code kc} försök (binomialfördelning).
	 * Termerna räknas fram en i taget från P(0) = (1 − p)^kc, så att stora kc inte svämmar över.
	 */
	public static double chanceAtLeast(long kc, int rate, int drops)
	{
		if (drops <= 1)
		{
			return chanceByNow(kc, rate);
		}
		if (kc < drops || rate < 1)
		{
			return 0;
		}
		if (rate == 1)
		{
			return 1;
		}
		double p = 1.0 / rate;
		double term = Math.exp(kc * Math.log1p(-p));
		double fewer = 0;
		for (int k = 0; k < drops; k++)
		{
			fewer += term;
			term *= (kc - k) / (double) (k + 1) * p / (1 - p);
		}
		return Math.max(0, Math.min(1, 1 - fewer));
	}

	/**
	 * Hur många gånger raten man har gjort, t.ex. 1.6 för 8 000 kc på 1/5000.
	 */
	public static double rateMultiple(long kc, int rate)
	{
		return rate < 1 ? 0 : Math.max(0, kc) / (double) rate;
	}

	/**
	 * T.ex. "1/5,000 from Zulrah", eller "≈1/500 from Chambers of Xeric" för en ungefärlig rate.
	 */
	public String describe()
	{
		return (isApproximate() ? "\u2248" : "") + "1/" + net.runelite.client.util.QuantityFormatter.formatNumber(rate) + " from " + activityName
			+ (getPerDrop() > 1 ? ", " + getPerDrop() + " per drop" : "");
	}
}
