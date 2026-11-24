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

import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.GoalIcon;
import com.gielinorgoals.model.Requirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Alla achievement diaries på en nivå, ett mål per område.
 */
public class DiaryTierTemplate implements GoalTemplate
{
	static final TemplateParameter TIER = TemplateParameter.choice("tier", "Tier", options(), DiaryTier.EASY.name());

	private static List<TemplateParameter.Option> options()
	{
		List<TemplateParameter.Option> options = new ArrayList<>();
		for (DiaryTier tier : DiaryTier.values())
		{
			options.add(new TemplateParameter.Option(tier.name(), tier.getDisplayName()));
		}
		return options;
	}

	@Override
	public String getId()
	{
		return "diary-tier";
	}

	@Override
	public String getName()
	{
		return "All diaries in a tier";
	}

	@Override
	public String getDescription()
	{
		return "Every area's achievement diary at one tier, e.g. all Hard diaries";
	}

	@Override
	public String getCategory()
	{
		return TemplateCategories.DIARIES;
	}

	@Override
	public List<String> getTags()
	{
		return List.of("diary", "diaries", "achievement diary", "achievement diaries", "all diaries", "easy", "medium",
			"hard", "elite");
	}

	@Override
	public Requirement getIconRequirement()
	{
		return new DiaryRequirement(DiaryArea.values()[0], DiaryTier.EASY);
	}

	@Override
	public List<TemplateParameter> getParameters()
	{
		return List.of(TIER);
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		return build(parameters, Map.of());
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters, Map<String, String> choices)
	{
		DiaryTier tier = DiaryTier.valueOf(TIER.choiceIn(choices));
		TemplateNode root = TemplateNode.aggregate("All " + tier.getDisplayName() + " diaries");
		root.setIcon(GoalIcon.side(GoalIcon.SideIcon.ACHIEVEMENT_DIARIES));
		root.setWikiPage("Achievement Diary");
		for (DiaryArea area : DiaryArea.values())
		{
			root.add(TemplateNode.requirement(new DiaryRequirement(area, tier)));
		}
		return root;
	}
}
