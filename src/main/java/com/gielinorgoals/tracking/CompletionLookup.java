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

import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryTier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.client.callback.ClientThread;

/**
 * Läser in en fullständig {@link CompletionStatus} på klienttråden. Körs bara när en
 * dialog öppnas, så att ~200 quest-script per tick aldrig behövs.
 */
@Singleton
public class CompletionLookup
{
	private final Client client;
	private final ClientThread clientThread;

	@Inject
	CompletionLookup(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	/**
	 * Läser asynkront; resultatet levereras på EDT.
	 */
	public void load(Consumer<CompletionStatus> callback)
	{
		clientThread.invokeLater(() ->
		{
			CompletionStatus status = read();
			SwingUtilities.invokeLater(() -> callback.accept(status));
		});
	}

	private CompletionStatus read()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return CompletionStatus.UNKNOWN;
		}

		Map<Integer, QuestState> quests = new HashMap<>();
		for (Quest quest : Quest.values())
		{
			quests.put(quest.getId(), quest.getState(client));
		}

		Map<Integer, Integer> varbits = new HashMap<>();
		for (DiaryArea area : DiaryArea.values())
		{
			for (DiaryTier tier : DiaryTier.values())
			{
				int varbit = area.varbitFor(tier);
				varbits.put(varbit, client.getVarbitValue(varbit));
			}
		}

		int[] caVarps = new int[AchievementTracker.CA_VARPS.length];
		for (int i = 0; i < caVarps.length; i++)
		{
			caVarps[i] = client.getVarpValue(AchievementTracker.CA_VARPS[i]);
		}

		return new CompletionStatus(true, quests, varbits, caVarps);
	}
}
