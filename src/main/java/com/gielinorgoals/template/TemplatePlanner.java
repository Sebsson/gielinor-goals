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

import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalProgress;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.Requirement;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * Torrkörning av {@link TemplateInserter} för förhandsvisningen: vad varje nod blir
 * (nytt mål, inkopplat befintligt mål eller dubblett inom templaten) och om den redan
 * är klar. Samma regler som insättningen, utan att ändra grafen.
 */
public final class TemplatePlanner
{
	public enum Status
	{
		// Ett nytt mål skapas.
		NEW,
		// Ett befintligt mål med samma krav kopplas in.
		LINKED,
		// Samma krav finns redan tidigare i templaten; det målet kopplas in här också.
		DUPLICATE,
		// Urbockad eller under en urbockad eller inkopplad nod; läggs inte till.
		SKIPPED
	}

	@Getter
	public static final class Plan
	{
		private final Map<TemplateNode, Status> statuses = new IdentityHashMap<>();
		private final Map<TemplateNode, Boolean> done = new IdentityHashMap<>();
		// Det befintliga målet för inkopplade noder.
		private final Map<TemplateNode, Goal> existing = new IdentityHashMap<>();
		private int newGoals;
		private int linkedGoals;
		private int alreadyDone;

		public Status statusOf(TemplateNode node)
		{
			return statuses.getOrDefault(node, Status.SKIPPED);
		}

		public boolean isDone(TemplateNode node)
		{
			return done.getOrDefault(node, false);
		}

		/**
		 * T.ex. "Adds 1 root goal, 165 new sub-goals, links 5 existing, 142 already done".
		 */
		public String summary()
		{
			return "Adds 1 root goal, " + newGoals + (newGoals == 1 ? " new sub-goal" : " new sub-goals")
				+ ", links " + linkedGoals + " existing, " + alreadyDone + " already done";
		}
	}

	private TemplatePlanner()
	{
	}

	/**
	 * @param data spelläget för att se vad som redan är klart; {@link GameData#EMPTY} om okänt
	 */
	public static Plan plan(GoalTree tree, TemplateNode root, GameData data)
	{
		List<Goal> existingGoals = new ArrayList<>();
		for (Goal goal : tree.flatten())
		{
			if (goal.getRequirement() != null)
			{
				existingGoals.add(goal);
			}
		}

		Plan plan = new Plan();
		List<TemplateNode> planned = new ArrayList<>();
		// Samma regel som insättningen: en rot med ett krav som redan finns kopplas in.
		Goal existingRoot = TemplateInserter.findExisting(existingGoals, root.getRequirement());
		if (existingRoot != null)
		{
			plan.statuses.put(root, Status.LINKED);
			plan.existing.put(root, existingRoot);
			plan.linkedGoals++;
			planChildren(root, existingGoals, planned, data, plan);
			boolean done = existingRoot.isCompleted() || met(root.getRequirement(), data);
			plan.done.put(root, done);
			plan.alreadyDone += done ? 1 : 0;
			return plan;
		}
		plan.statuses.put(root, Status.NEW);
		if (root.getRequirement() != null)
		{
			planned.add(root);
		}
		planChildren(root, existingGoals, planned, data, plan);
		plan.done.put(root, isDone(root, root.getRequirement(), data, plan));
		return plan;
	}

	private static void planChildren(TemplateNode node, List<Goal> existingGoals, List<TemplateNode> planned,
		GameData data, Plan plan)
	{
		for (TemplateNode child : node.getChildren())
		{
			if (!child.isIncluded())
			{
				continue;
			}

			Goal existing = TemplateInserter.findExisting(existingGoals, child.getRequirement());
			if (existing != null)
			{
				plan.statuses.put(child, Status.LINKED);
				plan.existing.put(child, existing);
				boolean done = existing.isCompleted() || met(child.getRequirement(), data);
				plan.done.put(child, done);
				plan.linkedGoals++;
				plan.alreadyDone += done ? 1 : 0;
				continue;
			}
			if (findPlanned(planned, child.getRequirement()) != null)
			{
				plan.statuses.put(child, Status.DUPLICATE);
				plan.done.put(child, met(child.getRequirement(), data));
				continue;
			}

			plan.statuses.put(child, Status.NEW);
			plan.newGoals++;
			if (child.getRequirement() != null)
			{
				planned.add(child);
			}
			planChildren(child, existingGoals, planned, data, plan);
			boolean done = isDone(child, child.getRequirement(), data, plan);
			plan.done.put(child, done);
			plan.alreadyDone += done ? 1 : 0;
		}
	}

	// Ett nytt mål är klart om kravet är uppfyllt, eller för ett räknarmål utan krav
	// om alla dess medtagna delmål är klara (samma regel som utvärderingen).
	private static boolean isDone(TemplateNode node, @Nullable Requirement requirement, GameData data, Plan plan)
	{
		if (requirement != null)
		{
			return met(requirement, data);
		}
		if (!node.isAggregate())
		{
			return false;
		}
		int counted = 0;
		for (TemplateNode child : node.getChildren())
		{
			if (plan.statusOf(child) == Status.SKIPPED)
			{
				continue;
			}
			counted++;
			if (!plan.isDone(child))
			{
				return false;
			}
		}
		return counted > 0;
	}

	private static boolean met(@Nullable Requirement requirement, GameData data)
	{
		if (requirement == null)
		{
			return false;
		}
		GoalProgress progress = requirement.progress(data);
		return progress != null && progress.isComplete();
	}

	@Nullable
	private static TemplateNode findPlanned(List<TemplateNode> planned, @Nullable Requirement requirement)
	{
		if (requirement == null)
		{
			return null;
		}
		for (TemplateNode node : planned)
		{
			if (requirement.isSameGoal(node.getRequirement()))
			{
				return node;
			}
		}
		return null;
	}
}
