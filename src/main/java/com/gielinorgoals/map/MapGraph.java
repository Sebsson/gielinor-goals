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
package com.gielinorgoals.map;

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
import lombok.Getter;

/**
 * Den del av målgrafen som syns på kartan i en viss vy. Varje mål finns en gång, även om
 * det ligger under flera föräldrar; de blir flera inkommande kanter i stället.
 *
 * <ul>
 * <li>Från omfångets rötter följs delmål bara under utfällda mål.</li>
 * <li>Med "Dölj klara" döljs ett klart mål som saknar synliga delmål. Omfångets rötter
 * visas alltid, så att kartan aldrig blir tom.</li>
 * <li>En kant ritas från ett utfällt mål till varje synligt delmål.</li>
 * </ul>
 * Ordningen är upptäcktsordningen i en djupet-först-genomgång med användarens egen
 * ordning på delmålen, så att samma mål och vy alltid ger samma graf.
 */
public final class MapGraph
{
	@Getter
	private final List<Goal> nodes;
	@Getter
	private final List<String> scopeRootIds;
	private final Map<String, List<String>> children;
	private final Map<String, List<String>> parents;
	// Ihopfällda mål som har delmål, för ▸ på kartan.
	private final Set<String> collapsed;

	private MapGraph(List<Goal> nodes, List<String> scopeRootIds, Map<String, List<String>> children,
		Map<String, List<String>> parents, Set<String> collapsed)
	{
		this.nodes = Collections.unmodifiableList(nodes);
		this.scopeRootIds = Collections.unmodifiableList(scopeRootIds);
		this.children = children;
		this.parents = parents;
		this.collapsed = collapsed;
	}

	public static MapGraph build(GoalTree tree, MapView view)
	{
		List<Goal> roots = new ArrayList<>();
		Goal chosen = view.getRootId() == null ? null : tree.find(view.getRootId());
		if (chosen != null)
		{
			roots.add(chosen);
		}
		else
		{
			// Ett borttaget rotmål faller tillbaka på "Alla mål".
			roots.addAll(tree.getRoots());
		}
		Set<String> rootIds = new HashSet<>();
		roots.forEach(root -> rootIds.add(root.getId()));

		// 1. Allt som nås genom utfällda mål, i upptäcktsordning.
		Map<String, Goal> reached = new LinkedHashMap<>();
		Set<String> expanded = new HashSet<>();
		for (Goal root : roots)
		{
			reach(tree, view, rootIds, root, reached, expanded);
		}

		// 2. "Dölj klara": barn före föräldrar, så att ett klart mål med synliga barn syns.
		Map<String, Boolean> visible = new HashMap<>();
		for (Goal goal : reached.values())
		{
			isVisible(tree, view, rootIds, expanded, goal, visible);
		}

		// 3. Noder och kanter bland de synliga.
		List<Goal> nodes = new ArrayList<>();
		Map<String, List<String>> children = new HashMap<>();
		Map<String, List<String>> parents = new HashMap<>();
		Set<String> collapsed = new HashSet<>();
		for (Goal goal : reached.values())
		{
			if (!visible.get(goal.getId()))
			{
				continue;
			}
			nodes.add(goal);
			children.put(goal.getId(), new ArrayList<>());
			parents.putIfAbsent(goal.getId(), new ArrayList<>());
			if (!expanded.contains(goal.getId()) && goal.hasChildren())
			{
				collapsed.add(goal.getId());
			}
		}
		for (Goal goal : nodes)
		{
			if (!expanded.contains(goal.getId()))
			{
				continue;
			}
			for (Goal child : tree.getChildren(goal))
			{
				if (Boolean.TRUE.equals(visible.get(child.getId())))
				{
					children.get(goal.getId()).add(child.getId());
					parents.get(child.getId()).add(goal.getId());
				}
			}
		}

		List<String> scopeRootIds = new ArrayList<>();
		roots.forEach(root -> scopeRootIds.add(root.getId()));
		return new MapGraph(nodes, scopeRootIds, children, parents, collapsed);
	}

	private static void reach(GoalTree tree, MapView view, Set<String> rootIds, Goal goal,
		Map<String, Goal> reached, Set<String> expanded)
	{
		if (reached.containsKey(goal.getId()))
		{
			return;
		}
		reached.put(goal.getId(), goal);
		if (!view.isExpanded(goal, rootIds.contains(goal.getId())))
		{
			return;
		}
		expanded.add(goal.getId());
		for (Goal child : tree.getChildren(goal))
		{
			reach(tree, view, rootIds, child, reached, expanded);
		}
	}

	private static boolean isVisible(GoalTree tree, MapView view, Set<String> rootIds, Set<String> expanded,
		Goal goal, Map<String, Boolean> memo)
	{
		Boolean known = memo.get(goal.getId());
		if (known != null)
		{
			return known;
		}
		boolean visible = rootIds.contains(goal.getId()) || !view.isHideCompleted() || !goal.isCompleted();
		if (expanded.contains(goal.getId()))
		{
			for (Goal child : tree.getChildren(goal))
			{
				// Alla barn utvärderas, så att varje synligt barn finns i memo till steg 3.
				visible |= isVisible(tree, view, rootIds, expanded, child, memo);
			}
		}
		memo.put(goal.getId(), visible);
		return visible;
	}

	public List<String> childrenOf(String id)
	{
		return children.getOrDefault(id, List.of());
	}

	public List<String> parentsOf(String id)
	{
		return parents.getOrDefault(id, List.of());
	}

	public boolean isCollapsed(String id)
	{
		return collapsed.contains(id);
	}

	public boolean contains(String id)
	{
		return children.containsKey(id);
	}

	public int size()
	{
		return nodes.size();
	}
}
