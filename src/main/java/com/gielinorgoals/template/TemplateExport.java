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

import com.gielinorgoals.model.CombatAchievementRequirement;
import com.gielinorgoals.model.CombatLevelRequirement;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DropSource;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalIcon;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.SkillRequirement;
import com.gielinorgoals.model.TotalLevelRequirement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Getter;
import net.runelite.api.Quest;

/**
 * Gör om ett mål och hela delgrafen under det till template-formatet ({@link JsonTemplate}).
 * Delade delmål inom delgrafen blir delade noder ({@code id}/{@code ref}); kopplingar till mål
 * utanför delgrafen följer inte med. Bara själva målen sparas, aldrig profilens data:
 * inte klar-status, datum, räknarvärden eller kill count.
 */
public final class TemplateExport
{
	/**
	 * Resultatet: templaten och varje måls nod-nyckel, så att målen kan kopplas till den
	 * sparade templaten.
	 */
	public static final class Result
	{
		private final JsonTemplate.FileSpec spec;
		// Mål-id → nyckeln för målets nod i templaten.
		@Getter
		private final Map<String, String> keys;
		@Getter
		private final int nodeCount;
		// XP-mål räknade från spelarens XP när de skapades; de sparas som manuella mål.
		@Getter
		private final int convertedXpGains;

		JsonTemplate.FileSpec getSpec()
		{
			return spec;
		}

		public String getName()
		{
			return spec.name;
		}

		/**
		 * Templaten som den kommer att se ut när den sätts in, för förhandsvisningen.
		 */
		public TemplateNode preview()
		{
			return JsonTemplate.from(spec).build(Map.of());
		}

		private Result(JsonTemplate.FileSpec spec, Map<String, String> keys, int nodeCount, int convertedXpGains)
		{
			this.spec = spec;
			this.keys = Collections.unmodifiableMap(keys);
			this.nodeCount = nodeCount;
			this.convertedXpGains = convertedXpGains;
		}
	}

	private final GoalTree tree;
	private final Set<String> inside = new HashSet<>();
	private final Map<String, Integer> parentsInside = new HashMap<>();
	// Mål-id → lokalt id för delade mål som redan skrivits ut.
	private final Map<String, String> written = new HashMap<>();
	private final Map<String, String> keys = new LinkedHashMap<>();
	private final Map<String, JsonTemplate.NodeSpec> specs = new HashMap<>();
	private int nodeCount;
	private int convertedXpGains;
	private int nextId = 1;

	private TemplateExport(GoalTree tree)
	{
		this.tree = tree;
	}

	public static Result export(GoalTree tree, Goal root, String name, String description, List<String> tags)
	{
		TemplateExport export = new TemplateExport(tree);
		export.collect(root);
		export.countParents();
		JsonTemplate.NodeSpec rootSpec = export.write(root);
		export.filterDoneWhen();
		return export.result(rootSpec, name, description, tags);
	}

	/**
	 * Alla profilens mål, som säkerhetskopia: en grupp "All goals" med varje rotmål som delmål.
	 * Vid import blir gruppens delmål egna rotmål igen, ihopslagna med de befintliga.
	 */
	public static Result exportAll(GoalTree tree)
	{
		TemplateExport export = new TemplateExport(tree);
		List<Goal> roots = tree.getRoots();
		roots.forEach(export::collect);
		export.countParents();
		JsonTemplate.NodeSpec container = new JsonTemplate.NodeSpec();
		container.title = ALL_GOALS;
		container.aggregate = Boolean.TRUE;
		container.children = new ArrayList<>();
		for (Goal root : roots)
		{
			container.children.add(export.write(root));
		}
		export.filterDoneWhen();
		return export.result(container, ALL_GOALS, "Every goal from a profile", List.of());
	}

	public static final String ALL_GOALS = "All goals";

	private void countParents()
	{
		for (String id : inside)
		{
			for (Goal child : tree.getChildren(tree.find(id)))
			{
				parentsInside.merge(child.getId(), 1, Integer::sum);
			}
		}
	}

	private Result result(JsonTemplate.NodeSpec rootSpec, String name, String description, List<String> tags)
	{
		JsonTemplate.FileSpec spec = new JsonTemplate.FileSpec();
		spec.formatVersion = JsonTemplate.FORMAT_VERSION;
		spec.name = name;
		spec.description = description;
		spec.tags = new ArrayList<>(tags);
		// Kedjorna som fanns under målen sparas som de är; de fälls inte ut en gång till.
		spec.expandQuestChains = false;
		spec.root = rootSpec;
		return new Result(spec, keys, nodeCount, convertedXpGains);
	}

