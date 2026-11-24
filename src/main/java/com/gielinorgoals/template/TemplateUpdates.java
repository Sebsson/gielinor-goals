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
		// Målet som noden ersätter (templatens "replaces"), eller null för ett rent tillägg.
		@javax.annotation.Nullable
		Goal replaces;

		public Addition(TemplateNode node, String parentGoalId)
		{
			this(node, parentGoalId, null);
		}

		public Addition(TemplateNode node, String parentGoalId, @javax.annotation.Nullable Goal replaces)
		{
			this.node = node;
			this.parentGoalId = parentGoalId;
			this.replaces = replaces;
		}
	}

	/**
	 * Ett befintligt mål vars krav har ändrats i templaten utan att nyckeln ändrats, t.ex. ett
	 * item-mål som fått motsvarigheter (countsAlso). Målet uppdateras där det står.
	 */
	@Value
	public static class Change
	{
		Goal goal;
		TemplateNode node;

		/**
		 * Nyckeln som sparas om användaren väljer bort ändringen, så att den inte föreslås igen.
		 */
		public String declineKey()
		{
			return CHANGE_KEY_PREFIX + node.getKey();
		}
	}

	static final String CHANGE_KEY_PREFIX = "change:";

	@Getter
	public static class Plan
	{
		private final Goal root;
		private final List<Addition> additions = new ArrayList<>();
		// Mål som templaten skapade men som inte längre finns i den (utom de som ersätts).
		private final List<Goal> removed = new ArrayList<>();
		private final List<Change> changes = new ArrayList<>();
		// Templatens nuvarande version och den som målen senast fick.
		private int templateVersion = 1;
		private int goalVersion = 1;

		private Plan(Goal root)
		{
			this.root = root;
		}

		public boolean isEmpty()
		{
			return additions.isEmpty() && removed.isEmpty() && changes.isEmpty();
		}

		public boolean isNewVersion()
		{
			return templateVersion > goalVersion;
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
		// Alla mål under roten (även inkopplade och delade), i visningsordning. Under ett delat mål
		// (som också hör till en annan template) går jämförelsen inte vidare; det målet sköts av
		// sin egen template.
		Set<String> foreign = foreignRoots(tree, root);
		List<Goal> below = new ArrayList<>();
		collect(tree, root, below, new HashSet<>(), foreign);
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
		GoalTemplate source = catalog.find(root.getTemplateId());
		plan.templateVersion = source == null ? 1 : source.getVersion();
		plan.goalVersion = root.getTemplateVersion() == null ? 1 : root.getTemplateVersion();
		Set<String> added = new HashSet<>();
		// Del-templates (i ett paket) jämförs med sin egen plan och sina egna inställningar.
		Map<String, Goal> parts = new LinkedHashMap<>();
		findAdditions(template, root, present, ignored, added, plan, tree, foreign, new HashSet<>(), parts);

		Set<String> templateKeys = new HashSet<>();
		collectKeys(template, templateKeys);
		// Mål som en nod ersätter föreslås bara tillsammans med bytet; har användaren valt bort
		// bytet ligger det gamla målet kvar utan att föreslås som borttaget.
		Set<String> replacedKeys = new HashSet<>();
		collectReplacedKeys(template, replacedKeys);
		for (Goal goal : below)
		{
			if (goal.getTemplateNodeKey() != null && !templateKeys.contains(goal.getTemplateNodeKey())
				&& !replacedKeys.contains(goal.getTemplateNodeKey())
				&& !plan.removed.contains(goal) && !isReplaced(plan, goal))
			{
				plan.removed.add(goal);
			}
		}

		// Paketet visar delarnas ändringar som sina egna; en del som paketet valt bort tas inte med.
		for (Goal part : parts.values())
		{
			Plan sub = plan(tree, part);
			if (sub == null)
			{
				continue;
			}
			for (Addition addition : sub.additions)
			{
				if (!ignored.contains(addition.getNode().getKey()) && added.add(addition.getNode().getKey()))
				{
					plan.additions.add(addition);
				}
			}
			for (Goal removed : sub.removed)
			{
				if (!plan.removed.contains(removed))
				{
					plan.removed.add(removed);
				}
			}
			for (Change change : sub.changes)
			{
				if (!ignored.contains(change.declineKey())
					&& plan.changes.stream().noneMatch(existing -> existing.getGoal() == change.getGoal()))
				{
					plan.changes.add(change);
				}
			}
		}
		return plan;
	}

	private static boolean isReplaced(Plan plan, Goal goal)
	{
		for (Addition addition : plan.additions)
		{
			if (addition.getReplaces() == goal)
			{
				return true;
			}
		}
		return false;
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
		Set<String> added, Plan plan, GoalTree tree, Set<String> foreign, Set<String> descended, Map<String, Goal> parts)
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
				// Bara mål som templaten skapade kan få nya delmål; ett inkopplat eget mål, eller
				// ett som också hör till en annan template, behåller sina egna delmål.
				// Samma mål flera gånger i templaten (t.ex. Heroes' Quest i två kedjor) fick sina delmål
				// från första förekomsten när det sattes in; bara den jämförs.
				if (child.getRef() == null && goal.getTemplateNodeKey() != null && !isShared(tree, goal, foreign)
					&& isChanged(goal, child)
					&& !ignored.contains(CHANGE_KEY_PREFIX + key)
					&& plan.changes.stream().noneMatch(change -> change.getGoal() == goal))
				{
					plan.changes.add(new Change(goal, child));
				}
				if (goal.getTemplateId() != null && child.getTemplateId() != null)
				{
					// En del-template med egen rot: dess plan räknas för sig nedan.
					parts.putIfAbsent(goal.getId(), goal);
				}
				else if (goal.getTemplateNodeKey() != null && !isShared(tree, goal, foreign)
					&& descended.add(goal.getId()))
				{
					findAdditions(child, goal, present, ignored, added, plan, tree, foreign, descended, parts);
				}
				continue;
			}
			// Samma nyckel flera gånger i templaten blir ett tillägg; insättningen kopplar resten.
			if (added.add(key))
			{
				plan.additions.add(new Addition(child, parentGoal.getId(), replacedGoal(child, present)));
			}
		}
	}

	/**
	 * Om templaten nu räknar ett befintligt mål på ett annat sätt utan att nyckeln ändrats: ett
	 * item-mål med andra motsvarigheter (countsAlso), eller andra doneWhen-nycklar, t.ex. när
	 * Graceful fick räkna ägda delar mot märkena eller Elite void-poängen blev klara med delarna.
	 * Andra skillnader i kravet ändrar nyckeln och blir tillägg och borttagningar i stället.
	 */
	static boolean isChanged(Goal goal, TemplateNode node)
	{
		if (goal.getRequirement() instanceof com.gielinorgoals.model.ItemRequirement
			&& node.getRequirement() instanceof com.gielinorgoals.model.ItemRequirement)
		{
			com.gielinorgoals.model.ItemRequirement have = (com.gielinorgoals.model.ItemRequirement) goal.getRequirement();
			com.gielinorgoals.model.ItemRequirement want = (com.gielinorgoals.model.ItemRequirement) node.getRequirement();
			if (have.isSameGoal(want) && !have.getCountsAlso().equals(want.getCountsAlso()))
			{
				return true;
			}
		}
		Set<String> haveKeys = goal.getDoneWhen() == null ? Set.of() : new HashSet<>(goal.getDoneWhen());
		Set<String> wantKeys = node.getDoneWhen() == null ? Set.of() : new HashSet<>(node.getDoneWhen());
		return !haveKeys.equals(wantKeys) || Boolean.TRUE.equals(goal.getDoneWhenAll()) != node.isDoneWhenAll();
	}

	// Ett befintligt mål med en av nodens "replaces"-nycklar, som noden ersätter.
	@javax.annotation.Nullable
	private static Goal replacedGoal(TemplateNode node, Map<String, Goal> present)
	{
		if (node.getReplaces() == null)
		{
			return null;
		}
		for (String key : node.getReplaces())
		{
			Goal goal = present.get(key);
			if (goal != null)
			{
				return goal;
			}
		}
		return null;
	}

	private static void collectReplacedKeys(TemplateNode node, Set<String> out)
	{
		for (TemplateNode child : node.getChildren())
		{
			if (child.getReplaces() != null)
			{
				out.addAll(child.getReplaces());
			}
			collectReplacedKeys(child, out);
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

	private static void collect(GoalTree tree, Goal goal, List<Goal> out, Set<String> visited, Set<String> foreign)
	{
		for (Goal child : tree.getChildren(goal))
		{
			if (visited.add(child.getId()))
			{
				out.add(child);
				// Under en del-template (egen rot) eller ett delat mål går jämförelsen inte vidare.
				if (child.getTemplateId() == null && !isShared(tree, child, foreign))
				{
					collect(tree, child, out, visited, foreign);
				}
			}
		}
	}

	/**
	 * Template-rötter som inte hör ihop med {@code root}: varken roten själv, något ovanför den
	 * (t.ex. paketet en del sitter i) eller något under den (delarna i ett paket).
	 */
	private static Set<String> foreignRoots(GoalTree tree, Goal root)
	{
		Set<String> foreign = new HashSet<>();
		for (Goal goal : tree.flatten())
		{
			if (goal.getTemplateId() != null && !goal.getId().equals(root.getId())
				&& !tree.isAncestor(goal.getId(), root.getId()) && !tree.isAncestor(root.getId(), goal.getId()))
			{
				foreign.add(goal.getId());
			}
		}
		return foreign;
	}

	/**
	 * Om målet också ligger under en annan template (t.ex. ett quest från Quest cape som
	 * kopplades in i ett paket). Då är det den templatens mål, och dess delmål jämförs där.
	 */
	private static boolean isShared(GoalTree tree, Goal goal, Set<String> foreign)
	{
		if (foreign.contains(goal.getId()))
		{
			return true;
		}
		for (String other : foreign)
		{
			if (tree.isAncestor(other, goal.getId()))
			{
				return true;
			}
		}
		return false;
	}
}
