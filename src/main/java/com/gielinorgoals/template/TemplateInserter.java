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
import com.gielinorgoals.model.Requirement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * Sätter in ett template-förslag i målgrafen. Roten blir alltid ett nytt rotmål. För
 * varje delmål med krav letas ett befintligt mål med exakt samma krav; finns det
 * kopplas det in under template-noden i stället för att ett nytt skapas. Ett inkopplat
 * mål behåller sina egna delmål, och template-nodens delmål läggs inte under det.
 *
 * <p>Ändrar bara grafen. Sparning, utvärdering och ombyggnad sköts av den som anropar,
 * en gång för hela insättningen.
 */
public final class TemplateInserter
{
	@Getter
	public static final class Result
	{
		// Null bara efter en ihopslagning där allt redan fanns.
		@Nullable
		private Goal root;
		private final List<Goal> created = new ArrayList<>();
		private final List<Goal> linked = new ArrayList<>();
		// Målet för varje nod med localId, så att en ref-nod kopplar in samma mål.
		private final java.util.Map<String, Goal> byLocalId = new java.util.HashMap<>();

		private Result(@Nullable Goal root)
		{
			this.root = root;
		}

		public List<Goal> getCreated()
		{
			return Collections.unmodifiableList(created);
		}

		public List<Goal> getLinked()
		{
			return Collections.unmodifiableList(linked);
		}

		/**
		 * Hur många av de nya och inkopplade målen som är klara, efter utvärderingen.
		 */
		public int countCompleted()
		{
			int count = 0;
			for (Goal goal : created)
			{
				count += goal.isCompleted() ? 1 : 0;
			}
			for (Goal goal : linked)
			{
				count += goal.isCompleted() ? 1 : 0;
			}
			return count;
		}
	}

	private TemplateInserter()
	{
	}

	public static Result insert(GoalTree tree, TemplateNode template)
	{
		return insert(tree, template, null);
	}

	/**
	 * @param parentId målet förslagets rot läggs under, eller null för ett nytt rotmål
	 */
	public static Result insert(GoalTree tree, TemplateNode template, @Nullable String parentId)
	{
		// Befintliga mål med krav; nya mål läggs till efter hand, så att samma krav
		// två gånger i en template bara ger ett mål.
		List<Goal> withRequirement = new ArrayList<>();
		for (Goal goal : tree.flatten())
		{
			if (goal.getRequirement() != null)
			{
				withRequirement.add(goal);
			}
		}

		// Bara mål som fanns före insättningen räknas som inkopplade befintliga mål.
		java.util.Set<String> before = new java.util.HashSet<>();
		for (Goal goal : withRequirement)
		{
			before.add(goal.getId());
		}

		// En rot med eget krav (t.ex. Total level 1500) som redan finns som mål blir det
		// befintliga målet, i stället för en dubblett. Det befintliga målet får ingen template.
		Goal existingRoot = findExisting(withRequirement, template.getRequirement());
		if (existingRoot != null)
		{
			if (parentId != null)
			{
				tree.link(parentId, existingRoot.getId());
			}
			Result result = new Result(existingRoot);
			result.linked.add(existingRoot);
			insertChildren(tree, template, existingRoot, withRequirement, before, result);
			collapseCounters(result);
			return result;
		}

		Goal root = create(template);
		if (template.getTemplateId() != null)
		{
			// Så att templaten kan byggas om och jämföras med målen senare.
			root.setTemplateId(template.getTemplateId());
			root.setTemplateParams(template.getTemplateParams() == null ? null
				: new java.util.LinkedHashMap<>(template.getTemplateParams()));
			root.setTemplateChoices(template.getTemplateChoices() == null || template.getTemplateChoices().isEmpty()
				? null : new java.util.LinkedHashMap<>(template.getTemplateChoices()));
			List<String> ignored = new ArrayList<>();
			collectExcludedKeys(template, ignored);
			root.setTemplateIgnoredKeys(ignored.isEmpty() ? null : ignored);
		}
		tree.add(parentId, root);
		Result result = new Result(root);
		result.created.add(root);
		if (root.getRequirement() != null)
		{
			withRequirement.add(root);
		}
		insertChildren(tree, template, root, withRequirement, before, result);

		collapseCounters(result);
		return result;
	}

