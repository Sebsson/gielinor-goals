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

	@Inject
	public TemplateCatalog(Gson gson)
	{
		List<GoalTemplate> all = new ArrayList<>();
		all.add(new QuestCapeTemplate(QuestCapeExclusions.load(gson)));
		all.add(new DiaryCapeTemplate());
		all.add(new BaseLevelTemplate());
		// Handplockade templates från JSON-filer i resources.
		all.addAll(JsonTemplates.load(gson));
		templates = Collections.unmodifiableList(all);
	}

	public List<GoalTemplate> getTemplates()
	{
		return templates;
	}

	/**
	 * @return templaten med id:t, eller null om den inte finns (t.ex. borttagen i en senare version)
	 */
	@javax.annotation.Nullable
	public GoalTemplate find(@javax.annotation.Nullable String id)
	{
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
