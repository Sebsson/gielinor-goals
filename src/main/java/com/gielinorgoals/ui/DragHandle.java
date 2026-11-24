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

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.BooleanSupplier;
import javax.swing.JComponent;
import net.runelite.client.ui.ColorScheme;

/**
 * Greppet till vänster på en rad: två kolumner med tre prickar (⋮⋮), ritade i stället för
 * tecken eftersom RuneScape-typsnittet saknar dem. Prickarna syns bara när {@code shown}
 * säger det (raden hovras eller dras); platsen finns alltid så att raden inte hoppar.
 */
final class DragHandle extends JComponent
{
	private static final int WIDTH = 8;
	private static final int DOT = 2;
	private static final int GAP = 2;

	private final BooleanSupplier shown;

	DragHandle(BooleanSupplier shown)
	{
		this.shown = shown;
		setPreferredSize(new Dimension(WIDTH, 16));
		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		setToolTipText("Drag to change the order");
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		if (!shown.getAsBoolean())
		{
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		int columnsWidth = DOT * 2 + GAP;
		int rowsHeight = DOT * 3 + GAP * 2;
		int x0 = (getWidth() - columnsWidth) / 2;
		int y0 = (getHeight() - rowsHeight) / 2;
		for (int column = 0; column < 2; column++)
		{
			for (int row = 0; row < 3; row++)
			{
				g2.fillOval(x0 + column * (DOT + GAP), y0 + row * (DOT + GAP), DOT, DOT);
			}
		}
		g2.dispose();
	}
}
