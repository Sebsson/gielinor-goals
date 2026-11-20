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
package com.gielinorgoals;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(GielinorGoalsConfig.GROUP)
public interface GielinorGoalsConfig extends Config
{
	// Gruppnamnet får aldrig bytas utan migrering, eftersom målen sparas under det.
	String GROUP = "gielinorgoals";
	String HIDE_COMPLETED_KEY = "hideCompleted";
	String OVERLAY_GOAL_COUNT_KEY = "overlayGoalCount";

	@ConfigItem(
		keyName = HIDE_COMPLETED_KEY,
		name = "Hide completed goals",
		description = "Hide goals that are ticked off, including their sub-goals",
		position = 1
	)
	default boolean hideCompleted()
	{
		return false;
	}

	@ConfigItem(
		keyName = "notifyOnCompletion",
		name = "Chat message on completion",
		description = "Show a chat message when a goal is ticked off automatically",
		position = 2
	)
	default boolean notifyOnCompletion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show next goals overlay",
		description = "Show your next goals in the game view",
		position = 3
	)
	default boolean showOverlay()
	{
		return true;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = OVERLAY_GOAL_COUNT_KEY,
		name = "Goals in overlay",
		description = "How many upcoming goals the overlay shows",
		position = 4
	)
	default int overlayGoalCount()
	{
		return 3;
	}
}
