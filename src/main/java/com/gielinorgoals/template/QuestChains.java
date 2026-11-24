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

import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.SkillRequirement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Fäller ut quest-noder i ett template-förslag till hela sin kedja: förkrav-quests
 * (rekursivt) som delmål, och questets skill- och quest point-krav som egna mål.
 *
 * <p>Dubbletter undviks på två sätt:
 * <ul>
 * <li>Ett quest fälls bara ut första gången det dyker upp i förslaget, räknat i samma ordning
 * som insättningen går igenom trädet (förälder före barn, barnen i tur och ordning). Senare
 * förekomster blir noder utan barn, och insättningen kopplar dem till det första målet.</li>
 * <li>Ett quest som redan finns som mål fälls inte ut: det kopplas in och behåller sina egna delmål.</li>
 * <li>Per skill blir bara det högsta kravet i hela förslaget ett mål, under varje quest som
 * har just det kravet. Lägre krav i samma skill tas inte med, eftersom det högsta täcker dem.
 * Samma gäller quest points.</li>
 * </ul>
 * Quests som saknas i {@link QuestRequirementsDb} förblir vanliga quest-mål utan kedja.
 */
public final class QuestChains
{
	private QuestChains()
	{
	}

	/**
	 * Lägger till kedjorna i förslaget, på plats.
	 *
	 * @return förslaget, för kedjade anrop
	 */
	public static TemplateNode expand(TemplateNode root, QuestRequirementsDb db)
	{
		return expand(root, db, null);
	}

	/**
	 * @param existing målen som redan finns, eller null; quests som redan är mål fälls inte ut
	 */
	public static TemplateNode expand(TemplateNode root, QuestRequirementsDb db, @javax.annotation.Nullable GoalTree existing)
	{
		return expand(root, db, existing, null);
	}

	private static TemplateNode expand(TemplateNode root, QuestRequirementsDb db,
		@javax.annotation.Nullable GoalTree existing, @javax.annotation.Nullable TemplateNode alwaysExpand)
	{
		// Del-templates i ett paket fälls ut var för sig, som när de sätts in ensamma, så att en
		// del ser likadan ut i paketet som när den byggs om för sin egen uppdatering.
		for (TemplateNode part : partRoots(root))
		{
			expand(part, db, existing, alwaysExpand);
		}
		List<TemplateNode> questNodes = new ArrayList<>();
		collectOwnQuestNodes(root, root, questNodes);
		if (questNodes.isEmpty())
		{
			return root;
		}

		Set<Integer> closure = closure(questNodes, db);
		Map<Skill, Integer> maxLevel = new EnumMap<>(Skill.class);
		Map<Skill, Boolean> boostableAtMax = new EnumMap<>(Skill.class);
		int maxQuestPoints = 0;
		for (int id : closure)
		{
			QuestRequirementsDb.Entry entry = db.get(id);
			if (entry == null)
			{
				continue;
			}
			for (QuestRequirementsDb.SkillReq req : entry.getSkills())
			{
				Skill skill = skill(req.getSkill());
				if (skill == null)
				{
					continue;
				}
				int current = maxLevel.getOrDefault(skill, 0);
				if (req.getLevel() > current)
				{
					maxLevel.put(skill, req.getLevel());
					boostableAtMax.put(skill, req.isBoostable());
				}
				else if (req.getLevel() == current)
				{
					// Boostbart bara om alla quests med högsta kravet tillåter boost.
					boostableAtMax.put(skill, boostableAtMax.get(skill) && req.isBoostable());
				}
			}
			if (entry.getQuestPoints() != null)
			{
				maxQuestPoints = Math.max(maxQuestPoints, entry.getQuestPoints());
			}
		}

		Expansion expansion = new Expansion(db, closure, maxLevel, boostableAtMax, maxQuestPoints);
		expansion.alwaysExpand = alwaysExpand;
		if (existing != null)
		{
			for (com.gielinorgoals.model.Goal goal : existing.flatten())
			{
				if (goal.getRequirement() instanceof QuestRequirement)
				{
					expansion.existingQuests.add(((QuestRequirement) goal.getRequirement()).getQuestId());
				}
			}
		}
		expansion.visit(root);
		return root;
	}

	/**
	 * Ett nytt förslag för ett enda quest med hela sin kedja, t.ex. när ett quest-mål
	 * skapas med "Add the quest's requirements as sub-goals".
	 */
	public static TemplateNode forQuest(String title, QuestRequirement requirement, boolean aggregate,
		QuestRequirementsDb db, @javax.annotation.Nullable GoalTree existing)
	{
		TemplateNode root = new TemplateNode(title, requirement, aggregate);
		// Roten är det nya målet självt och fälls alltid ut.
		return expand(root, db, existing, root);
	}

	/**
	 * @return om förslaget har quest-noder som kan fällas ut
	 */
	public static boolean hasQuestNodes(TemplateNode root)
	{
		List<TemplateNode> nodes = new ArrayList<>();
		collectQuestNodes(root, nodes);
		return !nodes.isEmpty();
	}

	// De översta noderna under roten som kommer från en template-referens.
	private static List<TemplateNode> partRoots(TemplateNode root)
	{
		List<TemplateNode> parts = new ArrayList<>();
		collectPartRoots(root, parts);
		return parts;
	}

	private static void collectPartRoots(TemplateNode node, List<TemplateNode> out)
	{
		for (TemplateNode child : node.getChildren())
		{
			if (child.getTemplateId() != null)
			{
				out.add(child);
			}
			else
			{
				collectPartRoots(child, out);
			}
		}
	}

