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
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Trädet av mål för en profil. Alla operationer på strukturen samlas här så att
 * UI:t aldrig ändrar listorna direkt. Klassen är inte trådsäker och används bara på EDT.
 */
public class GoalTree
{
	private final List<Goal> roots;

	public GoalTree()
	{
		this(new ArrayList<>());
	}

	public GoalTree(List<Goal> roots)
	{
		this.roots = roots;
	}

	public List<Goal> getRoots()
	{
		return Collections.unmodifiableList(roots);
	}

	public boolean isEmpty()
	{
		return roots.isEmpty();
	}

	@Nullable
	public Goal find(String id)
	{
		Location location = locate(id);
		return location == null ? null : location.goal;
	}

	/**
	 * Lägger till ett mål sist, antingen på rotnivå eller som delmål.
	 *
	 * @return false om föräldern inte finns
	 */
	public boolean add(@Nullable String parentId, Goal goal)
	{
		if (parentId == null)
		{
			roots.add(goal);
			return true;
		}

		Goal parent = find(parentId);
		if (parent == null)
		{
			return false;
		}

		parent.getChildren().add(goal);
		parent.setExpanded(true);
		return true;
	}

	public boolean remove(String id)
	{
		Location location = locate(id);
		if (location == null)
		{
			return false;
		}

		location.siblings.remove(location.index);
		return true;
	}

	public boolean moveUp(String id)
	{
		Location location = locate(id);
		if (location == null || location.index == 0)
		{
			return false;
		}

		Collections.swap(location.siblings, location.index, location.index - 1);
		return true;
	}

	public boolean moveDown(String id)
	{
		Location location = locate(id);
		if (location == null || location.index >= location.siblings.size() - 1)
		{
			return false;
		}

		Collections.swap(location.siblings, location.index, location.index + 1);
		return true;
	}

	/**
	 * Gör målet till sista delmålet under syskonet ovanför (som Tab i en punktlista).
	 */
	public boolean indent(String id)
	{
		Location location = locate(id);
		if (location == null || location.index == 0)
		{
			return false;
		}

		Goal newParent = location.siblings.get(location.index - 1);
		location.siblings.remove(location.index);
		newParent.getChildren().add(location.goal);
		newParent.setExpanded(true);
		return true;
	}

	/**
	 * Flyttar ut målet en nivå, direkt efter sin nuvarande förälder (som Shift+Tab).
	 */
	public boolean outdent(String id)
	{
		Location location = locate(id);
		if (location == null || location.parent == null)
		{
			return false;
		}

		Location parentLocation = locate(location.parent.getId());
		if (parentLocation == null)
		{
			return false;
		}

		location.siblings.remove(location.index);
		parentLocation.siblings.add(parentLocation.index + 1, location.goal);
		return true;
	}

	public boolean canIndent(String id)
	{
		Location location = locate(id);
		return location != null && location.index > 0;
	}

	public boolean canOutdent(String id)
	{
		Location location = locate(id);
		return location != null && location.parent != null;
	}

	public boolean canMoveUp(String id)
	{
		Location location = locate(id);
		return location != null && location.index > 0;
	}

	public boolean canMoveDown(String id)
	{
		Location location = locate(id);
		return location != null && location.index < location.siblings.size() - 1;
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
	 * Besöker alla mål i visningsordning (förälder före barn).
	 */
	public void forEach(Consumer<Goal> visitor)
	{
		visit(roots, visitor);
	}

	/**
	 * Alla mål som har målet som direkt delmål. I dagens träd är det högst ett, men
	 * anropare ska inte förutsätta det: delmål ska längre fram kunna ha flera föräldrar.
	 */
	public List<Goal> getParents(String id)
	{
		List<Goal> parents = new ArrayList<>();
		forEach(goal ->
		{
			for (Goal child : goal.getChildren())
			{
				if (child.getId().equals(id))
				{
					parents.add(goal);
					return;
				}
			}
		});
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
		return ancestor != null && containsBelow(ancestor.getChildren(), goalId);
	}

	private static boolean containsBelow(List<Goal> goals, String id)
	{
		for (Goal goal : goals)
		{
			if (goal.getId().equals(id) || containsBelow(goal.getChildren(), id))
			{
				return true;
			}
		}
		return false;
	}

	public int size()
	{
		return flatten().size();
	}

	/**
	 * Alla mål i visningsordning (förälder före barn).
	 */
	public List<Goal> flatten()
	{
		List<Goal> all = new ArrayList<>();
		forEach(all::add);
		return all;
	}

	private static void visit(List<Goal> goals, Consumer<Goal> visitor)
	{
		for (Goal goal : goals)
		{
			visitor.accept(goal);
			visit(goal.getChildren(), visitor);
		}
	}

	@Nullable
	private Location locate(String id)
	{
		return locate(roots, null, id);
	}

	@Nullable
	private static Location locate(List<Goal> siblings, @Nullable Goal parent, String id)
	{
		for (int i = 0; i < siblings.size(); i++)
		{
			Goal goal = siblings.get(i);
			if (goal.getId().equals(id))
			{
				return new Location(goal, parent, siblings, i);
			}

			Location found = locate(goal.getChildren(), goal, id);
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}

	// Var i trädet ett mål ligger: listan det ligger i, dess index och föräldern.
	private static final class Location
	{
		private final Goal goal;
		@Nullable
		private final Goal parent;
		private final List<Goal> siblings;
		private final int index;

		private Location(Goal goal, @Nullable Goal parent, List<Goal> siblings, int index)
		{
			this.goal = goal;
			this.parent = parent;
			this.siblings = siblings;
			this.index = index;
		}
	}
}
