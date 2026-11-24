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

import com.gielinorgoals.model.GoalIcon;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.SkillRequirement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * En skill i steg, 60 → 70 → 80 → 90 → 99, där varje steg är delmål till nästa.
 * Befintliga mål för samma level kopplas in som vanligt.
 */
public class SkillLadderTemplate implements GoalTemplate
{
	static final List<Integer> STEPS = List.of(60, 70, 80, 90, 99);
	static final TemplateParameter SKILL = TemplateParameter.choice("skill", "Skill", options(), Skill.ATTACK.name());

	// Skill.OVERALL är markerad som deprecated men måste ändå uteslutas här.
	@SuppressWarnings("deprecation")
	private static List<TemplateParameter.Option> options()
	{
		List<Skill> skills = new ArrayList<>();
		for (Skill skill : Skill.values())
		{
			if (skill != Skill.OVERALL)
			{
				skills.add(skill);
			}
		}
		// Bokstavsordning, så att en skill går lätt att hitta i listan.
		skills.sort(Comparator.comparing(Skill::getName));
		List<TemplateParameter.Option> options = new ArrayList<>();
		for (Skill skill : skills)
		{
			options.add(new TemplateParameter.Option(skill.name(), skill.getName()));
		}
		return options;
	}

	@Override
	public String getId()
	{
		return "skill-ladder";
	}

	@Override
	public String getName()
	{
		return "Skill ladder";
	}

	@Override
	public String getDescription()
	{
		return "One skill in steps: 60, 70, 80, 90 and 99";
	}

	@Override
	public String getCategory()
	{
		return TemplateCategories.SKILLS;
	}

	@Override
	public List<String> getTags()
	{
		return List.of("skill", "ladder", "99", "skillcape", "skill cape", "level");
	}

	@Override
	public Requirement getIconRequirement()
	{
		return SkillRequirement.level(Skill.ATTACK, 99);
	}

	@Override
	public List<TemplateParameter> getParameters()
	{
		return List.of(SKILL);
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		return build(parameters, Map.of());
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters, Map<String, String> choices)
	{
		Skill skill = Skill.valueOf(SKILL.choiceIn(choices));
		TemplateNode root = TemplateNode.aggregate(skill.getName() + " 60-99");
		root.setIcon(GoalIcon.side(GoalIcon.SideIcon.STATS));
		// Byggs nerifrån: 60 först, varje steg läggs sedan under nästa.
		TemplateNode below = null;
		for (int level : STEPS)
		{
			TemplateNode step = TemplateNode.requirement(SkillRequirement.level(skill, level));
			if (below != null)
			{
				step.add(below);
			}
			below = step;
		}
		root.add(below);
		return root;
	}
}
