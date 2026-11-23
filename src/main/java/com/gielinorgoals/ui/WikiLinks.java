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
import com.gielinorgoals.model.CombatLevelRequirement;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.SkillRequirement;
import com.gielinorgoals.model.TotalLevelRequirement;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.annotation.Nullable;
import net.runelite.client.hiscore.HiscoreSkill;

/**
 * Adresser till OSRS Wiki för ett mål, för "Open on wiki". Sidtitlar kodas korrekt
 * (mellanslag blir understreck, övriga specialtecken procentkodas, snedstreck behålls
 * för undersidor). En egen {@code wikiPage} på målet går före allt annat.
 */
final class WikiLinks
{
	static final String BASE = "https://oldschool.runescape.wiki/w/";

	// Hiscore-namn som skiljer sig från wikins sidtitlar. Övriga bossar har samma namn som
	// sidan (eller en omdirigering dit); svåra lägen länkar till grundsidan.
	private static final Map<HiscoreSkill, String> HISCORE_PAGES = Map.of(
		HiscoreSkill.BARROWS_CHESTS, "Barrows",
		HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE, "Chambers of Xeric",
		HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE, "Theatre of Blood",
		HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT, "Tombs of Amascut");

	private WikiLinks()
	{
	}

	/**
	 * @return adressen, eller null om målet inte har något att slå upp (t.ex. en egen räknare)
	 */
	@Nullable
	static String url(Goal goal)
	{
		if (goal.getWikiPage() != null && !goal.getWikiPage().trim().isEmpty())
		{
			return page(goal.getWikiPage());
		}
		return url(goal.getRequirement());
	}

	@Nullable
	static String url(@Nullable Requirement requirement)
	{
		if (requirement instanceof ItemRequirement)
		{
			// Special:Lookup hittar rätt sida för item-id:t, även när namnet är tvetydigt.
			return BASE + "Special:Lookup?type=item&id=" + ((ItemRequirement) requirement).getItemId();
		}
		if (requirement instanceof QuestRequirement)
		{
			return page(questPage(((QuestRequirement) requirement).getQuestName()));
		}
		if (requirement instanceof SkillRequirement)
		{
			return page(((SkillRequirement) requirement).getSkill().getName());
		}
		if (requirement instanceof DiaryRequirement)
		{
			return page(((DiaryRequirement) requirement).getArea().getDisplayName() + " Diary");
		}
		if (requirement instanceof CombatAchievementRequirement)
		{
			return page("Combat Achievements");
		}
		if (requirement instanceof QuestPointsRequirement)
		{
			return page("Quest points");
		}
		if (requirement instanceof TotalLevelRequirement)
		{
			return page("Total level");
		}
		if (requirement instanceof CombatLevelRequirement)
		{
			return page("Combat level");
		}
		if (requirement instanceof KillCountRequirement)
		{
			return killCount((KillCountRequirement) requirement);
		}
		return null;
	}

	@Nullable
	private static String killCount(KillCountRequirement kc)
	{
		switch (kc.getActivityType())
		{
			case NPC:
				return page(kc.getActivityName());
			case MANUAL:
				return null;
			default:
				HiscoreSkill skill = kc.hiscoreSkill();
				if (skill == null)
				{
					return null;
				}
				if (skill.name().startsWith("CLUE_SCROLL_"))
				{
					return page("Treasure Trails");
				}
				return page(HISCORE_PAGES.getOrDefault(skill, skill.getName()));
		}
	}

	// Recipe for Disasters delar heter "Recipe for Disaster - X" i RuneLite men är avsnitt
	// på huvudsidan; de länkas dit.
	static String questPage(String questName)
	{
		if (questName.startsWith("Recipe for Disaster - "))
		{
			return "Recipe for Disaster";
		}
		return questName;
	}

	/**
	 * Full adress för en sidtitel, t.ex. "King's Ransom" → ".../w/King%27s_Ransom".
	 */
	static String page(String title)
	{
		String underscored = title.trim().replace(' ', '_');
		return BASE + encode(underscored);
	}

	private static String encode(String title)
	{
		try
		{
			// URLEncoder kodar för formulär; snedstreck och understreck ska stå kvar i en sidväg.
			return URLEncoder.encode(title, StandardCharsets.UTF_8.name())
				.replace("%2F", "/")
				.replace("+", "%20");
		}
		catch (UnsupportedEncodingException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
