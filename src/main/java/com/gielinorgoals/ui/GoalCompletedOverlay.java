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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.Point;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Popup högst upp i spelvyn när ett mål klaras. Notiserna kommer färdiga från
 * {@link NotificationQueue}; här räknas bara storleken ut vid första framen och
 * sedan ritas samma sak varje frame med en genomskinlighet som styrs av tiden.
 */
@Singleton
public class GoalCompletedOverlay extends Overlay
{
	private static final int PADDING_X = 12;
	private static final int PADDING_Y = 8;
	private static final int ICON_BOX = 32;
	private static final int ICON_GAP = 10;
	private static final int LINE_GAP = 2;
	private static final int MIN_WIDTH = 180;

	// OSRS-aktiga färger: mörkbrun bakgrund, guldkant och guldtitel.
	private static final Color BACKGROUND = new Color(28, 22, 15, 215);
	private static final Color BORDER_OUTER = new Color(15, 10, 5, 230);
	private static final Color BORDER = new Color(200, 160, 72);
	private static final Color TITLE = new Color(255, 200, 40);
	private static final Color NAME = Color.WHITE;
	private static final Color DETAIL = new Color(190, 180, 160);
	private static final Stroke BORDER_STROKE = new BasicStroke(2f);
	public static final String SHOW_ON_MAP = "Show on map";

	private final Client client;
	private final ClientThread clientThread;
	private final NotificationQueue queue;
	// Målet i notisen som visas just nu; läses när "Show on map" väljs i menyn.
	@Nullable
	private volatile String shownGoalId;

	@Inject
	GoalCompletedOverlay(Client client, ClientThread clientThread, NotificationQueue queue)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.queue = queue;
		setPosition(OverlayPosition.TOP_CENTER);
		// Högerklick på popupen; menyvalet hanteras i pluginet och skickar inget till servern.
		addMenuEntry(MenuAction.RUNELITE_OVERLAY, SHOW_ON_MAP, "Gielinor Goals");
	}

	@Nullable
	public String getShownGoalId()
	{
		return shownGoalId;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}

		long now = System.currentTimeMillis();
		PopupNotice notice = queue.current(now, isHovered());
		shownGoalId = notice == null ? null : notice.getGoalId();
		if (notice == null)
		{
			return null;
		}

		if (notice.takeSound())
		{
			// Ljudet spelas en gång, när notisen börjar visas.
			int soundId = notice.getSoundId();
			clientThread.invokeLater(() -> client.playSoundEffect(soundId));
		}

		Font titleFont = FontManager.getRunescapeBoldFont();
		Font textFont = FontManager.getRunescapeFont();
		FontMetrics titleMetrics = graphics.getFontMetrics(titleFont);
		FontMetrics textMetrics = graphics.getFontMetrics(textFont);
		if (notice.width < 0)
		{
			layout(notice, titleMetrics, textMetrics);
		}

		float alpha = NotificationQueue.alpha(notice, now);
		Composite oldComposite = graphics.getComposite();
		Stroke oldStroke = graphics.getStroke();
		Object oldAntialias = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

		int width = notice.width;
		int height = notice.height;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(BACKGROUND);
		graphics.fillRoundRect(0, 0, width, height, 8, 8);
		graphics.setStroke(BORDER_STROKE);
		graphics.setColor(BORDER_OUTER);
		graphics.drawRoundRect(0, 0, width - 1, height - 1, 8, 8);
		graphics.setColor(BORDER);
		graphics.drawRoundRect(2, 2, width - 5, height - 5, 6, 6);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);

		int textX = PADDING_X;
		if (notice.isHasIcon())
		{
			BufferedImage icon = notice.getIcon();
			if (icon != null)
			{
				int iconX = PADDING_X + (ICON_BOX - icon.getWidth()) / 2;
				int iconY = (height - icon.getHeight()) / 2;
				graphics.drawImage(icon, iconX, iconY, null);
			}
			textX += ICON_BOX + ICON_GAP;
		}

		int y = PADDING_Y + titleMetrics.getAscent();
		drawShadowed(graphics, titleFont, TITLE, notice.getTitle(), textX, y);
		y += titleMetrics.getDescent() + LINE_GAP + textMetrics.getAscent();
		drawShadowed(graphics, textFont, NAME, notice.getName(), textX, y);
		if (notice.getDetail() != null)
		{
			y += textMetrics.getDescent() + LINE_GAP + textMetrics.getAscent();
			drawShadowed(graphics, textFont, DETAIL, notice.getDetail(), textX, y);
		}

		graphics.setStroke(oldStroke);
		graphics.setComposite(oldComposite);
		return new Dimension(width, height);
	}

	// Notisen stannar kvar medan musen är över den, så att man hinner läsa.
	// Ytan från förra framen räcker; notisen flyttar sig inte medan den visas.
	private boolean isHovered()
	{
		Rectangle bounds = getBounds();
		Point mouse = client.getMouseCanvasPosition();
		return bounds != null && mouse != null && !bounds.isEmpty() && bounds.contains(mouse.getX(), mouse.getY());
	}

	// Räknas ut en gång per notis; texten ändras inte medan den visas.
	private static void layout(PopupNotice notice, FontMetrics titleMetrics, FontMetrics textMetrics)
	{
		int textWidth = Math.max(titleMetrics.stringWidth(notice.getTitle()), textMetrics.stringWidth(notice.getName()));
		int textHeight = titleMetrics.getHeight() + LINE_GAP + textMetrics.getHeight();
		if (notice.getDetail() != null)
		{
			textWidth = Math.max(textWidth, textMetrics.stringWidth(notice.getDetail()));
			textHeight += LINE_GAP + textMetrics.getHeight();
		}

		int iconWidth = notice.isHasIcon() ? ICON_BOX + ICON_GAP : 0;
		notice.width = Math.max(MIN_WIDTH, PADDING_X * 2 + iconWidth + textWidth);
		notice.height = PADDING_Y * 2 + Math.max(textHeight, notice.isHasIcon() ? ICON_BOX : 0);
	}

	private static void drawShadowed(Graphics2D graphics, Font font, Color color, String text, int x, int y)
	{
		graphics.setFont(font);
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}
}
