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
package com.gielinorgoals.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Målen för en profil som en riktad acyklisk graf: varje mål lagras en gång (id → mål)
 * och har en ordnad lista med delmålens id:n, så ett delmål kan ha flera föräldrar.
 * Rötterna är en egen ordnad lista. Namnet är kvar från när det var ett träd.
 *
 * <p>Operationer som flyttar ett mål (upp, ner, indrag, utdrag) gäller bara kopplingen
 * i en viss förälder, den som raden visas under. {@code parentId == null} betyder
 * rotlistan. Klassen är inte trådsäker och används bara på EDT.
 */
public class GoalTree
{
	private final Map<String, Goal> goals = new LinkedHashMap<>();
	private final List<String> rootIds = new ArrayList<>();

	public GoalTree()
	{
	}

	/**
	 * Bygger grafen från sparad data och lagar det som inte håller ihop: dubbla id:n,
	 * kopplingar till mål som saknas, cykler, och mål som inte nås från någon rot
	 * (de blir rötter, så att inget försvinner tyst).
	 */
	public static GoalTree load(List<String> rootIds, Collection<Goal> goals)
	{
		GoalTree tree = new GoalTree();
		for (Goal goal : goals)
		{
			if (goal != null && goal.getId() != null && !tree.goals.containsKey(goal.getId()))
			{
				if (goal.getChildIds() == null)
				{
					goal.setChildIds(new ArrayList<>());
				}
				tree.goals.put(goal.getId(), goal);
			}
		}

		for (Goal goal : tree.goals.values())
		{
			// Ta bort saknade och dubbla kopplingar, men behåll ordningen.
			Set<String> seen = new HashSet<>();
			goal.getChildIds().removeIf(id -> id == null || !tree.goals.containsKey(id)
				|| id.equals(goal.getId()) || !seen.add(id));
		}
		tree.breakCycles();

		Set<String> seenRoots = new HashSet<>();
		for (String id : rootIds == null ? Collections.<String>emptyList() : rootIds)
		{
			if (id != null && tree.goals.containsKey(id) && seenRoots.add(id))
			{
				tree.rootIds.add(id);
			}
		}

		// Ett mål med förälder ska inte också vara rot; ett mål utan förälder måste vara rot.
		Set<String> hasParent = new HashSet<>();
		for (Goal goal : tree.goals.values())
		{
			hasParent.addAll(goal.getChildIds());
		}
		tree.rootIds.removeIf(hasParent::contains);
		for (String id : tree.goals.keySet())
		{
			if (!hasParent.contains(id) && !tree.rootIds.contains(id))
			{
				tree.rootIds.add(id);
			}
		}
		return tree;
	}

	// Tar bort kopplingar som skulle ge en cykel (bara möjligt i trasig sparad data).
	private void breakCycles()
	{
		Set<String> done = new HashSet<>();
		for (String id : new ArrayList<>(goals.keySet()))
		{
			breakCycles(id, new HashSet<>(), done);
		}
	}

	private void breakCycles(String id, Set<String> path, Set<String> done)
	{
		if (done.contains(id))
		{
			return;
		}
		path.add(id);
		Goal goal = goals.get(id);
		goal.getChildIds().removeIf(child -> path.contains(child));
		for (String child : new ArrayList<>(goal.getChildIds()))
		{
			breakCycles(child, path, done);
		}
		path.remove(id);
		done.add(id);
	}

	public List<Goal> getRoots()
	{
		return resolve(rootIds);
	}

	public List<String> getRootIds()
	{
		return Collections.unmodifiableList(rootIds);
	}

	/**
	 * Målets direkta delmål i användarens ordning.
	 */
	public List<Goal> getChildren(Goal goal)
	{
		return resolve(goal.getChildIds());
	}

	public boolean isEmpty()
	{
		return goals.isEmpty();
	}

	@Nullable
	public Goal find(String id)
	{
		return goals.get(id);
	}

