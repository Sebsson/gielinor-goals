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
import com.gielinorgoals.model.ItemRequirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/**
 * Ett områdes diary-föremål som en kedja: nivå 4 har diaryn på elite och nivå 3 som delmål,
 * nivå 3 har hard och nivå 2, och så vidare ner till nivå 1 med easy. En lägre nivå räknas
 * som klar när man har nästa ({@code doneWhen}), eftersom föremålen byts upp.
 */
public class DiaryRewardsTemplate implements GoalTemplate
{
	private final DiaryRewards rewards;
	private final TemplateParameter area;

	public DiaryRewardsTemplate(DiaryRewards rewards)
	{
		this.rewards = rewards;
		List<TemplateParameter.Option> options = new ArrayList<>();
		for (DiaryRewards.Reward reward : rewards.getRewards())
		{
			options.add(new TemplateParameter.Option(reward.getArea().name(),
				reward.getArea().getDisplayName() + " (" + reward.getName() + ")"));
		}
		if (options.isEmpty())
		{
			// Filen gick inte att läsa; ett val som ger ett tomt förslag i stället för en krasch.
			options.add(new TemplateParameter.Option(DiaryArea.ARDOUGNE.name(), DiaryArea.ARDOUGNE.getDisplayName()));
		}
		this.area = TemplateParameter.choice("area", "Area", options, options.get(0).getValue());
	}

	@Override
	public String getId()
	{
		return "diary-rewards";
	}

	@Override
	public String getName()
	{
		return "Diary reward items";
	}

	@Override
	public String getDescription()
	{
		return "One area's diary item from tier 1 to 4, e.g. Varrock armour 1-4";
	}

	@Override
	public String getCategory()
	{
		return TemplateCategories.DIARIES;
	}

	@Override
	public List<String> getTags()
	{
		List<String> tags = new ArrayList<>(List.of("diary", "diaries", "diary rewards", "diary items", "ardy cloak",
			"explorers ring", "mory legs", "sea boots", "rada", "kourend blessing"));
		for (DiaryRewards.Reward reward : rewards.getRewards())
		{
			tags.add(reward.getName().toLowerCase(Locale.ROOT));
		}
		return tags;
	}

	@Override
	public GoalIcon getIcon()
	{
		return GoalIcon.item(ItemID.VARROCK_ARMOUR_ELITE);
	}

	@Override
	public List<TemplateParameter> getParameters()
	{
		return List.of(area);
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		return build(parameters, Map.of());
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters, Map<String, String> choices)
	{
		DiaryRewards.Reward reward = rewards.get(DiaryArea.valueOf(area.choiceIn(choices)));
		if (reward == null)
		{
			return TemplateNode.aggregate("Diary reward items");
		}

		DiaryTier[] tiers = DiaryTier.values();
		TemplateNode root = TemplateNode.aggregate(reward.getName() + " 1-4");
		root.setIcon(GoalIcon.item(reward.getItems().get(tiers.length - 1)));
		root.setWikiPage(reward.getName());
		// Byggs nerifrån: nivå 1 först, varje nivå läggs sedan under nästa.
		TemplateNode below = null;
		for (int i = 0; i < tiers.length; i++)
		{
			TemplateNode item = TemplateNode.requirement(
				new ItemRequirement(reward.getItems().get(i), reward.itemName(i), 1, true));
			item.add(TemplateNode.requirement(new DiaryRequirement(reward.getArea(), tiers[i])));
			if (below != null)
			{
				item.add(below);
				below.setDoneWhen(List.of(item.getKey()));
			}
			below = item;
		}
		root.add(below);
		return root;
	}
}
