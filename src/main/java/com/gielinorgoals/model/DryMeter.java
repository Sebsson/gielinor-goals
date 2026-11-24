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
import net.runelite.client.util.QuantityFormatter;

/**
 * Texterna för torrhetsmätaren på item-mål med {@link DropSource}. Rena funktioner, så att
 * de kan testas utan UI. Ett mål på flera föremål (t.ex. 4 zenyte shards) räknas om till
 * antal drops som behövs, med hänsyn till hur många föremål en drop ger.
 */
public final class DryMeter
{
	// Fler drops än så blir ingen meningsfull mätare (och sannolikhetssumman blir lång).
	static final int MAX_DROPS = 1000;

	private DryMeter()
	{
	}

	/**
	 * Drop-källan om målet ska visa en torrhetsmätare: ett item-mål med giltig källa.
	 */
	@Nullable
	public static DropSource sourceOf(Goal goal)
	{
		DropSource source = goal.getDropSource();
		if (source == null || !source.isValid() || !(goal.getRequirement() instanceof ItemRequirement)
			|| dropsNeeded(goal, source) > MAX_DROPS)
		{
			return null;
		}
		return source;
	}

	/**
	 * Antal drops som behövs för målets antal, t.ex. 2 för 6 föremål med 3 per drop.
	 */
	static int dropsNeeded(Goal goal, DropSource source)
	{
		long quantity = ((ItemRequirement) goal.getRequirement()).getQuantity();
		long drops = (quantity + source.getPerDrop() - 1) / source.getPerDrop();
		return (int) Math.min(Integer.MAX_VALUE, Math.max(1, drops));
	}

	/**
	 * T.ex. "1,240 kc · 1.6× rate". Med flera drops jämförs mot raten gånger antalet drops.
	 * Visas bara medan målet inte är klart och räknaren är känd.
	 *
	 * @param kc räknarens värde, -1 om det inte är känt
	 */
	@Nullable
	public static String rowText(Goal goal, long kc)
	{
		DropSource source = sourceOf(goal);
		if (source == null || goal.isCompleted() || kc < 0)
		{
			return null;
		}
		double multiple = DropSource.rateMultiple(kc, source.getRate()) / dropsNeeded(goal, source);
		// KC är exakt, men andelen av raten är bara ungefärlig när raten är det.
		return QuantityFormatter.formatNumber(kc) + " kc · " + approx(source)
			+ String.format(Locale.ROOT, "%.1f", multiple) + "× rate";
	}

	/**
	 * T.ex. "Chance to have received it by now: 71%" eller, för flera drops,
	 * "Chance to have all 4 by now: 23%".
	 */
	@Nullable
	public static String chanceText(Goal goal, long kc)
	{
		DropSource source = sourceOf(goal);
		if (source == null || goal.isCompleted() || kc < 0)
		{
			return null;
		}
		int drops = dropsNeeded(goal, source);
		boolean certain = source.getRate() == 1 && kc >= drops;
		String percent = approx(source)
			+ formatPercent(DropSource.chanceAtLeast(kc, source.getRate(), drops), certain);
		long quantity = ((ItemRequirement) goal.getRequirement()).getQuantity();
		return quantity == 1
			? "Chance to have received it by now: " + percent
			: "Chance to have all " + QuantityFormatter.formatNumber(quantity) + " by now: " + percent;
	}

	// "≈" framför värden som bygger på en ungefärlig rate.
	private static String approx(DropSource source)
	{
		return source.isApproximate() ? "\u2248" : "";
	}

	/**
	 * T.ex. "Got it at 1,240 kc" för ett klart mål där KC sparades. Upptäcktes målet först
	 * vid inloggning är KC räknarens värde då, så det visas som ungefärligt.
	 */
	@Nullable
	public static String completedText(Goal goal)
	{
		if (sourceOf(goal) == null || !goal.isCompleted() || goal.getObtainedAtKc() == null)
		{
			return null;
		}
		return "Got it at " + (goal.isCompletedAtApprox() ? "about " : "")
			+ QuantityFormatter.formatNumber(goal.getObtainedAtKc()) + " kc";
	}

	/**
	 * Hela procent, men aldrig "0%" för en chans över noll eller "100%" om dropen inte är
	 * garanterad, eftersom det skulle se ut som säkert. Vid enormt kc blir chansen 1,0 i
	 * flyttal fast den egentligen är strax under; därför avgör {@code certain}, inte talet.
	 */
	static String formatPercent(double chance, boolean certain)
	{
		if (certain)
		{
			return "100%";
		}
		if (chance <= 0)
		{
			return "0%";
		}
		long rounded = Math.round(chance * 100);
		if (rounded < 1)
		{
			return "<1%";
		}
		if (rounded > 99)
		{
			return ">99%";
		}
		return rounded + "%";
	}
}
