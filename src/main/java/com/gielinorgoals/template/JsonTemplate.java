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
import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.SkillRequirement;
import com.gielinorgoals.model.TotalLevelRequirement;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * En handplockad template från en JSON-fil i resources. Formatet är gjort för att
 * skrivas för hand: varje nod har valfri titel, valfri räknarflagga, högst ett krav och
 * valfria barn. Krav anges med spelets enum-namn, t.ex.
 * <pre>
 * {"quest": "RECIPE_FOR_DISASTER"}
 * {"skill": "COOKING", "level": 70}
 * {"questPoints": 175}   {"totalLevel": 1500}   {"combatLevel": 100}
 * {"diary": {"area": "VARROCK", "tier": "HARD"}}
 * {"item": {"id": 7462, "name": "Barrows gloves", "quantity": 1}}
 * </pre>
 * Filen valideras när den läses; en trasig fil ger ingen template alls, aldrig en halv.
 */
public class JsonTemplate implements GoalTemplate
{
	// Fälten fylls av Gson.
	static final class FileSpec
	{
		// Stabilt id; om det saknas används filnamnet utan .json.
		String id;
		String name;
		String description;
		NodeSpec root;
	}

	static final class NodeSpec
	{
		String title;
		Boolean aggregate;
		String quest;
		String skill;
		Integer level;
		Integer questPoints;
		Integer totalLevel;
		Integer combatLevel;
		DiarySpec diary;
		ItemSpec item;
		List<NodeSpec> children;
	}

	static final class DiarySpec
	{
		String area;
		String tier;
	}

	static final class ItemSpec
	{
		Integer id;
		String name;
		Long quantity;
		Boolean variants;
	}

	private final String id;
	private final String name;
	private final String description;
	private final NodeSpec root;

	private JsonTemplate(String id, String name, String description, NodeSpec root)
	{
		this.id = id;
		this.name = name;
		this.description = description;
		this.root = root;
	}

	/**
	 * @throws IllegalArgumentException om filen saknar fält eller har okända namn
	 */
	static JsonTemplate from(FileSpec spec)
	{
		return from(spec, null);
	}

	/**
	 * @param fallbackId id om filen inte anger något, normalt filnamnet utan .json
	 */
	static JsonTemplate from(FileSpec spec, @Nullable String fallbackId)
	{
		if (spec == null || spec.name == null || spec.root == null)
		{
			throw new IllegalArgumentException("Template needs a name and a root");
		}
		String id = spec.id != null ? spec.id
			: fallbackId != null ? fallbackId
			: spec.name.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
		JsonTemplate template = new JsonTemplate(id, spec.name, spec.description == null ? "" : spec.description,
			spec.root);
		// Bygg en gång direkt, så att fel i filen märks när den läses och inte i dialogen.
		template.build(Map.of());
		return template;
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

	@Nullable
	@Override
	public Requirement getIconRequirement()
	{
		return toRequirement(root);
	}

	// Handplockade templates visar hela quest-kedjorna som standard.
	@Override
	public boolean defaultExpandQuestChains()
	{
		return true;
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		return toNode(root);
	}

	private static TemplateNode toNode(NodeSpec spec)
	{
		Requirement requirement = toRequirement(spec);
		String title = spec.title == null ? "" : spec.title;
		if (title.isEmpty() && requirement == null)
		{
			throw new IllegalArgumentException("A node needs a title or a requirement");
		}
		TemplateNode node = new TemplateNode(title, requirement, Boolean.TRUE.equals(spec.aggregate));
		if (spec.children != null)
		{
			for (NodeSpec child : spec.children)
			{
				node.add(toNode(child));
			}
		}
		return node;
	}

	@Nullable
	private static Requirement toRequirement(NodeSpec spec)
	{
		if (spec.quest != null)
		{
			Quest quest = Quest.valueOf(spec.quest);
			return new QuestRequirement(quest.getId(), quest.getName());
		}
		if (spec.skill != null)
		{
			if (spec.level == null)
			{
				throw new IllegalArgumentException("Skill " + spec.skill + " needs a level");
			}
			return SkillRequirement.level(Skill.valueOf(spec.skill), spec.level);
		}
		if (spec.questPoints != null)
		{
			return new QuestPointsRequirement(spec.questPoints);
		}
		if (spec.totalLevel != null)
		{
			return new TotalLevelRequirement(spec.totalLevel);
		}
		if (spec.combatLevel != null)
		{
			return new CombatLevelRequirement(spec.combatLevel);
		}
		if (spec.diary != null)
		{
			return new DiaryRequirement(DiaryArea.valueOf(spec.diary.area), DiaryTier.valueOf(spec.diary.tier));
		}
		if (spec.item != null)
		{
			if (spec.item.id == null || spec.item.name == null)
			{
				throw new IllegalArgumentException("Item needs an id and a name");
			}
			return new ItemRequirement(spec.item.id, spec.item.name,
				spec.item.quantity == null ? 1 : spec.item.quantity, Boolean.TRUE.equals(spec.item.variants));
		}
		return null;
	}
}