	/**
	 * Lägger till ett nytt mål sist, antingen på rotnivå eller som delmål.
	 *
	 * @return false om föräldern inte finns eller målets id redan används
	 */
	public boolean add(@Nullable String parentId, Goal goal)
	{
		if (goals.containsKey(goal.getId()))
		{
			return false;
		}
		if (parentId == null)
		{
			goals.put(goal.getId(), goal);
			rootIds.add(goal.getId());
			return true;
		}

		Goal parent = find(parentId);
		if (parent == null)
		{
			return false;
		}
		goals.put(goal.getId(), goal);
		parent.getChildIds().add(goal.getId());
		parent.setExpanded(true);
		return true;
	}

	/**
	 * @return om {@code childId} kan kopplas under {@code parentId} utan att skapa en cykel
	 */
	public boolean canLink(String parentId, String childId)
	{
		Goal parent = find(parentId);
		return parent != null && find(childId) != null && !parentId.equals(childId)
			&& !parent.getChildIds().contains(childId) && !isAncestor(childId, parentId);
	}

	/**
	 * Kopplar ett befintligt mål under ytterligare en förälder. Ett rotmål slutar vara rot.
	 *
	 * @return false om kopplingen redan finns, något mål saknas, eller det skulle bli en cykel
	 */
	public boolean link(String parentId, String childId)
	{
		if (!canLink(parentId, childId))
		{
			return false;
		}
		find(parentId).getChildIds().add(childId);
		rootIds.remove(childId);
		return true;
	}

	/**
	 * Tar bort kopplingen mellan förälder och delmål. Saknar målet då föräldrar blir det
	 * ett rotmål, så det försvinner aldrig.
	 */
	public boolean unlink(String parentId, String childId)
	{
		Goal parent = find(parentId);
		if (parent == null || !parent.getChildIds().remove(childId))
		{
			return false;
		}
		if (getParents(childId).isEmpty() && !rootIds.contains(childId))
		{
			rootIds.add(childId);
		}
		return true;
	}

	/**
	 * Tar bort målet helt, från alla föräldrar. Delmål som bara hade det här målet som
	 * förälder tas också bort; delmål med andra föräldrar ligger kvar.
	 */
	public boolean remove(String id)
	{
		Goal goal = goals.remove(id);
		if (goal == null)
		{
			return false;
		}
		rootIds.remove(id);
		for (Goal other : goals.values())
		{
			other.getChildIds().remove(id);
		}

		for (String childId : goal.getChildIds())
		{
			if (goals.containsKey(childId) && !rootIds.contains(childId) && getParents(childId).isEmpty())
			{
				remove(childId);
			}
		}
		return true;
	}

	public boolean moveUp(@Nullable String parentId, String id)
	{
		List<String> siblings = siblings(parentId);
		int index = siblings == null ? -1 : siblings.indexOf(id);
		if (index <= 0)
		{
			return false;
		}
		Collections.swap(siblings, index, index - 1);
		return true;
	}

	public boolean moveDown(@Nullable String parentId, String id)
	{
		List<String> siblings = siblings(parentId);
		int index = siblings == null ? -1 : siblings.indexOf(id);
		if (index < 0 || index >= siblings.size() - 1)
		{
			return false;
		}
		Collections.swap(siblings, index, index + 1);
		return true;
	}

	/**
	 * Gör målet till sista delmålet under syskonet ovanför (som Tab i en punktlista).
	 * Bara kopplingen i {@code parentId} flyttas; andra föräldrar påverkas inte.
	 */
	public boolean indent(@Nullable String parentId, String id)
	{
		if (!canIndent(parentId, id))
		{
			return false;
		}
		List<String> siblings = siblings(parentId);
		int index = siblings.indexOf(id);
		Goal newParent = find(siblings.get(index - 1));
		siblings.remove(index);
		if (!newParent.getChildIds().contains(id))
		{
			newParent.getChildIds().add(id);
		}
		newParent.setExpanded(true);
		return true;
	}

	/**
	 * Flyttar ut målet en nivå, direkt efter föräldern i listan föräldern visas i (som
	 * Shift+Tab). {@code grandparentId} är förälderns förälder i den raden, null för rotnivå.
	 */
	public boolean outdent(@Nullable String grandparentId, String parentId, String id)
	{
		if (!canOutdent(grandparentId, parentId, id))
		{
			return false;
		}
		List<String> parentSiblings = siblings(grandparentId);
		find(parentId).getChildIds().remove(id);
		if (!parentSiblings.contains(id))
		{
			parentSiblings.add(parentSiblings.indexOf(parentId) + 1, id);
		}
		return true;
	}

