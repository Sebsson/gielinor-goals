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

/**
 * Nå ett visst antal quest points, läst från spelets varp för quest points.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class QuestPointsRequirement extends Requirement
{
	public static final int MIN = 1;
	// Spelet har drygt 300 quest points i dag; gränsen lämnar gott om plats för nya quests.
	public static final int MAX = 500;

	private int target;

	// För Gson.
	QuestPointsRequirement()
	{
	}

	public QuestPointsRequirement(int target)
	{
		if (target < MIN || target > MAX)
		{
			throw new IllegalArgumentException("Quest points must be between " + MIN + " and " + MAX);
		}
		this.target = target;
	}

	@Override
	public RequirementType getType()
	{
		return RequirementType.QUEST_POINTS;
	}

	@Override
	public String describe()
	{
		return target + " quest points";
	}

	@Override
	public boolean isValid()
	{
		return target >= MIN && target <= MAX;
	}

	@Nullable
	@Override
	public GoalProgress progress(GameData data)
	{
		// Okänd data ger okänd progress, så att målet aldrig bockas av på saknad data.
		int current = data.getQuestPoints();
		return current < 0 ? null : AccountLevels.progress(current, target);
	}

	@Override
	public String describeProgress(GoalProgress progress)
	{
		return AccountLevels.describe("Quest points ", progress, "quest point", "quest points");
	}
}