	/**
	 * Sätter in noder under ett befintligt mål, med samma dubblettregel som en hel template.
	 * Används vid "Update from template". Resultatets rot är föräldern, som inte räknas som skapad.
	 */
	public static Result insertUnder(GoalTree tree, Goal parent, List<TemplateNode> nodes)
	{
		List<Goal> withRequirement = new ArrayList<>();
		java.util.Set<String> before = new java.util.HashSet<>();
		for (Goal goal : tree.flatten())
		{
			if (goal.getRequirement() != null)
			{
				withRequirement.add(goal);
				before.add(goal.getId());
			}
		}

		TemplateNode container = new TemplateNode("", null, false);
		nodes.forEach(container::add);
		Result result = new Result(parent);
		insertChildren(tree, container, parent, withRequirement, before, result);
		collapseCounters(result);
		return result;
	}

	/**
	 * Slår ihop importerade mål med profilens: delmålen till {@code container} blir rotmål.
	 * Ett mål som redan finns på samma plats (samma nyckel bland rotmålen eller förälderns
	 * delmål) återanvänds och får de delmål som saknas, och ett mål med samma krav någon
	 * annanstans kopplas in. Inget befintligt mål ersätts eller tas bort, så samma import
	 * två gånger ger inga dubbletter.
	 */
	public static Result merge(GoalTree tree, TemplateNode container)
	{
		List<Goal> withRequirement = new ArrayList<>();
		java.util.Set<String> before = new java.util.HashSet<>();
		for (Goal goal : tree.flatten())
		{
			before.add(goal.getId());
			if (goal.getRequirement() != null)
			{
				withRequirement.add(goal);
			}
		}
		Result result = new Result(null);
		mergeChildren(tree, container, null, true, withRequirement, before, result);
		collapseCounters(result);
		return result;
	}

	private static void mergeChildren(GoalTree tree, TemplateNode node, @Nullable Goal parent, boolean atRoot,
		List<Goal> withRequirement, java.util.Set<String> before, Result result)
	{
		for (TemplateNode child : node.getChildren())
		{
			if (!child.isIncluded())
			{
				continue;
			}
			if (child.getRef() != null)
			{
				Goal shared = result.byLocalId.get(child.getRef());
				if (shared != null && parent != null)
				{
					tree.link(parent.getId(), shared.getId());
				}
				continue;
			}

			Goal match = mergeMatch(tree, parent, atRoot, child, withRequirement);
			if (match != null)
			{
				if (child.getLocalId() != null)
				{
					result.byLocalId.put(child.getLocalId(), match);
				}
				if (parent != null)
				{
					tree.link(parent.getId(), match.getId());
				}
				if (before.contains(match.getId()) && !result.linked.contains(match))
				{
					result.linked.add(match);
				}
				mergeChildren(tree, child, match, false, withRequirement, before, result);
				continue;
			}

			Goal goal = create(child);
			tree.add(parent == null ? null : parent.getId(), goal);
			result.created.add(goal);
			if (result.root == null)
			{
				result.root = goal;
			}
			if (goal.getRequirement() != null)
			{
				withRequirement.add(goal);
			}
			if (child.getLocalId() != null)
			{
				result.byLocalId.put(child.getLocalId(), goal);
			}
			mergeChildren(tree, child, goal, false, withRequirement, before, result);
		}
	}