	private void collect(Goal goal)
	{
		if (!inside.add(goal.getId()))
		{
			return;
		}
		for (Goal child : tree.getChildren(goal))
		{
			collect(child);
		}
	}

	private JsonTemplate.NodeSpec write(Goal goal)
	{
		String shared = written.get(goal.getId());
		if (shared != null)
		{
			JsonTemplate.NodeSpec ref = new JsonTemplate.NodeSpec();
			ref.ref = shared;
			return ref;
		}

		nodeCount++;
		JsonTemplate.NodeSpec spec = new JsonTemplate.NodeSpec();
		if (parentsInside.getOrDefault(goal.getId(), 0) > 1)
		{
			spec.id = "n" + nextId++;
			written.put(goal.getId(), spec.id);
		}
		boolean hasRequirement = writeRequirement(goal, spec);
		String title = goal.getTitle() == null ? "" : goal.getTitle().trim();
		if (!hasRequirement && title.isEmpty() && goal.getRequirement() != null)
		{
			// Ett mål vars krav inte sparas (XP räknat från en start) behåller sin text.
			title = goal.getRequirement().describe();
		}
		spec.title = title.isEmpty() ? null : title;
		spec.aggregate = goal.isAggregate() ? Boolean.TRUE : null;
		spec.note = goal.getNote();
		spec.wikiPage = goal.getWikiPage();
		spec.icon = icon(goal.getIcon());
		if (goal.getDropSource() != null && goal.getRequirement() instanceof ItemRequirement)
		{
			spec.dropSource = dropSource(goal.getDropSource());
		}
		if (goal.getDoneWhen() != null && !goal.getDoneWhen().isEmpty())
		{
			spec.doneWhen = new ArrayList<>(goal.getDoneWhen());
		}

		String key = TemplateKeys.of(requirementOf(spec));
		keys.put(goal.getId(), key != null ? key : "group:" + (spec.title == null ? "" : spec.title.toLowerCase(Locale.ROOT)));
		specs.put(goal.getId(), spec);

		List<Goal> children = tree.getChildren(goal);
		if (!children.isEmpty())
		{
			spec.children = new ArrayList<>();
			for (Goal child : children)
			{
				spec.children.add(write(child));
			}
			// Questets kedja finns redan som delmål och ska inte fällas ut igen.
			if (spec.quest != null)
			{
				spec.expandChain = false;
			}
		}
		return spec;
	}

	// doneWhen behålls bara för nycklar till mål inom delgrafen.
	private void filterDoneWhen()
	{
		Set<String> present = new HashSet<>(keys.values());
		for (JsonTemplate.NodeSpec spec : specs.values())
		{
			if (spec.doneWhen != null)
			{
				spec.doneWhen.removeIf(key -> !present.contains(key));
				if (spec.doneWhen.isEmpty())
				{
					spec.doneWhen = null;
				}
			}
		}
	}

	// Kravet som noden får när den läses in igen; för nyckeln.
	@Nullable
	private static Requirement requirementOf(JsonTemplate.NodeSpec spec)
	{
		JsonTemplate.FileSpec file = new JsonTemplate.FileSpec();
		file.name = "key";
		JsonTemplate.NodeSpec copy = new JsonTemplate.NodeSpec();
		copy.title = "key";
		copy.quest = spec.quest;
		copy.skill = spec.skill;
		copy.level = spec.level;
		copy.xp = spec.xp;
		copy.combatAchievement = spec.combatAchievement;
		copy.questPoints = spec.questPoints;
		copy.totalLevel = spec.totalLevel;
		copy.combatLevel = spec.combatLevel;
		copy.diary = spec.diary;
		copy.item = spec.item;
		copy.killCount = spec.killCount;
		copy.points = spec.points;
		file.root = copy;
		return JsonTemplate.from(file).build(Map.of()).getRequirement();
	}

