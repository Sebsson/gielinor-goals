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
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;

/**
 * Håller koll på XP per skill. Alla metoder körs på klienttråden. Ändringar samlas
 * ihop och lämnas ut som en ögonblicksbild högst en gång per game tick, så att de
 * ~24 StatChanged-eventen vid inloggning bara ger en utvärdering.
 */
@Singleton
public class SkillTracker
{
	// Klientens läge och XP; utbytbart i tester.
	private final Supplier<GameState> gameState;
	private final ToIntFunction<Skill> experience;
	private final Map<Skill, Long> xp = new EnumMap<>(Skill.class);
	private boolean dirty;
	private boolean needFullRead;

	@Inject
	SkillTracker(Client client)
	{
		this(client::getGameState, client::getSkillExperience);
	}

	SkillTracker(Supplier<GameState> gameState, ToIntFunction<Skill> experience)
	{
		this.gameState = gameState;
		this.experience = experience;
	}

	public void reset(boolean loggedIn)
	{
		xp.clear();
		dirty = true;
		needFullRead = loggedIn;
	}

	public void onStatChanged(Skill skill, int newXp)
	{
		xp.put(skill, (long) newXp);
		dirty = true;
	}

	public void onGameStateChanged(GameState state)
	{
		if (state == GameState.LOGGED_IN)
		{
			// Statsen kommer inte alltid som events, så läs allt vid nästa tick.
			needFullRead = true;
		}
		else if (state == GameState.LOGIN_SCREEN)
		{
			// Glöm förra kontots värden så att de aldrig utvärderas mot en ny profil.
			reset(false);
		}
	}

	/**
	 * @return en ny ögonblicksbild om något har ändrats sedan förra anropet, annars null
	 */
	@Nullable
	public Map<Skill, Long> onGameTick()
	{
		if (needFullRead && gameState.get() == GameState.LOGGED_IN)
		{
			for (Skill skill : Skill.values())
			{
				if (isTrackable(skill))
				{
					xp.put(skill, (long) experience.applyAsInt(skill));
				}
			}
			needFullRead = false;
			dirty = true;
		}

		if (!dirty)
		{
			return null;
		}

		dirty = false;
		return Collections.unmodifiableMap(new EnumMap<>(xp));
	}

	@SuppressWarnings("deprecation")
	public static boolean isTrackable(Skill skill)
	{
		// OVERALL är ingen riktig skill och saknar egen XP i klienten.
		return skill != Skill.OVERALL;
	}
}