	public boolean canIndent(@Nullable String parentId, String id)
	{
		List<String> siblings = siblings(parentId);
		int index = siblings == null ? -1 : siblings.indexOf(id);
		// Målet får inte hamna under ett av sina egna delmål.
		return index > 0 && !isAncestor(id, siblings.get(index - 1));
	}

	public boolean canOutdent(@Nullable String grandparentId, @Nullable String parentId, String id)
	{
		if (parentId == null)
		{
			return false;
		}
		Goal parent = find(parentId);
		List<String> parentSiblings = siblings(grandparentId);
		return parent != null && parent.getChildIds().contains(id)
			&& parentSiblings != null && parentSiblings.contains(parentId);
	}

	public boolean canMoveUp(@Nullable String parentId, String id)
	{
		List<String> siblings = siblings(parentId);
		return siblings != null && siblings.indexOf(id) > 0;
	}

	public boolean canMoveDown(@Nullable String parentId, String id)
	{
		List<String> siblings = siblings(parentId);
		int index = siblings == null ? -1 : siblings.indexOf(id);
		return index >= 0 && index < siblings.size() - 1;
	}

	public boolean setCompleted(String id, boolean completed, long now)
	{
		Goal goal = find(id);
		if (goal == null)
		{
			return false;
		}

		goal.markCompleted(completed, now);
		return true;
	}

	/**
	 * Alla mål som har målet som direkt delmål, i lagringsordning.
	 */
	public List<Goal> getParents(String id)
	{
		List<Goal> parents = new ArrayList<>();
		for (Goal goal : goals.values())
		{
			if (goal.getChildIds().contains(id))
			{
				parents.add(goal);
			}
		}
		return parents;
	}

	/**
	 * @return true om {@code goalId} ligger någonstans under {@code ancestorId}, på valfritt djup
	 */
	public boolean isAncestor(String ancestorId, String goalId)
	{
		if (ancestorId.equals(goalId))
		{
			return false;
		}
		Goal ancestor = find(ancestorId);
		return ancestor != null && reaches(ancestor, goalId, new HashSet<>());
	}

	private boolean reaches(Goal from, String targetId, Set<String> visited)
	{
		for (String childId : from.getChildIds())
		{
			if (childId.equals(targetId))
			{
				return true;
			}
			Goal child = goals.get(childId);
			if (child != null && visited.add(childId) && reaches(child, targetId, visited))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Besöker varje mål en gång i visningsordning (förälder före barn). Ett delat
	 * delmål besöks där det först dyker upp.
	 */
	public void forEach(Consumer<Goal> visitor)
	{
		Set<String> visited = new HashSet<>();
		for (String id : rootIds)
		{
			visit(id, visitor, visited);
		}
	}

	private void visit(String id, Consumer<Goal> visitor, Set<String> visited)
	{
		Goal goal = goals.get(id);
		if (goal == null || !visited.add(id))
		{
			return;
		}
		visitor.accept(goal);
		for (String childId : goal.getChildIds())
		{
			visit(childId, visitor, visited);
		}
	}

	public int size()
	{
		return goals.size();
	}

	/**
	 * Alla mål, varje mål en gång, i visningsordning (förälder före barn).
	 */
	public List<Goal> flatten()
	{
		List<Goal> all = new ArrayList<>();
		forEach(all::add);
		return all;
	}

	@Nullable
	private List<String> siblings(@Nullable String parentId)
	{
		if (parentId == null)
		{
			return rootIds;
		}
		Goal parent = find(parentId);
		return parent == null ? null : parent.getChildIds();
	}

	private List<Goal> resolve(List<String> ids)
	{
		List<Goal> resolved = new ArrayList<>(ids.size());
		for (String id : ids)
		{
			Goal goal = goals.get(id);
			if (goal != null)
			{
				resolved.add(goal);
			}
		}
		return Collections.unmodifiableList(resolved);
	}
}
