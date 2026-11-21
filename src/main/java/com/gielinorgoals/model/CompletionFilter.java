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
import java.util.List;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * Väljer vilka mål i en batch av nyss klarade mål som ska få en egen notis.
 * Klaras ett mål och dess delmål samtidigt visas bara det översta, med antalet
 * dolda delmål. All släktskap går via {@link GoalTree#isAncestor} och
 * {@link GoalTree#getParents}, så att det fungerar även när delmål får flera föräldrar.
 */
public final class CompletionFilter
{
	@Value
	public static class Entry
	{
		Goal goal;
		// Delmål (på valfritt djup) som klarades i samma batch och slogs ihop hit.
		int hiddenDescendants;
		// Rotmål: målet har ingen förälder.
		boolean root;
		// En förälder som inte är klar än, för progressraden; null om ingen finns.
		@Nullable
		Goal openParent;
		int parentDone;
		int parentTotal;
	}

	private CompletionFilter()
	{
	}

	/**
	 * @param completed mål som just klarades, i den ordning de upptäcktes
	 * @param rootsOnly visa bara rotmål (delmål utan klar förälder i batchen försvinner)
	 */
	public static List<Entry> filter(GoalTree tree, List<Goal> completed, boolean rootsOnly)
	{
		List<Entry> entries = new ArrayList<>();
		for (Goal goal : completed)
		{
			if (hasAncestorIn(tree, goal, completed))
			{
				continue;
			}

			List<Goal> parents = tree.getParents(goal.getId());
			boolean root = parents.isEmpty();
			if (rootsOnly && !root)
			{
				continue;
			}

			int hidden = 0;
			for (Goal other : completed)
			{
				if (tree.isAncestor(goal.getId(), other.getId()))
				{
					hidden++;
				}
			}

			Goal openParent = null;
			int done = 0;
			for (Goal parent : parents)
			{
				if (!parent.isCompleted())
				{
					openParent = parent;
					for (Goal sibling : parent.getChildren())
					{
						if (sibling.isCompleted())
						{
							done++;
						}
					}
					break;
				}
			}

			entries.add(new Entry(goal, hidden, root, openParent, done,
				openParent == null ? 0 : openParent.getChildren().size()));
		}
		return entries;
	}

	private static boolean hasAncestorIn(GoalTree tree, Goal goal, List<Goal> completed)
	{
		for (Goal other : completed)
		{
			if (tree.isAncestor(other.getId(), goal.getId()))
			{
				return true;
			}
		}
		return false;
	}
}
