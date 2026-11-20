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
 * Klara en nivå i en achievement diary.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class DiaryRequirement extends Requirement
{
	private DiaryArea area;
	private DiaryTier tier;

	// För Gson.
	DiaryRequirement()
	{
	}

	public DiaryRequirement(DiaryArea area, DiaryTier tier)
	{
		this.area = area;
		this.tier = tier;
	}

	@Override
	public RequirementType getType()
	{
		return RequirementType.DIARY;
	}

	@Override
	public String describe()
	{
		return area.getDisplayName() + " " + tier.getDisplayName() + " diary";
	}

	@Override
	public boolean isValid()
	{
		return area != null && tier != null;
	}

	public int varbit()
	{
		return area.varbitFor(tier);
	}

	/**
	 * Varbit med antal klara uppgifter i nivån, för progress som "7/10 tasks".
	 */
	public int countVarbit()
	{
		return area.countVarbitFor(tier);
	}

	@Nullable
	@Override
	public GoalProgress progress(GameData data)
	{
		int value = data.getVarbit(varbit());
		if (value < 0)
		{
			return null;
		}

		boolean complete = value >= area.completeValueFor(tier);
		int total = area.taskTotalFor(tier);
		int done = data.getVarbit(countVarbit());
		if (done < 0)
		{
			return new GoalProgress(complete ? 1 : 0, 1, complete, complete ? "Done" : "Not done");
		}

		// Om Jagex lägger till uppgifter kan räknaren bli större än vår totalsiffra.
		total = Math.max(total, done);
		if (complete)
		{
			done = total;
		}
		return new GoalProgress(done, total, complete, done + "/" + total + " tasks");
	}
}
