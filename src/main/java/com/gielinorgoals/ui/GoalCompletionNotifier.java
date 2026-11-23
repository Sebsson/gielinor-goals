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
package com.gielinorgoals.ui;

import com.gielinorgoals.GielinorGoalsConfig;
import com.gielinorgoals.PopupLevel;
import com.gielinorgoals.model.CompletionFilter;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.tracking.InitialSync;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Gör om automatiskt klarade mål till popup-notiser: filtrerar bort delmål vars
 * förfader klarades samtidigt, väljer ljud och förbereder all text.
 * Under första synken ({@link InitialSync}) är den tyst, så att mål som klarades
 * offline bara bockas av utan popup.
 */
@Singleton
public class GoalCompletionNotifier
{
	private final GielinorGoalsConfig config;
	private final NotificationQueue queue;
	private final BiConsumer<Requirement, Consumer<BufferedImage>> iconLoader;
	private final InitialSync initialSync;

	@Inject
	GoalCompletionNotifier(GielinorGoalsConfig config, NotificationQueue queue, GoalIcons icons,
		InitialSync initialSync)
	{
		this(config, queue, icons::loadImage, initialSync);
	}

	GoalCompletionNotifier(GielinorGoalsConfig config, NotificationQueue queue,
		BiConsumer<Requirement, Consumer<BufferedImage>> iconLoader, InitialSync initialSync)
	{
		this.config = config;
		this.queue = queue;
		this.iconLoader = iconLoader;
		this.initialSync = initialSync;
	}

	public void clear()
	{
		queue.clear();
	}

	/**
	 * Mål som bockats av automatiskt (aldrig manuella bockar). Körs på EDT.
	 */
	public void onAutoCompleted(GoalTree tree, List<Goal> completed)
	{
		PopupLevel level = config.popupLevel();
		if (level == PopupLevel.OFF || completed.isEmpty() || initialSync.isActive())
		{
			return;
		}

		long holdMs = config.popupDuration() * 1000L;
		boolean sound = config.soundEnabled();
		for (CompletionFilter.Entry entry : CompletionFilter.filter(tree, completed, level == PopupLevel.ROOTS_ONLY))
		{
			queue.offer(toNotice(entry, holdMs, sound));
		}
	}

	private PopupNotice toNotice(CompletionFilter.Entry entry, long holdMs, boolean sound)
	{
		Goal goal = entry.getGoal();
		String name = goal.getDisplayTitle();
		if (entry.getHiddenDescendants() > 0)
		{
			name += " (+" + entry.getHiddenDescendants()
				+ (entry.getHiddenDescendants() == 1 ? " sub-goal)" : " sub-goals)");
		}

		String detail = null;
		if (entry.getOpenParent() != null)
		{
			detail = entry.getParentDone() + "/" + entry.getParentTotal() + " sub-goals done in "
				+ entry.getOpenParent().getDisplayTitle();
		}

		int soundId = PopupNotice.NO_SOUND;
		if (sound)
		{
			soundId = (entry.isRoot() ? config.rootGoalSound() : config.goalSound()).getSoundId();
		}

		Requirement requirement = goal.getRequirement();
		PopupNotice notice = new PopupNotice("Goal completed!", name, detail, requirement != null,
			entry.isRoot(), soundId, holdMs);
		notice.setGoalId(goal.getId());
		if (requirement != null)
		{
			iconLoader.accept(requirement, notice::setIcon);
		}
		return notice;
	}
}
