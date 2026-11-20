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
import com.gielinorgoals.model.NextGoals;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Visar nästa mål i spelet. Raderna räknas ut på EDT när målen ändras och lämnas hit
 * som en oföränderlig lista, så att render (som körs varje frame) inte gör något arbete.
 */
@Singleton
public class NextGoalOverlay extends OverlayPanel
{
	private static final int WIDTH = 190;

	private final Client client;
	private final GielinorGoalsConfig config;

	private volatile List<NextGoals.Entry> entries = Collections.emptyList();

	@Inject
	NextGoalOverlay(Client client, GielinorGoalsConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		addMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, OverlayManager.OPTION_CONFIGURE, "Gielinor Goals overlay");
	}

	public void setEntries(List<NextGoals.Entry> entries)
	{
		this.entries = entries;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<NextGoals.Entry> current = entries;
		if (!config.showOverlay() || current.isEmpty() || client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}

		panelComponent.setPreferredSize(new Dimension(WIDTH, 0));
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(current.size() == 1 ? "Next goal" : "Next goals")
			.color(ColorScheme.BRAND_ORANGE)
			.build());

		for (NextGoals.Entry entry : current)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(entry.getTitle())
				.right(entry.getProgressLabel() == null ? "" : entry.getProgressLabel())
				.rightColor(colorFor(entry.getFraction()))
				.build());
		}
		return super.render(graphics);
	}

	// Från vitt mot grönt ju närmare målet man är.
	private static Color colorFor(double fraction)
	{
		if (fraction >= 0.75)
		{
			return Color.GREEN;
		}
		if (fraction >= 0.4)
		{
			return Color.YELLOW;
		}
		return Color.WHITE;
	}
}
