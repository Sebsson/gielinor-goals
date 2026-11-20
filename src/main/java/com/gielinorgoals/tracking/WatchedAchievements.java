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

import com.gielinorgoals.model.CombatAchievementRequirement;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.Value;

/**
 * Vilka quests, varbits och CA-uppgifter som har mål och därför behöver läsas.
 * Att bara läsa det som bevakas håller nere antalet quest-script per tick.
 */
@Value
public class WatchedAchievements
{
	public static final WatchedAchievements NONE = new WatchedAchievements(
		Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

	Set<Integer> questIds;
	Set<Integer> varbits;
	Set<Integer> combatAchievementIds;

	public boolean isEmpty()
	{
		return questIds.isEmpty() && varbits.isEmpty() && combatAchievementIds.isEmpty();
	}

	public static WatchedAchievements from(GoalTree tree)
	{
		Set<Integer> quests = new HashSet<>();
		Set<Integer> varbits = new HashSet<>();
		Set<Integer> tasks = new HashSet<>();
		tree.forEach(goal ->
		{
			Requirement requirement = goal.getRequirement();
			if (requirement instanceof QuestRequirement)
			{
				quests.add(((QuestRequirement) requirement).getQuestId());
			}
			else if (requirement instanceof DiaryRequirement)
			{
				DiaryRequirement diary = (DiaryRequirement) requirement;
				varbits.add(diary.varbit());
				varbits.add(diary.countVarbit());
			}
			else if (requirement instanceof CombatAchievementRequirement)
			{
				tasks.add(((CombatAchievementRequirement) requirement).getTaskId());
			}
		});
		return new WatchedAchievements(Collections.unmodifiableSet(quests), Collections.unmodifiableSet(varbits),
			Collections.unmodifiableSet(tasks));
	}
}