	// Quest-noder under roten, utom de i del-templates (de fälls ut för sig).
	private static void collectOwnQuestNodes(TemplateNode node, TemplateNode root, List<TemplateNode> out)
	{
		if (node != root && node.getTemplateId() != null)
		{
			return;
		}
		if (node.getRequirement() instanceof QuestRequirement && node.isExpandChain())
		{
			out.add(node);
		}
		for (TemplateNode child : node.getChildren())
		{
			collectOwnQuestNodes(child, root, out);
		}
	}

	private static void collectQuestNodes(TemplateNode node, List<TemplateNode> out)
	{
		if (node.getRequirement() instanceof QuestRequirement && node.isExpandChain())
		{
			out.add(node);
		}
		for (TemplateNode child : node.getChildren())
		{
			collectQuestNodes(child, out);
		}
	}

	// Alla quests som ingår i kedjorna, inklusive questen själva.
	private static Set<Integer> closure(List<TemplateNode> questNodes, QuestRequirementsDb db)
	{
		Set<Integer> closure = new LinkedHashSet<>();
		Deque<Integer> queue = new ArrayDeque<>();
		for (TemplateNode node : questNodes)
		{
			queue.add(((QuestRequirement) node.getRequirement()).getQuestId());
		}
		while (!queue.isEmpty())
		{
			int id = queue.poll();
			if (!closure.add(id))
			{
				continue;
			}
			QuestRequirementsDb.Entry entry = db.get(id);
			if (entry != null)
			{
				for (QuestRequirementsDb.Ref ref : entry.getQuests())
				{
					queue.add(ref.getId());
				}
			}
		}
		return closure;
	}

	@javax.annotation.Nullable
	private static Skill skill(String name)
	{
		try
		{
			return Skill.valueOf(name);
		}
		catch (IllegalArgumentException | NullPointerException e)
		{
			return null;
		}
	}

	private static final class Expansion
	{
		private final QuestRequirementsDb db;
		private final Set<Integer> closure;
		private final Map<Skill, Integer> maxLevel;
		private final Map<Skill, Boolean> boostableAtMax;
		private final int maxQuestPoints;
		// Roten i forQuest fälls ut även om samma quest redan finns som mål.
		@javax.annotation.Nullable
		private TemplateNode alwaysExpand;
		private final Set<Integer> expanded = new HashSet<>();
		private final Set<Integer> existingQuests = new HashSet<>();
		// Ett delvis krav visas en gång, även om flera quests har det.
		private final Set<Integer> partialsAdded = new HashSet<>();
		private final Map<Integer, String> questNames = new HashMap<>();

		private Expansion(QuestRequirementsDb db, Set<Integer> closure, Map<Skill, Integer> maxLevel,
			Map<Skill, Boolean> boostableAtMax, int maxQuestPoints)
		{
			this.db = db;
			this.closure = closure;
			this.maxLevel = maxLevel;
			this.boostableAtMax = boostableAtMax;
			this.maxQuestPoints = maxQuestPoints;
			for (Quest quest : Quest.values())
			{
				questNames.put(quest.getId(), quest.getName());
			}
		}

		// Förälder före barn, barnen i ordning: samma ordning som insättningen och planeringen.
		private void visit(TemplateNode node)
		{
			if (node.getRequirement() instanceof QuestRequirement && node.isExpandChain())
			{
				int id = ((QuestRequirement) node.getRequirement()).getQuestId();
				if ((node == alwaysExpand || !existingQuests.contains(id)) && expanded.add(id))
				{
					addDirectRequirements(node, id);
				}
			}
			for (TemplateNode child : new ArrayList<>(node.getChildren()))
			{
				// En del-template är redan utfälld för sig.
				if (child.getTemplateId() == null)
				{
					visit(child);
				}
			}
		}

		// Questets direkta krav som barn; förkrav-questen fälls ut när besöket når dem.
		private void addDirectRequirements(TemplateNode node, int questId)
		{
			QuestRequirementsDb.Entry entry = db.get(questId);
			if (entry == null)
			{
				return;
			}

			for (QuestRequirementsDb.Ref ref : entry.getQuests())
			{
				node.add(TemplateNode.requirement(new QuestRequirement(ref.getId(), name(ref.getId(), ref.getName()))));
			}

			// Ett delvis krav behövs inte om samma quest ändå ska klaras helt i kedjan.
			for (QuestRequirementsDb.Partial partial : entry.getPartial())
			{
				if (!closure.contains(partial.getId()) && partialsAdded.add(partial.getId()))
				{
					TemplateNode partialNode = new TemplateNode(partial.getText() + " (partial requirement)", null, false);
					partialNode.setIcon(com.gielinorgoals.model.GoalIcon.side(com.gielinorgoals.model.GoalIcon.SideIcon.QUEST));
					node.add(partialNode);
				}
			}

			for (QuestRequirementsDb.SkillReq req : entry.getSkills())
			{
				Skill skill = skill(req.getSkill());
				if (skill != null && req.getLevel() == maxLevel.getOrDefault(skill, -1)
					&& req.getLevel() >= SkillRequirement.MIN_LEVEL)
				{
					SkillRequirement requirement = SkillRequirement.level(skill, req.getLevel());
					String title = boostableAtMax.getOrDefault(skill, false)
						? requirement.describe() + " (boostable)"
						: "";
					node.add(new TemplateNode(title, requirement, false));
				}
			}

			if (entry.getQuestPoints() != null && entry.getQuestPoints() == maxQuestPoints)
			{
				node.add(TemplateNode.requirement(new QuestPointsRequirement(maxQuestPoints)));
			}
		}

		private String name(int id, String fallback)
		{
			return questNames.getOrDefault(id, fallback);
		}
	}
}
