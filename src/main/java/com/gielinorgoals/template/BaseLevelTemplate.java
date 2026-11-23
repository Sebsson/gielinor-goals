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

import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.SkillRequirement;
import com.gielinorgoals.model.TotalLevelRequirement;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Base N: samma nivå i alla skills, genererat från {@link Skill} utan OVERALL.
 */
public class BaseLevelTemplate implements GoalTemplate
{
	static final TemplateParameter LEVEL = new TemplateParameter("level", "Level in every skill",
		SkillRequirement.MIN_LEVEL, Experience.MAX_REAL_LEVEL, 70);

	@Override
	public String getId()
	{
		return "base-level";
	}

	@Override
	public String getName()
	{
		return "Base N";
	}

	@Override
	public String getDescription()
	{
		return "The same level in every skill, e.g. base 70";
	}

	@Override
	public Requirement getIconRequirement()
	{
		// Stats-ikonen, samma som för total level.
		return new TotalLevelRequirement(TotalLevelRequirement.MIN);
	}

	@Override
	public List<TemplateParameter> getParameters()
	{
		return List.of(LEVEL);
	}

	// Skill.OVERALL är markerad som deprecated men måste ändå uteslutas här.
	@SuppressWarnings("deprecation")
	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		int level = LEVEL.valueIn(parameters);
		TemplateNode root = TemplateNode.aggregate("Base " + level);
		for (Skill skill : Skill.values())
		{
			if (skill != Skill.OVERALL)
			{
				root.add(TemplateNode.requirement(SkillRequirement.level(skill, level)));
			}
		}
		return root;
	}
}
