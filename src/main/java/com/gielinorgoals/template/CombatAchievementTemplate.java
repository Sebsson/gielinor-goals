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

import com.gielinorgoals.model.CombatAchievementRequirement;
import com.gielinorgoals.model.GoalIcon;
import com.gielinorgoals.model.Requirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Alla combat achievements i en nivå, ett mål per uppgift. Uppgifterna läses ur spelets
 * cache, så templaten byggs på klienttråden och kräver att man är inloggad (för att
 * kunna se vilka som redan är klara). Katalogen vet inte vilken boss en uppgift hör till,
 * så listan är platt, i spelets ordning.
 */
public class CombatAchievementTemplate implements GoalTemplate
{
	// Samma namn som CombatAchievementCatalog ger uppgifterna.
	static final List<String> TIERS = List.of("Easy", "Medium", "Hard", "Elite", "Master", "Grandmaster");
	static final TemplateParameter TIER = TemplateParameter.choice("tier", "Tier", options(), "Easy");

	private final CombatAchievementSource source;

	public CombatAchievementTemplate(CombatAchievementSource source)
	{
		this.source = source;
	}

	private static List<TemplateParameter.Option> options()
	{
		List<TemplateParameter.Option> options = new ArrayList<>();
		for (String tier : TIERS)
		{
			options.add(new TemplateParameter.Option(tier, tier));
		}
		return options;
	}

	@Override
	public String getId()
	{
		return "combat-achievements";
	}

	@Override
	public String getName()
	{
		return "Combat Achievements tier";
	}

	@Override
	public String getDescription()
	{
		return "Every combat achievement in one tier, e.g. all Hard tasks";
	}

	@Override
	public String getCategory()
	{
		return TemplateCategories.BOSSES;
	}

	@Override
	public List<String> getTags()
	{
		List<String> tags = new ArrayList<>(List.of("ca", "combat achievements", "combat achievement", "cas", "tasks"));
		for (String tier : TIERS)
		{
			tags.add(tier.toLowerCase(java.util.Locale.ROOT));
		}
		tags.add("gm");
		return tags;
	}

	@Override
	public Requirement getIconRequirement()
	{
		return new CombatAchievementRequirement(0, "", TIERS.get(0));
	}

	@Override
	public List<TemplateParameter> getParameters()
	{
		return List.of(TIER);
	}

	@Override
	public boolean needsClientData()
	{
		return true;
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		return build(parameters, Map.of());
	}

	// Klienttråden, via TemplateBuilder.
	@Override
	public TemplateNode build(Map<String, Integer> parameters, Map<String, String> choices)
	{
		String tier = TIER.choiceIn(choices);
		List<CombatAchievementRequirement> tasks = source.tasksIfLoggedIn();
		if (tasks == null || tasks.isEmpty())
		{
			throw new TemplateUnavailableException(TemplateUnavailableException.LOG_IN);
		}

		TemplateNode root = TemplateNode.aggregate("Combat Achievements: " + tier);
		root.setIcon(GoalIcon.side(GoalIcon.SideIcon.COMBAT));
		root.setWikiPage(tier + " Combat Achievements");
		for (CombatAchievementRequirement task : tasks)
		{
			if (tier.equals(task.getTierName()))
			{
				root.add(TemplateNode.requirement(task));
			}
		}
		if (root.getChildren().isEmpty())
		{
			// Nivån gick inte att läsa; hellre ett meddelande än ett tomt mål.
			throw new TemplateUnavailableException(TemplateUnavailableException.LOG_IN);
		}
		return root;
	}
}
