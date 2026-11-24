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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * Räknar ut progress för alla automatiska mål och bockar av de som har nått målet.
 * Avbockningen är spärrad: ett klart mål avmarkeras aldrig automatiskt, så att
 * t.ex. ett föremål som används upp senare inte gör målet ogjort igen.
 *
 * <p>Aggregerade mål ("X av Y") får progress av hur många direkta delmål som är klara.
 * Ett aggregat utan eget krav bockas av när alla direkta delmål är klara. Aggregaten
 * räknas efter kraven och barn före föräldrar, så att nästlade aggregat klaras i samma
 * utvärdering.
 *
 * <p>Ett mål med {@link Goal#getDoneWhen()} bockas av när något av de nämnda målen är klart,
 * t.ex. en bronze defender när dragon defendern är klar. Det och aggregaten upprepas tills
 * inget mer ändras, så att kedjor av sådana mål klaras i en och samma utvärdering.
 */
public final class GoalEvaluator
{
	@Value
	public static class Result
	{
		// Progress per mål-id, bara för mål med krav och känd data.
		Map<String, GoalProgress> progress;
		// Mål som bockades av i just den här utvärderingen.
		List<Goal> newlyCompleted;
	}

	private GoalEvaluator()
	{
	}

	public static Result evaluate(GoalTree tree, GameData data, long now)
	{
		return evaluate(tree, data, now, Goal::getTemplateNodeKey);
	}

	/**
	 * @param keyOf målets nyckel som {@code doneWhen} jämförs med, t.ex. "item:12954"
	 */
	public static Result evaluate(GoalTree tree, GameData data, long now, Function<Goal, String> keyOf)
	{
		Map<String, GoalProgress> progress = new HashMap<>();
		List<Goal> newlyCompleted = new ArrayList<>();

		tree.forEach(goal ->
		{
			Requirement requirement = goal.getRequirement();
			if (requirement == null)
			{
				return;
			}

			GoalProgress goalProgress = requirement.progress(data);
			if (goalProgress == null)
			{
				return;
			}

			progress.put(goal.getId(), goalProgress);
			if (goalProgress.isComplete() && !goal.isCompleted())
			{
				goal.markCompleted(true, now);
				newlyCompleted.add(goal);
			}
		});

		// Mål vars progress kommer från kravet; för övriga aggregat visas räknaren.
		Set<String> fromRequirement = new HashSet<>(progress.keySet());
		List<Goal> order = childrenFirst(tree);
		// Varje varv bockar av minst ett mål, så det tar slut senast när alla är klara.
		boolean changed = true;
		while (changed)
		{
			changed = completeImplied(order, now, keyOf, newlyCompleted, data);
			changed |= completeAggregates(tree, order, now, progress, fromRequirement, newlyCompleted);
		}

		return new Result(Collections.unmodifiableMap(progress), Collections.unmodifiableList(newlyCompleted));
	}

	// Mål som bockas av för att ett annat mål de nämner i doneWhen är klart.
	private static boolean completeImplied(List<Goal> goals, long now, Function<Goal, String> keyOf,
		List<Goal> newlyCompleted, GameData data)
	{
		Set<String> doneKeys = new HashSet<>();
		boolean anyWaiting = false;
		for (Goal goal : goals)
		{
			if (goal.isCompleted())
			{
				String key = keyOf.apply(goal);
				if (key != null)
				{
					doneKeys.add(key);
				}
			}
			else if (goal.getDoneWhen() != null && !goal.getDoneWhen().isEmpty())
			{
				anyWaiting = true;
			}
		}
		if (!anyWaiting)
		{
			return false;
		}

		boolean changed = false;
		for (Goal goal : goals)
		{
			if (!goal.isCompleted() && implied(goal, doneKeys, data))
			{
				goal.markCompleted(true, now);
				newlyCompleted.add(goal);
				changed = true;
			}
		}
		return changed;
	}

	// Klart via doneWhen: någon nyckel, eller med doneWhenAll alla, är klar som mål eller i spelet.
	private static boolean implied(Goal goal, Set<String> doneKeys, GameData data)
	{
		List<String> keys = goal.getDoneWhen();
		if (keys == null || keys.isEmpty())
		{
			return false;
		}
		if (!Boolean.TRUE.equals(goal.getDoneWhenAll()))
		{
			return impliedBy(keys, doneKeys) || metInGame(keys, data);
		}
		for (String key : keys)
		{
			if (!doneKeys.contains(key) && !metInGame(List.of(key), data))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Som {@link #metInGame(List, GameData)}, men med {@code all} måste varje nyckel vara uppfylld.
	 */
	public static boolean metInGame(@Nullable List<String> doneWhen, boolean all, GameData data)
	{
		if (!all || doneWhen == null || doneWhen.isEmpty())
		{
			return metInGame(doneWhen, data);
		}
		for (String key : doneWhen)
		{
			if (!metInGame(List.of(key), data))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Om någon av nycklarna redan är uppfylld i spelet, t.ex. "quest:47" när Fairytale II är klart,
	 * även om det inte finns något mål med den nyckeln.
	 */
	public static boolean metInGame(@Nullable List<String> doneWhen, GameData data)
	{
		if (doneWhen == null)
		{
			return false;
		}
		for (String key : doneWhen)
		{
			Requirement requirement = KeyRequirements.parse(key);
			if (requirement != null)
			{
				GoalProgress progress = requirement.progress(data);
				if (progress != null && progress.isComplete())
				{
					return true;
				}
			}
		}
		return false;
	}

	private static boolean impliedBy(@Nullable List<String> doneWhen, Set<String> doneKeys)
	{
		if (doneWhen == null)
		{
			return false;
		}
		for (String key : doneWhen)
		{
			if (doneKeys.contains(key))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean completeAggregates(GoalTree tree, List<Goal> order, long now,
		Map<String, GoalProgress> progress, Set<String> fromRequirement, List<Goal> newlyCompleted)
	{
		boolean changed = false;
		for (Goal goal : order)
		{
			if (!goal.isAggregate())
			{
				continue;
			}

			GoalProgress counter = aggregateProgress(tree, goal);
			if (goal.getRequirement() == null)
			{
				progress.put(goal.getId(), counter);
				if (counter.isComplete() && !goal.isCompleted())
				{
					goal.markCompleted(true, now);
					newlyCompleted.add(goal);
					changed = true;
				}
			}
			else if (!fromRequirement.contains(goal.getId()))
			{
				// Med eget krav styr kravet när målet är klart; räknaren visas bara.
				progress.put(goal.getId(), counter);
			}
		}
		return changed;
	}

	// Postordning: varje mål kommer efter alla sina delmål, även delade delmål i en DAG.
	private static List<Goal> childrenFirst(GoalTree tree)
	{
		List<Goal> order = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		for (Goal root : tree.getRoots())
		{
			postOrder(tree, root, visited, order);
		}
		return order;
	}

	private static void postOrder(GoalTree tree, Goal goal, Set<String> visited, List<Goal> order)
	{
		if (!visited.add(goal.getId()))
		{
			return;
		}
		for (Goal child : tree.getChildren(goal))
		{
			postOrder(tree, child, visited, order);
		}
		order.add(goal);
	}

	/**
	 * Hur många av målets direkta delmål som är klara, t.ex. "142/170". Ett aggregat utan
	 * delmål är aldrig klart, så att ett tomt mål inte bockas av direkt.
	 */
	public static GoalProgress aggregateProgress(GoalTree tree, Goal goal)
	{
		List<Goal> children = tree.getChildren(goal);
		int done = 0;
		for (Goal child : children)
		{
			if (child.isCompleted())
			{
				done++;
			}
		}
		boolean complete = !children.isEmpty() && done == children.size();
		return new GoalProgress(done, children.size(), complete, done + "/" + children.size());
	}
}
