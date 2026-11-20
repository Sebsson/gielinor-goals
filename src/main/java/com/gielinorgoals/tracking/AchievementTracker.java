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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.VarPlayerID;

/**
 * Läser quest-status, diary-varbits och CA-flaggor för de mål som bevakas.
 * Körs på klienttråden. VarbitChanged kommer mycket ofta, så eventet markerar bara
 * att något kan ha ändrats; läsningen sker på nästa tick och quests (som kräver ett
 * script per quest) läses högst var femte tick.
 */
@Singleton
public class AchievementTracker
{
	static final int QUEST_READ_INTERVAL_TICKS = 5;

	// Varps med klarmarkeringar för CA-uppgifter: bit (id % 32) i varp nummer (id / 32).
	static final int[] CA_VARPS = {
		VarPlayerID.CA_TASK_COMPLETED_0, VarPlayerID.CA_TASK_COMPLETED_1, VarPlayerID.CA_TASK_COMPLETED_2,
		VarPlayerID.CA_TASK_COMPLETED_3, VarPlayerID.CA_TASK_COMPLETED_4, VarPlayerID.CA_TASK_COMPLETED_5,
		VarPlayerID.CA_TASK_COMPLETED_6, VarPlayerID.CA_TASK_COMPLETED_7, VarPlayerID.CA_TASK_COMPLETED_8,
		VarPlayerID.CA_TASK_COMPLETED_9, VarPlayerID.CA_TASK_COMPLETED_10, VarPlayerID.CA_TASK_COMPLETED_11,
		VarPlayerID.CA_TASK_COMPLETED_12, VarPlayerID.CA_TASK_COMPLETED_13, VarPlayerID.CA_TASK_COMPLETED_14,
		VarPlayerID.CA_TASK_COMPLETED_15, VarPlayerID.CA_TASK_COMPLETED_16, VarPlayerID.CA_TASK_COMPLETED_17,
		VarPlayerID.CA_TASK_COMPLETED_18, VarPlayerID.CA_TASK_COMPLETED_19, VarPlayerID.CA_TASK_COMPLETED_20,
	};

	/**
	 * Oföränderlig ögonblicksbild av de bevakade värdena.
	 */
	@Value
	public static class Achievements
	{
		public static final Achievements NONE = new Achievements(
			Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());

		Map<Integer, QuestState> quests;
		Map<Integer, Integer> varbits;
		Map<Integer, Boolean> combatAchievements;
	}

	private static final Map<Integer, Quest> QUESTS_BY_ID = new HashMap<>();

	static
	{
		for (Quest quest : Quest.values())
		{
			QUESTS_BY_ID.put(quest.getId(), quest);
		}
	}

	private final Client client;

	// Sätts från EDT, läses på klienttråden.
	private volatile WatchedAchievements watched = WatchedAchievements.NONE;

	private WatchedAchievements lastWatched;
	private Map<Integer, QuestState> questStates = Collections.emptyMap();
	private Achievements last;
	private boolean dirty;
	private int ticksSinceQuestRead;

	@Inject
	AchievementTracker(Client client)
	{
		this.client = client;
	}

	public void setWatched(WatchedAchievements watched)
	{
		this.watched = watched;
	}

	public void reset()
	{
		lastWatched = null;
		questStates = Collections.emptyMap();
		last = null;
		dirty = true;
		ticksSinceQuestRead = QUEST_READ_INTERVAL_TICKS;
	}

	public void onVarChanged()
	{
		dirty = true;
	}

	public void onGameStateChanged(GameState state)
	{
		if (state == GameState.LOGGED_IN)
		{
			dirty = true;
		}
		else if (state == GameState.LOGIN_SCREEN)
		{
			reset();
		}
	}

	/**
	 * @return nya värden om något bevakat har ändrats sedan förra anropet, annars null
	 */
	@Nullable
	public Achievements onGameTick()
	{
		ticksSinceQuestRead++;
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}

		WatchedAchievements current = watched;
		boolean watchedChanged = !current.equals(lastWatched);
		if (!dirty && !watchedChanged)
		{
			return null;
		}

		if (watchedChanged || ticksSinceQuestRead >= QUEST_READ_INTERVAL_TICKS)
		{
			questStates = readQuests(current);
			ticksSinceQuestRead = 0;
			// Varbits och CA läses varje tick; dirty släpps först när även quests är lästa.
			dirty = false;
		}
		lastWatched = current;

		Achievements achievements = new Achievements(questStates, readVarbits(current), readCombatAchievements(current));
		if (achievements.equals(last))
		{
			return null;
		}
		last = achievements;
		return achievements;
	}

	private Map<Integer, QuestState> readQuests(WatchedAchievements current)
	{
		Map<Integer, QuestState> states = new HashMap<>();
		for (int questId : current.getQuestIds())
		{
			Quest quest = QUESTS_BY_ID.get(questId);
			if (quest != null)
			{
				states.put(questId, quest.getState(client));
			}
		}
		return Collections.unmodifiableMap(states);
	}

	private Map<Integer, Integer> readVarbits(WatchedAchievements current)
	{
		Map<Integer, Integer> values = new HashMap<>();
		for (int varbit : current.getVarbits())
		{
			values.put(varbit, client.getVarbitValue(varbit));
		}
		return Collections.unmodifiableMap(values);
	}

	private Map<Integer, Boolean> readCombatAchievements(WatchedAchievements current)
	{
		Map<Integer, Boolean> values = new HashMap<>();
		for (int taskId : current.getCombatAchievementIds())
		{
			Boolean complete = isTaskComplete(taskId, varpIndex -> client.getVarpValue(CA_VARPS[varpIndex]));
			if (complete != null)
			{
				values.put(taskId, complete);
			}
		}
		return Collections.unmodifiableMap(values);
	}

	/**
	 * @return om uppgiften är klar, eller null om id:t ligger utanför de kända varps
	 */
	@Nullable
	static Boolean isTaskComplete(int taskId, IntUnaryOperator varpByIndex)
	{
		int index = taskId / 32;
		if (taskId < 0 || index >= CA_VARPS.length)
		{
			return null;
		}
		return (varpByIndex.applyAsInt(index) & (1 << (taskId % 32))) != 0;
	}

	/**
	 * Kända quests, för att lista dem i UI:t.
	 */
	public static Map<Integer, Quest> questsById()
	{
		return Collections.unmodifiableMap(QUESTS_BY_ID);
	}
}
