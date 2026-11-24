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
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/**
 * Spelläget med fullständig quest-, diary- och CA-status ovanpå, t.ex. för förhandsvisningen
 * av en template: där finns quests som ännu inte är mål, och som därför inte bevakas.
 */
public final class CompletionOverlay implements GameData
{
	private final GameData base;
	private final GameData status;

	/**
	 * @param status fullständig status, t.ex. från CompletionLookup; värden den inte känner till
	 *               tas från {@code base}
	 */
	public CompletionOverlay(GameData base, GameData status)
	{
		this.base = base;
		this.status = status;
	}

	@Override
	public long getXp(Skill skill)
	{
		return base.getXp(skill);
	}

	@Override
	public long getItemCount(int itemId)
	{
		return base.getItemCount(itemId);
	}

	@Nullable
	@Override
	public Long getBankSeenAt()
	{
		return base.getBankSeenAt();
	}

	@Nullable
	@Override
	public QuestState getQuestState(int questId)
	{
		QuestState state = status.getQuestState(questId);
		return state != null ? state : base.getQuestState(questId);
	}

	@Override
	public int getVarbit(int varbitId)
	{
		int value = status.getVarbit(varbitId);
		return value >= 0 ? value : base.getVarbit(varbitId);
	}

	@Override
	public int getVarp(int varpId)
	{
		return base.getVarp(varpId);
	}

	@Override
	public long getActivityCount(String counterKey)
	{
		return base.getActivityCount(counterKey);
	}

	@Override
	public int getQuestPoints()
	{
		return base.getQuestPoints();
	}

	@Nullable
	@Override
	public Boolean isCombatAchievementComplete(int taskId)
	{
		Boolean complete = status.isCombatAchievementComplete(taskId);
		return complete != null ? complete : base.isCombatAchievementComplete(taskId);
	}
}
