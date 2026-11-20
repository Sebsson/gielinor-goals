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
 * Det spelläge som kraven läser. Implementationerna är oföränderliga ögonblicksbilder,
 * så att kraven kan utvärderas utanför klienttråden och testas utan klienten.
 * Fler metoder (quests, diaries …) läggs till i kommande milstolpar, som default-metoder
 * så att enkla lambdas i tester fortsätter att fungera.
 */
public interface GameData
{
	GameData EMPTY = skill -> -1;

	/**
	 * @return total XP i skillen, eller -1 om den inte är känd än
	 */
	long getXp(Skill skill);

	/**
	 * Antal av ett föremål i alla räknade förråd (inventory, equipment, bank och
	 * seed vault). Noterade föremål och placeholders är redan omräknade till grundföremålet.
	 *
	 * @return antal, eller -1 om föremål inte är kända än
	 */
	default long getItemCount(int itemId)
	{
		return -1;
	}

	/**
	 * @return när banken senast var öppen (epoch ms), eller null om den aldrig setts
	 */
	@Nullable
	default Long getBankSeenAt()
	{
		return null;
	}

	/**
	 * @return questens status, eller null om den inte är läst (t.ex. inte bevakad än)
	 */
	@Nullable
	default QuestState getQuestState(int questId)
	{
		return null;
	}

	/**
	 * @return varbitens värde, eller -1 om den inte är läst
	 */
	default int getVarbit(int varbitId)
	{
		return -1;
	}

	/**
	 * @return om uppgiften är klar, eller null om det inte är läst
	 */
	@Nullable
	default Boolean isCombatAchievementComplete(int taskId)
	{
		return null;
	}
}
