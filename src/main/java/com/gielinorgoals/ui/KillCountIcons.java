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

import com.gielinorgoals.model.KillCountRequirement;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.hiscore.HiscoreSkill;

/**
 * Spelets egna hiscore-ikoner för KC-mål. Kopplingen görs med en uttrycklig lista,
 * eftersom sprite-konstanterna inte går att slå upp på namn utan reflection. Bossar
 * utan egen ikon får combat-ikonen.
 */
final class KillCountIcons
{
	private static final Map<HiscoreSkill, Integer> SPRITES = new EnumMap<>(HiscoreSkill.class);

	static
	{
		boss(HiscoreSkill.ABYSSAL_SIRE, SpriteID.IconBoss25x25.ABYSSAL_SIRE);
		boss(HiscoreSkill.ALCHEMICAL_HYDRA, SpriteID.IconBoss25x25.ALCHEMICAL_HYDRA);
		boss(HiscoreSkill.AMOXLIATL, SpriteID.IconBoss25x25.AMOXLIATL);
		boss(HiscoreSkill.ARAXXOR, SpriteID.IconBoss25x25.ARAXXOR);
		boss(HiscoreSkill.ARTIO, SpriteID.IconBoss25x25.ARTIO_CALLISTO);
		boss(HiscoreSkill.CALLISTO, SpriteID.IconBoss25x25.ARTIO_CALLISTO);
		boss(HiscoreSkill.BARROWS_CHESTS, SpriteID.IconBoss25x25.BARROWS_CHESTS);
		boss(HiscoreSkill.BRUTUS, SpriteID.IconBoss25x25.BRUTUS);
		boss(HiscoreSkill.BRYOPHYTA, SpriteID.IconBoss25x25.BRYOPHYTA);
		boss(HiscoreSkill.CALVARION, SpriteID.IconBoss25x25.CALVARION_VETION);
		boss(HiscoreSkill.VETION, SpriteID.IconBoss25x25.CALVARION_VETION);
		boss(HiscoreSkill.CERBERUS, SpriteID.IconBoss25x25.CERBERUS);
		boss(HiscoreSkill.CHAMBERS_OF_XERIC, SpriteID.IconBoss25x25.CHAMBERS_OF_XERIC);
		boss(HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE, SpriteID.IconBoss25x25.CHAMBERS_OF_XERIC_CHALLENGE_MODE);
		boss(HiscoreSkill.CHAOS_ELEMENTAL, SpriteID.IconBoss25x25.CHAOS_ELEMENTAL);
		boss(HiscoreSkill.CHAOS_FANATIC, SpriteID.IconBoss25x25.CHAOS_FANATIC);
		boss(HiscoreSkill.COMMANDER_ZILYANA, SpriteID.IconBoss25x25.COMMANDER_ZILYANA);
		boss(HiscoreSkill.CORPOREAL_BEAST, SpriteID.IconBoss25x25.CORPOREAL_BEAST);
		boss(HiscoreSkill.CRAZY_ARCHAEOLOGIST, SpriteID.IconBoss25x25.CRAZY_ARCHAEOLOGIST);
		boss(HiscoreSkill.DAGANNOTH_PRIME, SpriteID.IconBoss25x25.DAGANNOTH_PRIME);
		boss(HiscoreSkill.DAGANNOTH_REX, SpriteID.IconBoss25x25.DAGANNOTH_REX);
		boss(HiscoreSkill.DAGANNOTH_SUPREME, SpriteID.IconBoss25x25.DAGANNOTH_SUPREME);
		boss(HiscoreSkill.DERANGED_ARCHAEOLOGIST, SpriteID.IconBoss25x25.DERANGED_ARCHAEOLOGIST);
		boss(HiscoreSkill.DOOM_OF_MOKHAIOTL, SpriteID.IconBoss25x25.DOOM_OF_MOKHAIOTL);
		boss(HiscoreSkill.DUKE_SUCELLUS, SpriteID.IconBoss25x25.DUKE_SUCELLUS);
		boss(HiscoreSkill.GENERAL_GRAARDOR, SpriteID.IconBoss25x25.GENERAL_GRAARDOR);
		boss(HiscoreSkill.GIANT_MOLE, SpriteID.IconBoss25x25.GIANT_MOLE);
		boss(HiscoreSkill.GROTESQUE_GUARDIANS, SpriteID.IconBoss25x25.GROTESQUE_GUARDIANS);
		boss(HiscoreSkill.HESPORI, SpriteID.IconBoss25x25.HESPORI);
		boss(HiscoreSkill.KALPHITE_QUEEN, SpriteID.IconBoss25x25.KALPHITE_QUEEN);
		boss(HiscoreSkill.KING_BLACK_DRAGON, SpriteID.IconBoss25x25.KING_BLACK_DRAGON);
		boss(HiscoreSkill.KRAKEN, SpriteID.IconBoss25x25.KRAKEN);
		boss(HiscoreSkill.KREEARRA, SpriteID.IconBoss25x25.KREEARRA);
		boss(HiscoreSkill.KRIL_TSUTSAROTH, SpriteID.IconBoss25x25.KRIL_TSUTSAROTH);
		boss(HiscoreSkill.LUNAR_CHESTS, SpriteID.IconBoss25x25.LUNAR_CHESTS);
		boss(HiscoreSkill.MAD_ANGEL, SpriteID.IconBoss25x25.MAD_ANGEL);
		boss(HiscoreSkill.MAGGOT_KING, SpriteID.IconBoss25x25.MAGGOT_KING);
		boss(HiscoreSkill.MIMIC, SpriteID.IconBoss25x25.MIMIC);
		boss(HiscoreSkill.NEX, SpriteID.IconBoss25x25.NEX);
		boss(HiscoreSkill.NIGHTMARE, SpriteID.IconBoss25x25.NIGHTMARE);
		boss(HiscoreSkill.PHOSANIS_NIGHTMARE, SpriteID.IconBoss25x25.NIGHTMARE);
		boss(HiscoreSkill.OBOR, SpriteID.IconBoss25x25.OBOR);
		boss(HiscoreSkill.PHANTOM_MUSPAH, SpriteID.IconBoss25x25.PHANTOM_MUSPAH);
		boss(HiscoreSkill.SARACHNIS, SpriteID.IconBoss25x25.SARACHNIS);
		boss(HiscoreSkill.SCORPIA, SpriteID.IconBoss25x25.SCORPIA);
		boss(HiscoreSkill.SCURRIUS, SpriteID.IconBoss25x25.SCURRIUS);
		boss(HiscoreSkill.SHELLBANE_GRYPHON, SpriteID.IconBoss25x25.SHELLBANE_GRYPHON);
		boss(HiscoreSkill.SKOTIZO, SpriteID.IconBoss25x25.SKOTIZO);
		boss(HiscoreSkill.SOL_HEREDIT, SpriteID.IconBoss25x25.SOL_HEREDIT);
		boss(HiscoreSkill.SPINDEL, SpriteID.IconBoss25x25.SPINDEL_VENENATIS);
		boss(HiscoreSkill.VENENATIS, SpriteID.IconBoss25x25.SPINDEL_VENENATIS);
		boss(HiscoreSkill.TEMPOROSS, SpriteID.IconBoss25x25.TEMPOROSS);
		boss(HiscoreSkill.THE_GAUNTLET, SpriteID.IconBoss25x25.THE_GAUNTLET);
		boss(HiscoreSkill.THE_CORRUPTED_GAUNTLET, SpriteID.IconBoss25x25.THE_CORRUPTED_GAUNTLET);
		boss(HiscoreSkill.THE_FRACTURED_ARCHIVE, SpriteID.IconBoss25x25.THE_FRACTURED_ARCHIVE);
		boss(HiscoreSkill.THE_HUEYCOATL, SpriteID.IconBoss25x25.THE_HUEYCOATL);
		boss(HiscoreSkill.THE_LEVIATHAN, SpriteID.IconBoss25x25.THE_LEVIATHAN);
		boss(HiscoreSkill.THE_ROYAL_TITANS, SpriteID.IconBoss25x25.ROYAL_TITANS);
		boss(HiscoreSkill.THE_WHISPERER, SpriteID.IconBoss25x25.THE_WHISPERER);
		boss(HiscoreSkill.THEATRE_OF_BLOOD, SpriteID.IconBoss25x25.THEATRE_OF_BLOOD);
		boss(HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE, SpriteID.IconBoss25x25.THEATRE_OF_BLOOD);
		boss(HiscoreSkill.THERMONUCLEAR_SMOKE_DEVIL, SpriteID.IconBoss25x25.THERMONUCLEAR_SMOKE_DEVIL);
		boss(HiscoreSkill.TOMBS_OF_AMASCUT, SpriteID.IconBoss25x25.TOMBS_OF_AMASCUT);
		boss(HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT, SpriteID.IconBoss25x25.TOMBS_OF_AMASCUT_EXPERT);
		boss(HiscoreSkill.TZKAL_ZUK, SpriteID.IconBoss25x25.TZKAL_ZUK);
		boss(HiscoreSkill.TZTOK_JAD, SpriteID.IconBoss25x25.TZTOK_JAD);
		boss(HiscoreSkill.VARDORVIS, SpriteID.IconBoss25x25.VARDORVIS);
		boss(HiscoreSkill.VORKATH, SpriteID.IconBoss25x25.VORKATH);
		boss(HiscoreSkill.WINTERTODT, SpriteID.IconBoss25x25.WINTERTODT);
		boss(HiscoreSkill.YAMA, SpriteID.IconBoss25x25.YAMA);
		boss(HiscoreSkill.ZALCANO, SpriteID.IconBoss25x25.ZALCANO);
		boss(HiscoreSkill.ZULRAH, SpriteID.IconBoss25x25.ZULRAH);

		// Alla clue-nivåer delar clue-ikonen.
		for (HiscoreSkill skill : HiscoreSkill.values())
		{
			if (skill.name().startsWith("CLUE_SCROLL_"))
			{
				boss(skill, SpriteID.IconActivities25x25.CLUE_SCROLL_ALL);
			}
		}
		boss(HiscoreSkill.BOUNTY_HUNTER_HUNTER, SpriteID.IconActivities25x25.BOUNTY_HUNTER_HUNTER);
		boss(HiscoreSkill.BOUNTY_HUNTER_ROGUE, SpriteID.IconActivities25x25.BOUNTY_HUNTER_ROGUE);
		boss(HiscoreSkill.LAST_MAN_STANDING, SpriteID.IconActivities25x25.LAST_MAN_STANDING);
		boss(HiscoreSkill.LEAGUE_POINTS, SpriteID.IconActivities25x25.LEAGUE_POINTS);
		boss(HiscoreSkill.SOUL_WARS_ZEAL, SpriteID.IconActivities25x25.SOUL_WARS_ZEAL);
		boss(HiscoreSkill.PVP_ARENA_RANK, SpriteID.IconActivities25x25.PVP_ARENA_RANK);
		boss(HiscoreSkill.RIFTS_CLOSED, SpriteID.IconActivities25x25.RIFTS_CLOSED);
		boss(HiscoreSkill.COLOSSEUM_GLORY, SpriteID.IconActivities25x25.COLOSSEUM_GLORY);
		boss(HiscoreSkill.COLLECTIONS_LOGGED, SpriteID.IconActivities25x25.COLLECTIONS_LOGGED);
	}

	private KillCountIcons()
	{
	}

	private static void boss(HiscoreSkill skill, int sprite)
	{
		SPRITES.put(skill, sprite);
	}

	static int spriteFor(KillCountRequirement requirement)
	{
		switch (requirement.getActivityType())
		{
			case NPC:
				return SpriteID.SideIcons.COMBAT;
			case MANUAL:
				return SpriteID.SideIcons.STATS;
			default:
				HiscoreSkill skill = requirement.hiscoreSkill();
				return skill == null ? SpriteID.SideIcons.COMBAT : spriteFor(skill);
		}
	}

	static int spriteFor(HiscoreSkill skill)
	{
		return SPRITES.getOrDefault(skill, SpriteID.SideIcons.COMBAT);
	}
}
