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
package com.gielinorgoals.template;

import com.gielinorgoals.ui.CombatAchievementCatalog;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Alla templates som går att lägga till. Resursfilerna läses en gång när katalogen skapas.
 */
@Singleton
public class TemplateCatalog
{
	private final List<GoalTemplate> templates;

	// Användarens egna templates; null i tester med en egen lista.
	@javax.annotation.Nullable
	private final CustomTemplates custom;

	@Inject
	TemplateCatalog(Gson gson, QuestRequirementsDb questRequirements, CombatAchievementCatalog combatAchievements,
		CustomTemplates custom)
	{
		this(gson, questRequirements, (CombatAchievementSource) combatAchievements, custom);
	}

	/**
	 * Utan klient: combat achievement-templaten ber om inloggning. För tester.
	 */
	public TemplateCatalog(Gson gson)
	{
		this(gson, QuestRequirementsDb.load(gson), CombatAchievementSource.NONE);
	}

	public TemplateCatalog(Gson gson, QuestRequirementsDb questRequirements, CombatAchievementSource combatAchievements)
	{
		this(gson, questRequirements, combatAchievements, CustomTemplates.inMemory(gson));
	}

	public TemplateCatalog(Gson gson, QuestRequirementsDb questRequirements, CombatAchievementSource combatAchievements,
		CustomTemplates custom)
	{
		this.custom = custom;
		List<GoalTemplate> all = new ArrayList<>();
		all.add(new QuestCapeTemplate(QuestCapeExclusions.load(gson)));
		all.add(new DiaryCapeTemplate());
		all.add(new MaxCapeTemplate());
		all.add(new BaseLevelTemplate());
		all.add(new SkillLadderTemplate());
		all.add(AccountLevelTemplate.totalLevel());
		all.add(AccountLevelTemplate.combatLevel());
		all.add(new CombatAchievementTemplate(combatAchievements));
		all.add(new QuestSeriesTemplate(questRequirements));
		all.add(new DiaryTierTemplate());
		all.add(new DiaryRewardsTemplate(DiaryRewards.load(gson)));
		// Handplockade templates från JSON-filer i resources.
		all.addAll(JsonTemplates.load(gson));
		templates = Collections.unmodifiableList(all);
	}

	// För tester: en egen lista.
	TemplateCatalog(List<GoalTemplate> templates)
	{
		this.custom = null;
		this.templates = Collections.unmodifiableList(templates);
	}

	/**
	 * Egna templates först, sedan de inbyggda. En ny lista vid varje anrop, eftersom de egna
	 * kan ändras.
	 */
	public List<GoalTemplate> getTemplates()
	{
		if (custom == null)
		{
			return templates;
		}
		List<GoalTemplate> all = new ArrayList<>(custom.getTemplates());
		all.addAll(templates);
		return Collections.unmodifiableList(all);
	}

	/**
	 * @return templaten med id:t, eller null om den inte finns (t.ex. borttagen i en senare version)
	 */
	@javax.annotation.Nullable
	public GoalTemplate find(@javax.annotation.Nullable String id)
	{
		if (CustomTemplates.isCustomId(id))
		{
			// En borttagen egen template ger null: inget uppdateringsmärke och inget fel.
			return custom == null ? null : custom.find(id);
		}
		for (GoalTemplate template : templates)
		{
			if (template.getId().equals(id))
			{
				return template;
			}
		}
		return null;
	}
}
