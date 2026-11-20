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
import net.runelite.api.QuestState;

/**
 * Klara en quest. Questen sparas med sitt id (stabilt i spelet) och namn för visning,
 * så att målet överlever om RuneLite byter namn på konstanter i Quest-enumen.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class QuestRequirement extends Requirement
{
	private int questId;
	private String questName;

	// För Gson.
	QuestRequirement()
	{
	}

	public QuestRequirement(int questId, String questName)
	{
		this.questId = questId;
		this.questName = questName;
	}

	@Override
	public RequirementType getType()
	{
		return RequirementType.QUEST;
	}

	@Override
	public String describe()
	{
		return "Complete " + (questName == null || questName.isEmpty() ? "quest " + questId : questName);
	}

	@Override
	public boolean isValid()
	{
		return questId >= 0;
	}

	@Nullable
	@Override
	public GoalProgress progress(GameData data)
	{
		QuestState state = data.getQuestState(questId);
		if (state == null)
		{
			return null;
		}

		switch (state)
		{
			case FINISHED:
				return new GoalProgress(2, 2, true, "Finished");
			case IN_PROGRESS:
				return new GoalProgress(1, 2, false, "In progress");
			case NOT_STARTED:
			default:
				return new GoalProgress(0, 2, false, "Not started");
		}
	}
}
