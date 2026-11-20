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
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.GoalProgress;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nullable;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/**
 * Ögonblicksbild av ALLA quests, diary-nivåer och CA-flaggor, till skillnad från
 * AchievementTracker som bara läser bevakade mål. Används av dialogen för att dölja
 * sådant som redan är klart. Implementerar GameData så att kravens egen logik avgör
 * vad som räknas som klart.
 */
public final class CompletionStatus implements GameData
{
	public static final CompletionStatus UNKNOWN = new CompletionStatus(false,
		Collections.emptyMap(), Collections.emptyMap(), new int[0]);

	private final boolean known;
	private final Map<Integer, QuestState> quests;
	private final Map<Integer, Integer> varbits;
	private final int[] caVarps;

	public CompletionStatus(boolean known, Map<Integer, QuestState> quests, Map<Integer, Integer> varbits,
		int[] caVarps)
	{
		this.known = known;
		this.quests = Collections.unmodifiableMap(quests);
		this.varbits = Collections.unmodifiableMap(varbits);
		this.caVarps = caVarps.clone();
	}

	/**
	 * @return false före inloggning; då döljs ingenting
	 */
	public boolean isKnown()
	{
		return known;
	}

	/**
	 * @return true om kravet redan är uppfyllt enligt spelet
	 */
	public boolean isAlreadyComplete(Requirement requirement)
	{
		if (!known)
		{
			return false;
		}
		GoalProgress progress = requirement.progress(this);
		return progress != null && progress.isComplete();
	}

	@Override
	public long getXp(Skill skill)
	{
		return -1;
	}

	@Nullable
	@Override
	public QuestState getQuestState(int questId)
	{
		return quests.get(questId);
	}

	@Override
	public int getVarbit(int varbitId)
	{
		return varbits.getOrDefault(varbitId, -1);
	}

	@Nullable
	@Override
	public Boolean isCombatAchievementComplete(int taskId)
	{
		if (!known)
		{
			return null;
		}
		return AchievementTracker.isTaskComplete(taskId,
			index -> index < caVarps.length ? caVarps[index] : 0);
	}
}
