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
package com.gielinorgoals.ui;

import com.gielinorgoals.model.CombatAchievementRequirement;
import com.gielinorgoals.template.CombatAchievementSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.GameState;
import net.runelite.api.StructComposition;
import net.runelite.client.callback.ClientThread;

/**
 * Läser alla combat achievement-uppgifter ur spelets cache, så att listan alltid
 * följer spelet utan nätverksanrop. Verifierat mot clientscripten ca_tasks_init och
 * ca_tasks_draw_list: en enum per tier (easy → grandmaster) med struct-id:n, där
 * param 1306 är uppgiftens id, 1308 namnet och 1309 beskrivningen.
 */
@Singleton
public class CombatAchievementCatalog implements CombatAchievementSource
{
	static final int[] TIER_ENUMS = {3981, 3982, 3983, 3984, 3985, 3986};
	static final String[] TIER_NAMES = {"Easy", "Medium", "Hard", "Elite", "Master", "Grandmaster"};
	static final int PARAM_TASK_ID = 1306;
	static final int PARAM_NAME = 1308;
	static final int PARAM_DESCRIPTION = 1309;

	@Value
	public static class Task
	{
		int id;
		String name;
		String description;
		String tier;

		@Override
		public String toString()
		{
			return name + " (" + tier + ")";
		}
	}

	private final Client client;
	private final ClientThread clientThread;
	private final java.util.function.Supplier<List<Task>> loader;

	// Byggs och läses bara på klienttråden. Null tills uppgifterna har gått att läsa.
	private List<Task> tasks;

	@Inject
	CombatAchievementCatalog(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.loader = this::load;
	}

	// För tester: en egen källa i stället för spelets cache.
	CombatAchievementCatalog(java.util.function.Supplier<List<Task>> loader)
	{
		this.client = null;
		this.clientThread = null;
		this.loader = loader;
	}

	/**
	 * Söker asynkront; resultatet levereras på EDT.
	 */
	public void search(String query, int maxResults, Consumer<List<Task>> callback)
	{
		clientThread.invokeLater(() ->
		{
			List<Task> results = SearchRanking.rank(tasks(), Task::getName, query, maxResults);
			SwingUtilities.invokeLater(() -> callback.accept(results));
		});
	}

	/**
	 * Uppgifterna, lästa en gång. En tom lista (t.ex. om enumarna inte gick att läsa än)
	 * sparas inte, så att nästa sökning försöker igen i stället för att vara tom för gott.
	 * Klienttråden.
	 */
	List<Task> tasks()
	{
		if (tasks != null)
		{
			return tasks;
		}
		List<Task> loaded = loader.get();
		if (!loaded.isEmpty())
		{
			tasks = loaded;
		}
		return loaded;
	}

	/**
	 * Alla uppgifter som krav, för combat achievement-templaten. Utloggad ger null, eftersom
	 * templaten då inte kan visa vilka som redan är klara. Klienttråden.
	 */
	@Override
	@javax.annotation.Nullable
	public List<CombatAchievementRequirement> tasksIfLoggedIn()
	{
		if (client != null && client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}
		List<Task> all = tasks();
		if (all.isEmpty())
		{
			return null;
		}
		List<CombatAchievementRequirement> requirements = new ArrayList<>(all.size());
		for (Task task : all)
		{
			requirements.add(new CombatAchievementRequirement(task.getId(), task.getName(), task.getTier()));
		}
		return requirements;
	}

	private List<Task> load()
	{
		List<Task> loaded = new ArrayList<>();
		for (int tier = 0; tier < TIER_ENUMS.length; tier++)
		{
			EnumComposition tierEnum = client.getEnum(TIER_ENUMS[tier]);
			if (tierEnum == null)
			{
				continue;
			}

			for (int structId : tierEnum.getIntVals())
			{
				StructComposition struct = client.getStructComposition(structId);
				if (struct == null)
				{
					continue;
				}
				loaded.add(new Task(
					struct.getIntValue(PARAM_TASK_ID),
					struct.getStringValue(PARAM_NAME),
					struct.getStringValue(PARAM_DESCRIPTION),
					TIER_NAMES[tier]));
			}
		}
		return Collections.unmodifiableList(loaded);
	}
}
