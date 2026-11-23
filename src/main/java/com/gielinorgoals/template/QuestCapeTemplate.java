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

import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;

/**
 * Quest point cape: ett räknarmål med ett quest-mål per quest i RuneLites {@link Quest},
 * utom miniquests och Recipe for Disasters delar (se {@link QuestCapeExclusions}).
 */
public class QuestCapeTemplate implements GoalTemplate
{
	private final QuestCapeExclusions exclusions;

	public QuestCapeTemplate(QuestCapeExclusions exclusions)
	{
		this.exclusions = exclusions;
	}

	@Override
	public String getName()
	{
		return "Quest cape";
	}

	@Override
	public String getDescription()
	{
		return "Every quest needed for the quest point cape (no miniquests)";
	}

	@Override
	public Requirement getIconRequirement()
	{
		return new QuestRequirement(Quest.COOKS_ASSISTANT.getId(), Quest.COOKS_ASSISTANT.getName());
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		List<Quest> quests = new ArrayList<>();
		for (Quest quest : Quest.values())
		{
			if (exclusions.countsForCape(quest))
			{
				quests.add(quest);
			}
		}
		// Bokstavsordning, som i spelets quest-lista.
		quests.sort(Comparator.comparing(Quest::getName, String.CASE_INSENSITIVE_ORDER));

		TemplateNode root = TemplateNode.aggregate("Quest cape");
		for (Quest quest : quests)
		{
			root.add(TemplateNode.requirement(new QuestRequirement(quest.getId(), quest.getName())));
		}
		return root;
	}
}
