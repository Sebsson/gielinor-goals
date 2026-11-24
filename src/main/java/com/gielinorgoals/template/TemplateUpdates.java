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

import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Value;

/**
 * Jämför en insatt template med vad den ger i dag: templaten byggs om med samma parametrar
 * och nodernas nycklar jämförs med målen under roten. Det som saknas under roten är
 * tillägg; mål som kom från templaten men inte längre finns i den är borttagna noder.
 * Ingenting ändras här; det gör {@code GoalManager} när användaren har valt.
 */
@Singleton
public class TemplateUpdates
{
	// Parameter på roten: 1 om templaten sattes in med hela quest-kedjor.
	public static final String QUEST_CHAINS_PARAM = "questChains";

	/**
	 * En ny nod (med sitt delträd) och målet den ska läggas under.
	 */
	@Value
	public static class Addition
	{
		TemplateNode node;
		String parentGoalId;
	}

	@Getter
	public static class Plan
	{
		private final Goal root;
		private final List<Addition> additions = new ArrayList<>();
		// Mål som templaten skapade men som inte längre finns i den.
		private final List<Goal> removed = new ArrayList<>();

		private Plan(Goal root)
		{
			this.root = root;
		}

		public boolean isEmpty()
		{
			return additions.isEmpty() && removed.isEmpty();
		}
	}

	private final TemplateCatalog catalog;
	private final QuestRequirementsDb questRequirements;
	// Rotens id → (målens revision, om det finns en uppdatering). Räknas om bara när målen ändrats.
	private final Map<String, long[]> cache = new HashMap<>();

	@Inject
	public TemplateUpdates(TemplateCatalog catalog, QuestRequirementsDb questRequirements)
	{
		this.catalog = catalog;
		this.questRequirements = questRequirements;
	}

	/**
	 * Billig fråga för rotraden: cachas per rot och revision, så att templaten bara byggs om
	 * när målen faktiskt har ändrats (t.ex. när profilen laddas), aldrig per tick.
	 */
	public boolean hasUpdate(GoalTree tree, Goal root, long revision)
	{
		if (root.getTemplateId() == null)
		{
			return false;
		}
		long[] cached = cache.get(root.getId());
		if (cached != null && cached[0] == revision)
		{
			return cached[1] == 1;
		}
		Plan plan = plan(tree, root);
		boolean update = plan != null && !plan.isEmpty();
		cache.put(root.getId(), new long[]{revision, update ? 1 : 0});
		return update;
	}

	/**
	 * @return tillägg och borttagna noder, eller null om templaten inte finns längre eller
	 * behöver spelets data (då bygger {@link TemplateBuilder} om den och
	 * {@link #plan(GoalTree, Goal, TemplateNode)} används)
	 */
	@Nullable
	public Plan plan(GoalTree tree, Goal root)
	{
		TemplateNode template = rebuild(root);
		return template == null ? null : plan(tree, root, template);
	}

	/**
	 * Jämför målen under roten med en färdigbyggd template, t.ex. en som byggts på klienttråden.
	 */
	public Plan plan(GoalTree tree, Goal root, TemplateNode template)
	{
		// Alla mål under roten (även inkopplade och delade), i visningsordning.
		List<Goal> below = new ArrayList<>();
		collect(tree, root, below, new HashSet<>());
		Map<String, Goal> present = new LinkedHashMap<>();
		for (Goal goal : below)
		{
			String key = TemplateKeys.of(goal);
			if (key != null)
			{
				present.putIfAbsent(key, goal);
			}
		}

		Set<String> ignored = root.getTemplateIgnoredKeys() == null
			? Collections.emptySet()
			: new HashSet<>(root.getTemplateIgnoredKeys());
		Plan plan = new Plan(root);
		Set<String> added = new HashSet<>();
		findAdditions(template, root, present, ignored, added, plan);

		Set<String> templateKeys = new HashSet<>();
		collectKeys(template, templateKeys);
		for (Goal goal : below)
		{
			if (goal.getTemplateNodeKey() != null && !templateKeys.contains(goal.getTemplateNodeKey())
				&& !plan.removed.contains(goal))
			{
				plan.removed.add(goal);
			}
		}
		return plan;
	}

	/**
	 * Templaten byggd som när den sattes in: samma parametrar, samma val och samma val av
	 * quest-kedjor. Templates som behöver spelets data byggs inte här (den här metoden körs
	 * på EDT); de ger null och byggs i stället med {@link #rebuildAsync}.
	 */
	@Nullable
	public TemplateNode rebuild(Goal root)
	{
		GoalTemplate template = catalog.find(root.getTemplateId());
		if (template == null || template.needsClientData())
		{
			return null;
		}
		return finish(root, template.build(numbers(root), choices(root)));
	}

	/**
	 * Som {@link #rebuild} men för alla templates: de som behöver spelets data byggs på
	 * klienttråden. {@code callback} körs på EDT; null om templaten inte finns längre.
	 */
	public void rebuildAsync(Goal root, TemplateBuilder builder, java.util.function.Consumer<TemplateBuilder.Result> callback)
	{
		GoalTemplate template = catalog.find(root.getTemplateId());
		if (template == null)
		{
			callback.accept(null);
			return;
		}
		builder.build(template, numbers(root), choices(root), result -> callback.accept(result.isReady()
			? new TemplateBuilder.Result(finish(root, result.getNode()), null)
			: result));
	}

	private TemplateNode finish(Goal root, TemplateNode node)
	{
		if (Integer.valueOf(1).equals(numbers(root).get(QUEST_CHAINS_PARAM)))
		{
			// Full utfällning; quests som redan fanns när templaten sattes in ligger under
			// roten och räknas därför som befintliga i jämförelsen.
			QuestChains.expand(node, questRequirements, null);
		}
		return node;
	}

	private static Map<String, Integer> numbers(Goal root)
	{
		return root.getTemplateParams() == null ? Collections.emptyMap() : root.getTemplateParams();
	}

	// Äldre sparade rötter saknar val; då gäller varje parameters standardval.
	private static Map<String, String> choices(Goal root)
	{
		return root.getTemplateChoices() == null ? Collections.emptyMap() : root.getTemplateChoices();
	}

	private static void findAdditions(TemplateNode node, Goal parentGoal, Map<String, Goal> present, Set<String> ignored,
		Set<String> added, Plan plan)
	{
		for (TemplateNode child : node.getChildren())
		{
			String key = child.getKey();
			if (ignored.contains(key))
			{
				continue;
			}
			Goal goal = present.get(key);
			if (goal != null)
			{
				// Bara mål som templaten skapade kan få nya delmål; ett inkopplat eget mål
				// behåller sina egna delmål.
				if (goal.getTemplateNodeKey() != null)
				{
					findAdditions(child, goal, present, ignored, added, plan);
				}
				continue;
			}
			// Samma nyckel flera gånger i templaten blir ett tillägg; insättningen kopplar resten.
			if (added.add(key))
			{
				plan.additions.add(new Addition(child, parentGoal.getId()));
			}
		}
	}

	private static void collectKeys(TemplateNode node, Set<String> out)
	{
		for (TemplateNode child : node.getChildren())
		{
			out.add(child.getKey());
			collectKeys(child, out);
		}
	}

	private static void collect(GoalTree tree, Goal goal, List<Goal> out, Set<String> visited)
	{
		for (Goal child : tree.getChildren(goal))
		{
			if (visited.add(child.getId()))
			{
				out.add(child);
				collect(tree, child, out, visited);
			}
		}
	}
}