	/**
	 * @return true om kravet sparades; false för mål utan krav och för XP räknat från en start
	 */
	private boolean writeRequirement(Goal goal, JsonTemplate.NodeSpec spec)
	{
		Requirement requirement = goal.getRequirement();
		if (requirement instanceof QuestRequirement)
		{
			Quest quest = quest(((QuestRequirement) requirement).getQuestId());
			if (quest == null)
			{
				// Questet finns inte i den här versionen av RuneLite; målet blir manuellt.
				return false;
			}
			spec.quest = quest.name();
			return true;
		}
		if (requirement instanceof SkillRequirement)
		{
			SkillRequirement skill = (SkillRequirement) requirement;
			if (skill.getMode() == SkillRequirement.Mode.XP_GAIN)
			{
				convertedXpGains++;
				return false;
			}
			spec.skill = skill.getSkill().name();
			if (skill.getMode() == SkillRequirement.Mode.LEVEL)
			{
				spec.level = (int) skill.getTarget();
			}
			else
			{
				spec.xp = skill.getTarget();
			}
			return true;
		}
		if (requirement instanceof QuestPointsRequirement)
		{
			spec.questPoints = ((QuestPointsRequirement) requirement).getTarget();
			return true;
		}
		if (requirement instanceof TotalLevelRequirement)
		{
			spec.totalLevel = ((TotalLevelRequirement) requirement).getTarget();
			return true;
		}
		if (requirement instanceof CombatLevelRequirement)
		{
			spec.combatLevel = ((CombatLevelRequirement) requirement).getTarget();
			return true;
		}
		if (requirement instanceof DiaryRequirement)
		{
			DiaryRequirement diary = (DiaryRequirement) requirement;
			spec.diary = new JsonTemplate.DiarySpec();
			spec.diary.area = diary.getArea().name();
			spec.diary.tier = diary.getTier().name();
			return true;
		}
		if (requirement instanceof ItemRequirement)
		{
			ItemRequirement item = (ItemRequirement) requirement;
			spec.item = new JsonTemplate.ItemSpec();
			spec.item.id = item.getItemId();
			spec.item.name = item.getItemName();
			spec.item.quantity = item.getQuantity() == 1 ? null : item.getQuantity();
			spec.item.includeVariants = item.isIncludeVariants() ? Boolean.TRUE : null;
			return true;
		}
		if (requirement instanceof KillCountRequirement)
		{
			KillCountRequirement kc = (KillCountRequirement) requirement;
			spec.killCount = activity(kc.getActivityType(), kc.getActivityId(), kc.getActivityName());
			spec.killCount.count = kc.getTarget();
			return true;
		}
		if (requirement instanceof com.gielinorgoals.model.PointsRequirement)
		{
			com.gielinorgoals.model.PointsRequirement points = (com.gielinorgoals.model.PointsRequirement) requirement;
			spec.points = new JsonTemplate.PointsSpec();
			spec.points.activity = points.getActivity().name();
			spec.points.count = points.getTarget();
			return true;
		}
		if (requirement instanceof CombatAchievementRequirement)
		{
			CombatAchievementRequirement ca = (CombatAchievementRequirement) requirement;
			spec.combatAchievement = new JsonTemplate.CombatAchievementSpec();
			spec.combatAchievement.id = ca.getTaskId();
			spec.combatAchievement.name = ca.getTaskName();
			spec.combatAchievement.tier = ca.getTierName();
			return true;
		}
		return false;
	}

	private static JsonTemplate.ActivitySpec dropSource(DropSource source)
	{
		JsonTemplate.ActivitySpec spec = activity(source.getActivityType(), source.getActivityId(),
			source.getActivityName());
		spec.rate = source.getRate();
		spec.perDrop = source.getPerDrop() == 1 ? null : source.getPerDrop();
		return spec;
	}

	// Hiscore-aktiviteter sparas med enum-namnet, NPC:er och egna räknare med sitt namn.
	private static JsonTemplate.ActivitySpec activity(KillCountRequirement.ActivityType type, String id, String name)
	{
		JsonTemplate.ActivitySpec spec = new JsonTemplate.ActivitySpec();
		switch (type)
		{
			case HISCORE:
				spec.hiscore = id;
				break;
			case NPC:
				spec.npc = name;
				break;
			default:
				spec.manual = name;
		}
		return spec;
	}

	@Nullable
	private static JsonTemplate.IconSpec icon(@Nullable GoalIcon icon)
	{
		if (icon == null)
		{
			return null;
		}
		JsonTemplate.IconSpec spec = new JsonTemplate.IconSpec();
		if (icon.getItemId() != null)
		{
			spec.item = icon.getItemId();
		}
		else if (icon.getActivity() != null)
		{
			spec.activity = icon.getActivity();
		}
		else if (icon.getSideIcon() != null)
		{
			spec.sideIcon = icon.getSideIcon().name();
		}
		else
		{
			return null;
		}
		return spec;
	}

	@Nullable
	private static Quest quest(int id)
	{
		for (Quest quest : Quest.values())
		{
			if (quest.getId() == id)
			{
				return quest;
			}
		}
		return null;
	}
}
