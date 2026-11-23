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
		private final Goal root;
		private final List<Goal> created = new ArrayList<>();
		private final List<Goal> linked = new ArrayList<>();

		private Result(Goal root)
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

		Goal root = create(template);
		tree.add(null, root);
		Result result = new Result(root);
		result.created.add(root);
		if (root.getRequirement() != null)
		{
			withRequirement.add(root);
		}
		insertChildren(tree, template, root, withRequirement, result);

		// tree.add fäller ut föräldern; räknarmål ska vara ihopfällda som standard.
		for (Goal goal : result.created)
		{
			if (goal.isAggregate())
			{
				goal.setExpanded(false);
			}
		}
		return result;
	}

	private static void insertChildren(GoalTree tree, TemplateNode node, Goal parent, List<Goal> withRequirement,
		Result result)
	{
		for (TemplateNode child : node.getChildren())
		{
			if (!child.isIncluded())
			{
				continue;
			}

			Goal existing = findExisting(withRequirement, child.getRequirement());
			if (existing != null)
			{
				// Kopplingen kan redan finnas om templaten har samma krav två gånger.
				if (tree.link(parent.getId(), existing.getId()))
				{
					result.linked.add(existing);
				}
				continue;
			}

			Goal goal = create(child);
			tree.add(parent.getId(), goal);
			result.created.add(goal);
			if (goal.getRequirement() != null)
			{
				withRequirement.add(goal);
			}
			insertChildren(tree, child, goal, withRequirement, result);
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
		return goal;
	}
}
