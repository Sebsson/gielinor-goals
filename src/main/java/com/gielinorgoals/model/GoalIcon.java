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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.runelite.client.hiscore.HiscoreSkill;

/**
 * En egen ikon för ett mål utan krav, t.ex. en grupp i en template: en föremålsbild, en
 * boss- eller aktivitetsikon från hiscores, eller en av spelets sidoflikar. Mål med krav
 * får kravets ikon i stället. Fälten serialiseras med Gson och får inte bytas namn.
 */
@Getter
@EqualsAndHashCode
public class GoalIcon
{
	/**
	 * Sidoflikarnas ikoner som kan väljas; kopplas till sprites i UI-lagret.
	 */
	public enum SideIcon
	{
		QUEST,
		STATS,
		COMBAT,
		INVENTORY,
		EQUIPMENT,
		ACHIEVEMENT_DIARIES,
		MINIGAMES
	}

	// Exakt ett av fälten är satt.
	private Integer itemId;
	// Namnet på en HiscoreSkill, t.ex. "ZULRAH".
	private String activity;
	private SideIcon sideIcon;

	// För Gson.
	GoalIcon()
	{
	}

	public static GoalIcon item(int itemId)
	{
		if (itemId < 0)
		{
			throw new IllegalArgumentException("Invalid item id");
		}
		GoalIcon icon = new GoalIcon();
		icon.itemId = itemId;
		return icon;
	}

	public static GoalIcon activity(HiscoreSkill skill)
	{
		GoalIcon icon = new GoalIcon();
		icon.activity = skill.name();
		return icon;
	}

	public static GoalIcon side(SideIcon sideIcon)
	{
		GoalIcon icon = new GoalIcon();
		icon.sideIcon = sideIcon;
		return icon;
	}

	@Nullable
	public HiscoreSkill hiscoreSkill()
	{
		if (activity == null)
		{
			return null;
		}
		try
		{
			return HiscoreSkill.valueOf(activity);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	/**
	 * Trasiga ikoner från sparfilen (t.ex. en borttagen hiscore-aktivitet) visas inte alls.
	 */
	public boolean isValid()
	{
		int set = (itemId != null ? 1 : 0) + (activity != null ? 1 : 0) + (sideIcon != null ? 1 : 0);
		if (set != 1)
		{
			return false;
		}
		return (itemId == null || itemId >= 0) && (activity == null || hiscoreSkill() != null);
	}
}
