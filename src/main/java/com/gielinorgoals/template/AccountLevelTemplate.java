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

import com.gielinorgoals.model.CombatLevelRequirement;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.TotalLevelRequirement;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Ett enda mål för total level eller combat level, för snabb åtkomst från dialogen.
 * Finns målet redan kopplas templaten till det i stället för att skapa en dubblett.
 */
public final class AccountLevelTemplate implements GoalTemplate
{
	// Spec: 500 som lägsta; högsta följer antalet skills (2376 med Sailing).
	static final int MIN_TOTAL = 500;

	private final String id;
	private final String name;
	private final String description;
	private final List<String> tags;
	private final TemplateParameter level;
	private final IntFunction<Requirement> requirement;

	private AccountLevelTemplate(String id, String name, String description, List<String> tags,
		TemplateParameter level, IntFunction<Requirement> requirement)
	{
		this.id = id;
		this.name = name;
		this.description = description;
		this.tags = tags;
		this.level = level;
		this.requirement = requirement;
	}

	public static AccountLevelTemplate totalLevel()
	{
		return new AccountLevelTemplate("total-level", "Total level N", "Reach a total level, e.g. 1500",
			List.of("total", "total level", "total lvl", "skill total"),
			TemplateParameter.number("level", "Total level", MIN_TOTAL, TotalLevelRequirement.MAX, 1500),
			TotalLevelRequirement::new);
	}

	public static AccountLevelTemplate combatLevel()
	{
		return new AccountLevelTemplate("combat-level", "Combat level N", "Reach a combat level, e.g. 100",
			List.of("combat", "combat level", "cb", "cmb", "cb level"),
			TemplateParameter.number("level", "Combat level", CombatLevelRequirement.MIN, CombatLevelRequirement.MAX,
				100),
			CombatLevelRequirement::new);
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public String getCategory()
	{
		return TemplateCategories.SKILLS;
	}

	@Override
	public List<String> getTags()
	{
		return tags;
	}

	@Override
	public Requirement getIconRequirement()
	{
		return requirement.apply(level.getDefaultValue());
	}

	@Override
	public List<TemplateParameter> getParameters()
	{
		return List.of(level);
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		return TemplateNode.requirement(requirement.apply(level.valueIn(parameters)));
	}
}