	/**
	 * Målet som en importerad nod slås ihop med: ett med samma nyckel på samma plats, annars
	 * ett med samma krav var som helst, annars null.
	 *
	 * @param atRoot true om noden blir ett rotmål; annars jämförs med parents delmål
	 */
	@Nullable
	static Goal mergeMatch(GoalTree tree, @Nullable Goal parent, boolean atRoot, TemplateNode node,
		List<Goal> withRequirement)
	{
		String key = node.getKey();
		List<Goal> siblings = atRoot ? tree.getRoots() : parent != null ? tree.getChildren(parent)
			: Collections.emptyList();
		for (Goal candidate : siblings)
		{
			if (key.equals(mergeKey(candidate)))
			{
				return candidate;
			}
		}
		return findExisting(withRequirement, node.getRequirement());
	}

	// Målets nyckel, och för egna mål utan nyckel samma group-nyckel som en nod med samma titel.
	private static String mergeKey(Goal goal)
	{
		String key = TemplateKeys.of(goal);
		return key != null ? key : "group:" + goal.getTitle().trim().toLowerCase(java.util.Locale.ROOT);
	}

	// tree.add fäller ut föräldern; räknarmål ska vara ihopfällda som standard.
	private static void collapseCounters(Result result)
	{
		for (Goal goal : result.created)
		{
			if (goal.isAggregate())
			{
				goal.setExpanded(false);
			}
		}
	}

	// Nycklar för noder användaren bockat ur; deras delmål räknas som bortvalda via föräldern.
	private static void collectExcludedKeys(TemplateNode node, List<String> out)
	{
		for (TemplateNode child : node.getChildren())
		{
			if (!child.isIncluded())
			{
				out.add(child.getKey());
			}
			else
			{
				collectExcludedKeys(child, out);
			}
		}
	}

	private static void insertChildren(GoalTree tree, TemplateNode node, Goal parent, List<Goal> withRequirement,
		java.util.Set<String> before, Result result)
	{
		for (TemplateNode child : node.getChildren())
		{
			if (!child.isIncluded())
			{
				continue;
			}

			if (child.getRef() != null)
			{
				// Ett delat delmål: samma mål under en till förälder. Var målet bortvalt finns
				// det inget att koppla in.
				Goal shared = result.byLocalId.get(child.getRef());
				if (shared != null)
				{
					tree.link(parent.getId(), shared.getId());
				}
				continue;
			}

			Goal existing = findExisting(withRequirement, child.getRequirement());
			if (existing != null)
			{
				if (child.getLocalId() != null)
				{
					result.byLocalId.put(child.getLocalId(), existing);
				}
				// Kopplingen kan redan finnas om templaten har samma krav två gånger.
				// Ett mål som skapades tidigare i samma insättning kopplas in men räknas inte
				// som ett befintligt mål.
				if (tree.link(parent.getId(), existing.getId()) && before.contains(existing.getId()))
				{
					result.linked.add(existing);
				}
				continue;
			}

			Goal goal = create(child);
			tree.add(parent.getId(), goal);
			result.created.add(goal);
			if (child.getLocalId() != null)
			{
				result.byLocalId.put(child.getLocalId(), goal);
			}
			if (goal.getRequirement() != null)
			{
				withRequirement.add(goal);
			}
			insertChildren(tree, child, goal, withRequirement, before, result);
		}
	}

	/**
	 * Ett befintligt mål med exakt samma krav, eller null.
	 */
	@Nullable
	public static Goal findExisting(List<Goal> withRequirement, @Nullable Requirement requirement)
	{
		if (requirement == null)
		{
			return null;
		}
		for (Goal goal : withRequirement)
		{
			if (requirement.isSameGoal(goal.getRequirement()))
			{
				return goal;
			}
		}
		return null;
	}

	private static Goal create(TemplateNode node)
	{
		Goal goal = new Goal(node.getTitle());
		goal.setRequirement(node.getRequirement());
		goal.setAggregate(node.isAggregate());
		goal.setTemplateNodeKey(node.getKey());
		goal.setWikiPage(node.getWikiPage());
		goal.setNote(node.getNote());
		goal.setDropSource(node.getDropSource());
		goal.setIcon(node.getIcon());
		goal.setDoneWhen(node.getDoneWhen() == null ? null : new ArrayList<>(node.getDoneWhen()));
		return goal;
	}
}
