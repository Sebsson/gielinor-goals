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
 * Klara en combat achievement-uppgift. Task-id är det id spelet använder för
 * klarmarkeringen (struct-param 1306); namn och tier sparas för visning.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class CombatAchievementRequirement extends Requirement
{
	private int taskId;
	private String taskName;
	private String tierName;

	// För Gson.
	CombatAchievementRequirement()
	{
	}

	public CombatAchievementRequirement(int taskId, String taskName, String tierName)
	{
		this.taskId = taskId;
		this.taskName = taskName;
		this.tierName = tierName;
	}

	@Override
	public RequirementType getType()
	{
		return RequirementType.COMBAT_ACHIEVEMENT;
	}

	@Override
	public String describe()
	{
		String name = taskName == null || taskName.isEmpty() ? "Task " + taskId : taskName;
		return tierName == null || tierName.isEmpty() ? "CA: " + name : "CA (" + tierName + "): " + name;
	}

	@Override
	public boolean isValid()
	{
		return taskId >= 0;
	}

	@Nullable
	@Override
	public GoalProgress progress(GameData data)
	{
		Boolean complete = data.isCombatAchievementComplete(taskId);
		if (complete == null)
		{
			return null;
		}
		return new GoalProgress(complete ? 1 : 0, 1, complete, complete ? "Done" : "Not done");
	}

	// Namn och tier är bara för visning.
	@Override
	public boolean isSameGoal(@Nullable Requirement other)
	{
		return other instanceof CombatAchievementRequirement && ((CombatAchievementRequirement) other).taskId == taskId;
	}
}
