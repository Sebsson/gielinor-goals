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

import com.gielinorgoals.GoalManager;
import com.gielinorgoals.model.ActivityCounters;
import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.template.QuestRequirementsDb;
import com.gielinorgoals.template.TemplateBuilder;
import com.gielinorgoals.tracking.AccountStatus;
import com.gielinorgoals.template.TemplateUpdates;
import com.gielinorgoals.tracking.CompletionLookup;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;

/**
 * Samlar tjänster som dialogerna och raderna behöver, så att de inte måste skickas en och en.
 * Används på EDT.
 */
@Singleton
public class DialogServices
{
	@Getter
	private final ItemSearch itemSearch;
	@Getter
	private final CombatAchievementCatalog combatAchievements;
	@Getter
	private final CompletionLookup completionLookup;
	@Getter
	private final GoalIcons icons;
	@Getter
	private final QuestRequirementsDb questRequirements;
	@Getter
	private final TemplateUpdates templateUpdates;
	@Getter
	private final AccountStatus accountStatus;
	// Bygger templates, på klienttråden för de som behöver spelets data.
	@Getter
	private final TemplateBuilder templateBuilder;
	private final GoalManager goalManager;
	// Sätts av pluginet, som äger kartfönstret.
	private Consumer<String> mapOpener = id ->
	{
	};

	@Inject
	DialogServices(ItemSearch itemSearch, CombatAchievementCatalog combatAchievements,
		CompletionLookup completionLookup, GoalIcons icons, GoalManager goalManager,
		QuestRequirementsDb questRequirements, TemplateUpdates templateUpdates, AccountStatus accountStatus,
		TemplateBuilder templateBuilder)
	{
		this.templateBuilder = templateBuilder;
		this.itemSearch = itemSearch;
		this.combatAchievements = combatAchievements;
		this.completionLookup = completionLookup;
		this.icons = icons;
		this.goalManager = goalManager;
		this.questRequirements = questRequirements;
		this.templateUpdates = templateUpdates;
		this.accountStatus = accountStatus;
	}

	/**
	 * Senaste spelläget för den laddade profilen (XP, föremål), eller tomt om det inte är känt.
	 */
	public GameData currentGameData()
	{
		return goalManager.getCurrentGameData();
	}

	/**
	 * Profilens räknare (kill count, clues, NPC-kills, egna räknare).
	 */
	public ActivityCounters counters()
	{
		return goalManager.getCounters();
	}

	public void setMapOpener(Consumer<String> mapOpener)
	{
		this.mapOpener = mapOpener;
	}

	/**
	 * "Show on map": öppnar kartan och centrerar och markerar målet.
	 */
	public void showOnMap(String goalId)
	{
		mapOpener.accept(goalId);
	}

	public boolean isDuplicate(Requirement requirement, @Nullable String exceptGoalId)
	{
		return goalManager.hasGoalWithRequirement(requirement, exceptGoalId);
	}
}
