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

/**
 * Minst N poäng i en aktivitet, t.ex. 750 slayer points, läst från spelets varbit eller varp
 * (se {@link PointsActivity}).
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class PointsRequirement extends Requirement
{
	public static final int MIN = 1;
	public static final int MAX = 10_000_000;

	private PointsActivity activity;
	private int target;

	// För Gson.
	PointsRequirement()
	{
	}

	public PointsRequirement(PointsActivity activity, int target)
	{
		if (activity == null)
		{
			throw new IllegalArgumentException("Pick an activity");
		}
		if (target < MIN || target > MAX)
		{
			throw new IllegalArgumentException("Points must be between " + MIN + " and " + MAX);
		}
		this.activity = activity;
		this.target = target;
	}

	@Override
	public RequirementType getType()
	{
		return RequirementType.POINTS;
	}

	@Override
	public String describe()
	{
		return target + " " + (activity == null ? "points" : activity.getDisplayName());
	}

	@Override
	public boolean isValid()
	{
		return activity != null && target >= MIN && target <= MAX;
	}

	@Nullable
	@Override
	public GoalProgress progress(GameData data)
	{
		// Okänd data ger okänd progress, så att målet aldrig bockas av på saknad data.
		int current = activity == null ? -1 : activity.read(data);
		return current < 0 ? null : AccountLevels.progress(current, target);
	}

	@Override
	public String describeProgress(GoalProgress progress)
	{
		String name = activity == null ? "Points" : activity.getDisplayName();
		// "Slayer points 320/750 (430 points left)"; ordet efter siffran är aktivitetens sista ord.
		String[] words = name.split(" ");
		String plural = words[words.length - 1].toLowerCase(Locale.ROOT);
		String singular = plural.endsWith("s") ? plural.substring(0, plural.length() - 1) : plural;
		return AccountLevels.describe(name + " ", progress, singular, plural);
	}
}
