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
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.gameval.ItemID;

/**
 * Max cape: Base N med N = 99, med eget namn, ikon och taggar. Bygget görs av
 * {@link BaseLevelTemplate}, så skill-målen blir samma och kopplas ihop med en Base 99.
 */
public class MaxCapeTemplate implements GoalTemplate
{
	@Override
	public String getId()
	{
		return "max-cape";
	}

	@Override
	public String getName()
	{
		return "Max cape";
	}

	@Override
	public String getDescription()
	{
		return "Level 99 in every skill (Base N with N = 99)";
	}

	@Override
	public String getCategory()
	{
		return TemplateCategories.CAPES;
	}

	@Override
	public List<String> getTags()
	{
		return List.of("max", "max cape", "maxed", "maxing", "all 99s", "base 99", "cape");
	}

	@Override
	public GoalIcon getIcon()
	{
		return GoalIcon.item(ItemID.SKILLCAPE_MAX);
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		return BaseLevelTemplate.build(Experience.MAX_REAL_LEVEL, "Max cape", GoalIcon.item(ItemID.SKILLCAPE_MAX));
	}
}
