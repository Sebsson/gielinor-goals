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
 * Spelläget kompletterat med profilens räknare, så att KC-mål kan utvärderas tillsammans
 * med allt annat. Räknarna finns även utan spelläge, t.ex. för egna räknare när man är utloggad.
 */
public final class CountedGameData implements GameData
{
	private final GameData data;
	private final ActivityCounters counters;

	public CountedGameData(GameData data, ActivityCounters counters)
	{
		this.data = data;
		this.counters = counters;
	}

	@Override
	public long getXp(Skill skill)
	{
		return data.getXp(skill);
	}

	@Override
	public long getItemCount(int itemId)
	{
		return data.getItemCount(itemId);
	}

	@Nullable
	@Override
	public Long getBankSeenAt()
	{
		return data.getBankSeenAt();
	}

	@Nullable
	@Override
	public QuestState getQuestState(int questId)
	{
		return data.getQuestState(questId);
	}

	@Override
	public int getVarbit(int varbitId)
	{
		return data.getVarbit(varbitId);
	}

	@Override
	public int getQuestPoints()
	{
		return data.getQuestPoints();
	}

	@Nullable
	@Override
	public Boolean isCombatAchievementComplete(int taskId)
	{
		return data.isCombatAchievementComplete(taskId);
	}

	@Override
	public long getActivityCount(String counterKey)
	{
		return counters.value(counterKey);
	}
}
