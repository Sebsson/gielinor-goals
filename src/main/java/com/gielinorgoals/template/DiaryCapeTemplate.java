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
import com.gielinorgoals.model.Requirement;
import java.util.Map;

/**
 * Achievement diary cape: ett räknarmål per område med ett diary-mål per nivå,
 * genererat från {@link DiaryArea} och {@link DiaryTier}.
 */
public class DiaryCapeTemplate implements GoalTemplate
{
	@Override
	public String getId()
	{
		return "diary-cape";
	}

	@Override
	public String getName()
	{
		return "Achievement diary cape";
	}

	@Override
	public String getDescription()
	{
		return "Every achievement diary, Easy to Elite, grouped by area";
	}

	@Override
	public Requirement getIconRequirement()
	{
		return new DiaryRequirement(DiaryArea.values()[0], DiaryTier.EASY);
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		TemplateNode root = TemplateNode.aggregate("Achievement diary cape");
		for (DiaryArea area : DiaryArea.values())
		{
			TemplateNode areaNode = TemplateNode.aggregate(area.getDisplayName() + " diary");
			for (DiaryTier tier : DiaryTier.values())
			{
				areaNode.add(TemplateNode.requirement(new DiaryRequirement(area, tier)));
			}
			root.add(areaNode);
		}
		return root;
	}
}
