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
package com.gielinorgoals.tracking;

import com.gielinorgoals.model.GameData;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/**
 * Oföränderlig kombination av senaste data från alla trackers.
 */
public class GameSnapshot implements GameData
{
	/**
	 * Föremålsdelen: totalsummor per grundföremål och när banken senast sågs.
	 */
	@Value
	public static class Items
	{
		public static final Items UNKNOWN = new Items(null, null);

		// Null tills inventory har lästs efter inloggning.
		@Nullable
		Map<Integer, Long> totals;
		@Nullable
		Long bankSeenAt;
	}

	private final Map<Skill, Long> xp;
	private final Items items;
	private final AchievementTracker.Achievements achievements;

	public GameSnapshot(Map<Skill, Long> xp, Items items, AchievementTracker.Achievements achievements)
	{
		this.xp = Collections.unmodifiableMap(xp);
		this.items = items;
		this.achievements = achievements;
	}

	@Nullable
	@Override
	public QuestState getQuestState(int questId)
	{
		return achievements.getQuests().get(questId);
	}

	@Override
	public int getVarbit(int varbitId)
	{
		return achievements.getVarbits().getOrDefault(varbitId, -1);
	}

	@Override
	public int getVarp(int varpId)
	{
		return achievements.getVarps().getOrDefault(varpId, -1);
	}

	@Override
	public int getQuestPoints()
	{
		return achievements.getQuestPoints();
	}

	@Nullable
	@Override
	public Boolean isCombatAchievementComplete(int taskId)
	{
		return achievements.getCombatAchievements().get(taskId);
	}

	@Override
	public long getXp(Skill skill)
	{
		return xp.getOrDefault(skill, -1L);
	}

	@Override
	public long getItemCount(int itemId)
	{
		Map<Integer, Long> totals = items.getTotals();
		return totals == null ? -1 : totals.getOrDefault(itemId, 0L);
	}

	@Nullable
	@Override
	public Long getBankSeenAt()
	{
		return items.getBankSeenAt();
	}
}
