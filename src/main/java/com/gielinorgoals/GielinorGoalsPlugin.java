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

import com.gielinorgoals.model.DiaryJournal;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.NextGoals;
import com.gielinorgoals.tracking.AchievementTracker;
import com.gielinorgoals.tracking.DiaryJournalParser;
import com.gielinorgoals.tracking.GameSnapshot;
import com.gielinorgoals.tracking.InitialSync;
import com.gielinorgoals.tracking.ItemTracker;
import com.gielinorgoals.tracking.SkillTracker;
import com.gielinorgoals.tracking.WatchedAchievements;
import com.gielinorgoals.ui.GielinorGoalsPanel;
import com.gielinorgoals.ui.GoalCompletedOverlay;
import com.gielinorgoals.ui.GoalCompletionNotifier;
import com.gielinorgoals.ui.NextGoalOverlay;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Gielinor Goals",
	description = "A goal and todo list that ticks off goals automatically from game data",
	tags = {"goals", "todo", "checklist", "ironman", "progress"}
)
public class GielinorGoalsPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private GoalManager goalManager;

	@Inject
	private SkillTracker skillTracker;

	@Inject
	private ItemTracker itemTracker;

	@Inject
	private AchievementTracker achievementTracker;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private GielinorGoalsConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private NextGoalOverlay nextGoalOverlay;

	@Inject
	private GoalCompletedOverlay goalCompletedOverlay;

	@Inject
	private GoalCompletionNotifier completionNotifier;

	@Inject
	private InitialSync initialSync;

	private GielinorGoalsPanel panel;
	private NavigationButton navigationButton;

	// Senaste data från varje tracker; används bara på klienttråden.
	private Map<Skill, Long> latestXp = Collections.emptyMap();
	private GameSnapshot.Items latestItems = GameSnapshot.Items.UNKNOWN;
	private AchievementTracker.Achievements latestAchievements = AchievementTracker.Achievements.NONE;

	@Override
	protected void startUp() throws Exception
	{
		panel = injector.getInstance(GielinorGoalsPanel.class);
		goalManager.setListener(this::onGoalsChanged);
		goalManager.setCompletionListener(this::announceCompleted);

		// Läs alla stats vid nästa tick om pluginet slås på medan man är inloggad.
		clientThread.invokeLater(() ->
		{
			boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
			latestXp = Collections.emptyMap();
			latestItems = GameSnapshot.Items.UNKNOWN;
			latestAchievements = AchievementTracker.Achievements.NONE;
			skillTracker.reset(loggedIn);
			itemTracker.reset(loggedIn);
			achievementTracker.reset();
		});

		BufferedImage icon = ImageUtil.loadImageResource(GielinorGoalsPlugin.class, "panel_icon.png");
		navigationButton = NavigationButton.builder()
			.tooltip("Gielinor Goals")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		overlayManager.add(nextGoalOverlay);
		overlayManager.add(goalCompletedOverlay);

		// Mål som klarades medan pluginet var avstängt ska inte ge popups.
		initialSync.start();
		// Om pluginet slås på medan man redan är inloggad finns profilen redan.
		goalManager.loadProfile(configManager.getRSProfileKey());
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navigationButton);
		overlayManager.remove(nextGoalOverlay);
		overlayManager.remove(goalCompletedOverlay);
		completionNotifier.clear();
		nextGoalOverlay.setEntries(Collections.emptyList());
		goalManager.setListener(() ->
		{
		});
		goalManager.setCompletionListener(goals ->
		{
		});
		goalManager.unload();
		achievementTracker.setWatched(WatchedAchievements.NONE);
		navigationButton = null;
		panel = null;
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		// Eventet kan komma från klienttråden; modellen ägs av EDT.
		String newProfile = event.getNewProfile();
		SwingUtilities.invokeLater(() ->
		{
			// Notiser från förra profilen visas inte, och första synken för den nya är tyst.
			completionNotifier.clear();
			initialSync.start();
			goalManager.loadProfile(newProfile);
		});
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		skillTracker.onStatChanged(event.getSkill(), event.getXp());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		skillTracker.onGameStateChanged(event.getGameState());
		itemTracker.onGameStateChanged(event.getGameState());
		achievementTracker.onGameStateChanged(event.getGameState());

		if (event.getGameState() == GameState.LOGGED_IN)
		{
			// Första synken efter inloggning (och världsbyte) ska bara bocka av tyst.
			initialSync.start();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			completionNotifier.clear();
		}
	}

	/**
	 * Läser en diary-journal när den öppnas. Högre prioritet än RuneLites Diary Requirements-plugin,
	 * så att texten läses innan den pluginen lägger till sina krav i raderna.
	 */
	@Subscribe(priority = 1)
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() != InterfaceID.JOURNALSCROLL)
		{
			return;
		}

		Widget title = client.getWidget(InterfaceID.Journalscroll.TITLE);
		Widget textLayer = client.getWidget(InterfaceID.Journalscroll.TEXTLAYER);
		if (title == null || textLayer == null
			|| !Text.removeTags(title.getText()).toLowerCase(Locale.ROOT).startsWith("achievement diary"))
		{
			return;
		}

		Widget[] children = textLayer.getStaticChildren();
		if (children == null || children.length < 2)
		{
			return;
		}

		// Första raden är områdets rubrik, resten är nivåer och uppgifter.
		List<String> lines = new ArrayList<>();
		for (int i = 1; i < children.length; i++)
		{
			lines.add(children[i].getText());
		}
		DiaryJournal journal = DiaryJournalParser.parse(children[0].getText(), lines);
		if (journal != null && !journal.getMismatchedTiers().isEmpty())
		{
			// Felsökning om Jagex ändrar journalens format: vilka nivåer som inte gick att läsa.
			log.debug("Diary journal {} could not be read for tiers {}", journal.getArea(), journal.getMismatchedTiers());
		}
		if (journal == null)
		{
			return;
		}

		SwingUtilities.invokeLater(() ->
		{
			List<String> messages = goalManager.applyDiaryJournal(journal);
			messages.forEach(this::sendChatMessage);
		});
	}

	private void sendChatMessage(String text)
	{
		String message = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append("Gielinor Goals: ")
			.append(ChatColorType.NORMAL)
			.append(Text.escapeJagex(text))
			.build();
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// Kommer mycket ofta; trackern läser först vid nästa tick.
		achievementTracker.onVarChanged();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		itemTracker.onItemContainerChanged(event.getContainerId(), event.getItemContainer());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Profilen fångas här på klienttråden, tillsammans med datan den hör till.
		String profile = configManager.getRSProfileKey();

		Map<Skill, Long> xp = skillTracker.onGameTick();
		GameSnapshot.Items items = itemTracker.onGameTick(profile);
		AchievementTracker.Achievements achievements = achievementTracker.onGameTick();
		if (xp == null && items == null && achievements == null)
		{
			return;
		}

		if (xp != null)
		{
			latestXp = xp;
		}
		if (items != null)
		{
			latestItems = items;
		}
		if (achievements != null)
		{
			latestAchievements = achievements;
		}

		GameSnapshot snapshot = new GameSnapshot(latestXp, latestItems, latestAchievements);
		SwingUtilities.invokeLater(() -> goalManager.updateGameData(profile, snapshot));
	}

	// Körs på EDT efter varje ändring i målen eller spelläget.
	private void onGoalsChanged()
	{
		if (panel != null)
		{
			panel.rebuild();
		}
		achievementTracker.setWatched(WatchedAchievements.from(goalManager.getTree()));
		nextGoalOverlay.setEntries(goalManager.hasProfile()
			? NextGoals.select(goalManager.getTree(), goalManager::getProgress, config.overlayGoalCount())
			: Collections.emptyList());
	}

	// Anropas bara för automatiskt klarade mål; manuella bockar i panelen går inte hit.
	private void announceCompleted(List<Goal> goals)
	{
		completionNotifier.onAutoCompleted(goalManager.getTree(), goals);
		if (!config.notifyOnCompletion())
		{
			return;
		}

		for (Goal goal : goals)
		{
			String message = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append("Goal completed: ")
				.append(ChatColorType.NORMAL)
				.append(Text.escapeJagex(goal.getDisplayTitle()))
				.build();

			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage(message)
				.build());
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (GielinorGoalsConfig.GROUP.equals(event.getGroup())
			&& (GielinorGoalsConfig.HIDE_COMPLETED_KEY.equals(event.getKey())
			|| GielinorGoalsConfig.OVERLAY_GOAL_COUNT_KEY.equals(event.getKey())
			|| GielinorGoalsConfig.SHOW_TIME_TO_COMPLETE_KEY.equals(event.getKey())))
		{
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					onGoalsChanged();
				}
			});
		}
	}

	@Provides
	GielinorGoalsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GielinorGoalsConfig.class);
	}
}
