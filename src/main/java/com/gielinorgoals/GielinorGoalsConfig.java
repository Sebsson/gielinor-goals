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
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(GielinorGoalsConfig.GROUP)
public interface GielinorGoalsConfig extends Config
{
	// Gruppnamnet får aldrig bytas utan migrering, eftersom målen sparas under det.
	String GROUP = "gielinorgoals";
	String HIDE_COMPLETED_KEY = "hideCompleted";
	String OVERLAY_GOAL_COUNT_KEY = "overlayGoalCount";
	String SHOW_TIME_TO_COMPLETE_KEY = "showTimeToComplete";
	String FETCH_HISCORES_KEY = "fetchHiscores";
	String MAP_WINDOW_BOUNDS_KEY = "mapWindowBounds";

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

	@ConfigSection(
		name = "Notifications",
		description = "Popup and sound when goals are completed automatically",
		position = 10
	)
	String notificationsSection = "notifications";

	// Enum-värdena sparas med sina namn, så konstanterna i PopupLevel och
	// CompletionSound får inte bytas namn utan migrering.
	@ConfigItem(
		keyName = "popupLevel",
		name = "Popup",
		description = "Which automatically completed goals show a popup at the top of the screen",
		position = 11,
		section = notificationsSection
	)
	default PopupLevel popupLevel()
	{
		return PopupLevel.ALL;
	}

	@Range(min = 2, max = 10)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "popupDuration",
		name = "Popup duration",
		description = "How long each popup stays on screen",
		position = 12,
		section = notificationsSection
	)
	default int popupDuration()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "soundEnabled",
		name = "Play sound",
		description = "Play a game sound effect with each popup (uses the in-game sound effect volume)",
		position = 13,
		section = notificationsSection
	)
	default boolean soundEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "goalSound",
		name = "Sub-goal sound",
		description = "Sound for goals that are part of a bigger goal",
		position = 14,
		section = notificationsSection
	)
	default CompletionSound goalSound()
	{
		return CompletionSound.TROPHY_DING;
	}

	@ConfigItem(
		keyName = "rootGoalSound",
		name = "Main goal sound",
		description = "Sound for top-level goals",
		position = 15,
		section = notificationsSection
	)
	default CompletionSound rootGoalSound()
	{
		return CompletionSound.FANFARE;
	}

	@ConfigSection(
		name = "Completed",
		description = "The Completed tab in the side panel",
		position = 20
	)
	String completedSection = "completed";

	@ConfigItem(
		keyName = SHOW_TIME_TO_COMPLETE_KEY,
		name = "Show time to complete",
		description = "Show how many days each goal took, when both dates are known",
		position = 21,
		section = completedSection
	)
	default boolean showTimeToComplete()
	{
		return true;
	}

	@ConfigSection(
		name = "Kill count",
		description = "Kill count, clue and activity goals",
		position = 30
	)
	String killCountSection = "killCount";

	// HiscoreClient går direkt mot Jagex egna hiscores (secure/services.runescape.com),
	// inte via en tredjepartsserver, så växeln får vara på som standard.
	@ConfigItem(
		keyName = FETCH_HISCORES_KEY,
		name = "Fetch from hiscores",
		description = "Look up your kill counts, clues and activities on the official OSRS hiscores after login"
			+ " and when you add a kill count goal",
		position = 31,
		section = killCountSection
	)
	default boolean fetchHiscores()
	{
		return true;
	}

	// Kartfönstrets position och storlek, "x,y,bredd,höjd". Sätts av fönstret själv.
	@ConfigItem(
		keyName = MAP_WINDOW_BOUNDS_KEY,
		name = "",
		description = "",
		hidden = true
	)
	default String mapWindowBounds()
	{
		return "";
	}
}
