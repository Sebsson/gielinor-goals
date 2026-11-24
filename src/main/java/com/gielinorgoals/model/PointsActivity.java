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
import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Aktiviteter vars poäng spelet håller i en varbit eller varp, så att ett POINTS-mål kan läsa
 * värdet direkt. Listan är kuraterad: bara variabler vars namn i gameval tydligt är
 * aktivitetens poängsaldo. Aktiviteter utan en sådan variabel (t.ex. Pest Control och
 * Mahogany Homes) förblir manuella räknare.
 * <p>
 * Namnen sparas i JSON och får aldrig bytas utan migrering.
 */
@Getter
public enum PointsActivity
{
	SLAYER("Slayer points", Source.VARBIT, VarbitID.SLAYER_POINTS, "Slayer reward point", Skill.SLAYER),
	TITHE_FARM("Tithe Farm points", Source.VARBIT, VarbitID.HOSIDIUS_TITHE_REWARDPOINTS, "Tithe Farm", Skill.FARMING),
	GIANTS_FOUNDRY("Giants' Foundry reputation", Source.VARP, VarPlayerID.GIANTS_FOUNDRY_REWARD_SHOP_POINTS,
		"Giants' Foundry", Skill.SMITHING),
	TEMPOROSS("Tempoross reward permits", Source.VARBIT, VarbitID.TEMPOROSS_REWARDPERMITS, "Reward permit",
		Skill.FISHING),
	SOUL_WARS("Soul Wars zeal", Source.VARP, VarPlayerID.SOUL_WARS_ZEAL, "Zeal", null),
	NIGHTMARE_ZONE("Nightmare Zone reward points", Source.VARP, VarPlayerID.NZONE_REWARDPOINTS,
		"Nightmare Zone/Rewards", null);

	/**
	 * Var värdet finns: en varbit eller en hel varp.
	 */
	public enum Source
	{
		VARBIT,
		VARP
	}

	private final String displayName;
	private final Source source;
	private final int id;
	private final String wikiPage;
	// Skill-ikonen som visas för målet; null ger minigame-ikonen.
	@Nullable
	private final Skill iconSkill;

	PointsActivity(String displayName, Source source, int id, String wikiPage, @Nullable Skill iconSkill)
	{
		this.displayName = displayName;
		this.source = source;
		this.id = id;
		this.wikiPage = wikiPage;
		this.iconSkill = iconSkill;
	}

	/**
	 * @return poängen, eller -1 om värdet inte är läst
	 */
	public int read(GameData data)
	{
		return source == Source.VARBIT ? data.getVarbit(id) : data.getVarp(id);
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
