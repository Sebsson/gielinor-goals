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
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Quest;

/**
 * En quest-serie ur quest-databasen, med seriens quests i ordning och deras fulla kedjor
 * (kedjorna är påslagna från början i dialogen).
 */
public class QuestSeriesTemplate implements GoalTemplate
{
	private final QuestRequirementsDb questRequirements;
	private final TemplateParameter series;

	public QuestSeriesTemplate(QuestRequirementsDb questRequirements)
	{
		this.questRequirements = questRequirements;
		List<TemplateParameter.Option> options = new ArrayList<>();
		for (QuestRequirementsDb.Series entry : questRequirements.getSeries())
		{
			options.add(new TemplateParameter.Option(entry.getName(), entry.getName()));
		}
		if (options.isEmpty())
		{
			// Databasen saknar serier; ett val som ger ett tomt förslag i stället för en krasch.
			options.add(new TemplateParameter.Option("", "No series"));
		}
		this.series = TemplateParameter.choice("series", "Series", options, options.get(0).getValue());
	}

	@Override
	public String getId()
	{
		return "quest-series";
	}

	@Override
	public String getName()
	{
		return "Quest series";
	}

	@Override
	public String getDescription()
	{
		return "Every quest in a storyline, in order, with their full quest chains";
	}

	@Override
	public String getCategory()
	{
		return TemplateCategories.QUESTS;
	}

	@Override
	public List<String> getTags()
	{
		List<String> tags = new ArrayList<>(List.of("quest", "quests", "series", "quest series", "storyline", "lore"));
		for (QuestRequirementsDb.Series entry : questRequirements.getSeries())
		{
			tags.add(entry.getName().toLowerCase(Locale.ROOT));
		}
		return tags;
	}

	@Override
	public Requirement getIconRequirement()
	{
		return new QuestRequirement(Quest.COOKS_ASSISTANT.getId(), Quest.COOKS_ASSISTANT.getName());
	}

	@Override
	public boolean defaultExpandQuestChains()
	{
		return true;
	}

	@Override
	public List<TemplateParameter> getParameters()
	{
		return List.of(series);
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		return build(parameters, Map.of());
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters, Map<String, String> choices)
	{
		QuestRequirementsDb.Series chosen = questRequirements.series(series.choiceIn(choices));
		if (chosen == null)
		{
			return TemplateNode.aggregate("Quest series");
		}
		TemplateNode root = TemplateNode.aggregate(chosen.getName() + " quest series");
		root.setIcon(GoalIcon.side(GoalIcon.SideIcon.QUEST));
		root.setWikiPage("Quests/Series");
		for (QuestRequirementsDb.Ref quest : chosen.getQuests())
		{
			// expandChain är sant från början: hela kedjan fälls ut när kedjor är påslagna.
			root.add(TemplateNode.requirement(new QuestRequirement(quest.getId(), quest.getName())));
		}
		return root;
	}
}
