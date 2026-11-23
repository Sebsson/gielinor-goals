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

import com.gielinorgoals.GielinorGoalsConfig;
import com.gielinorgoals.GoalManager;
import com.gielinorgoals.model.KillCountRequirement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;
import net.runelite.client.hiscore.Skill;

/**
 * Hämtar spelarens kill counts, clue-räkningar och minigames från de officiella
 * hiscores (Jagex egna adresser via RuneLites {@link HiscoreClient}). Anropet görs
 * asynkront på OkHttps trådar, aldrig på klienttråden, och resultatet lämnas till
 * {@link GoalManager} på EDT.
 */
@Slf4j
@Singleton
public class HiscoreFetcher
{
	// Efter inloggning räcker det med en hämtning då och då; hiscores uppdateras inte ofta.
	static final long LOGIN_COOLDOWN_MS = 10 * 60 * 1000;
	// När ett nytt KC-mål skapas vill man se startvärdet, men utan att spamma vid många mål.
	static final long NEW_GOAL_COOLDOWN_MS = 30 * 1000;

	// Värden för VarbitID.IRONMAN, samma indelning som RuneLites hiscore-plugin.
	private static final int ACCOUNT_IRONMAN = 1;
	private static final int ACCOUNT_ULTIMATE = 2;
	private static final int ACCOUNT_HARDCORE = 3;

	private final Client client;
	private final HiscoreClient hiscoreClient;
	private final GielinorGoalsConfig config;
	private final GoalManager goalManager;

	// Läses och skrivs bara på klienttråden.
	private long lastFetchAt;
	@Nullable
	private String lastProfile;

	@Inject
	HiscoreFetcher(Client client, HiscoreClient hiscoreClient, GielinorGoalsConfig config, GoalManager goalManager)
	{
		this.client = client;
		this.hiscoreClient = hiscoreClient;
		this.config = config;
		this.goalManager = goalManager;
	}

	/**
	 * Begär en hämtning om config tillåter och cooldownen har gått ut. Körs på klienttråden.
	 *
	 * @param newGoal true när ett nytt KC-mål har skapats (kortare cooldown)
	 * @return true om en hämtning startades
	 */
	public boolean request(@Nullable String profileKey, boolean newGoal)
	{
		Player player = client.getLocalPlayer();
		if (!config.fetchHiscores() || profileKey == null || player == null || player.getName() == null)
		{
			return false;
		}

		long now = System.currentTimeMillis();
		long cooldown = newGoal ? NEW_GOAL_COOLDOWN_MS : LOGIN_COOLDOWN_MS;
		if (Objects.equals(profileKey, lastProfile) && now - lastFetchAt < cooldown)
		{
			return false;
		}
		lastProfile = profileKey;
		lastFetchAt = now;

		String name = player.getName();
		HiscoreEndpoint endpoint = endpoint();
		hiscoreClient.lookupAsync(name, endpoint).whenComplete((result, error) ->
		{
			if (error != null || result == null)
			{
				log.debug("Hiscore lookup failed", error);
				return;
			}
			Map<String, Long> values = toCounters(result);
			SwingUtilities.invokeLater(() -> goalManager.recordHiscores(profileKey, values));
		});
		return true;
	}

	// Säsongs- och deadman-världar har egna hiscores; annars avgör kontotypen.
	private HiscoreEndpoint endpoint()
	{
		HiscoreEndpoint fromWorld = HiscoreEndpoint.fromWorldTypes(client.getWorldType());
		if (fromWorld != HiscoreEndpoint.NORMAL)
		{
			return fromWorld;
		}
		switch (client.getVarbitValue(VarbitID.IRONMAN))
		{
			case ACCOUNT_IRONMAN:
				return HiscoreEndpoint.IRONMAN;
			case ACCOUNT_ULTIMATE:
				return HiscoreEndpoint.ULTIMATE_IRONMAN;
			case ACCOUNT_HARDCORE:
				return HiscoreEndpoint.HARDCORE_IRONMAN;
			default:
				return HiscoreEndpoint.NORMAL;
		}
	}

	/**
	 * Bossar och aktiviteter med kända värden, nycklade som i räknarlagret. Ej rankad (−1)
	 * betyder okänt och tas inte med.
	 */
	static Map<String, Long> toCounters(HiscoreResult result)
	{
		Map<String, Long> values = new HashMap<>();
		for (Map.Entry<HiscoreSkill, Skill> entry : result.getSkills().entrySet())
		{
			HiscoreSkill skill = entry.getKey();
			Skill score = entry.getValue();
			if (score == null || score.getLevel() < 0
				|| (skill.getType() != HiscoreSkillType.BOSS && skill.getType() != HiscoreSkillType.ACTIVITY))
			{
				continue;
			}
			values.put(KillCountRequirement.counterKey(KillCountRequirement.ActivityType.HISCORE, skill.name()),
				(long) score.getLevel());
		}
		return values;
	}
}
