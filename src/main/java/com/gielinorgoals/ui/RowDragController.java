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

import com.gielinorgoals.GoalManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;

/**
 * Dra och släpp i mållistan med vanliga muslyssnare (inte Swings DnD-ramverk). Raden dras i
 * sitt grepp; ett mål flyttas bara bland syskonen under samma förälder-instans, alltså de rader
 * som visas under samma förälderrad. En linje visar var raden hamnar och syns bara på giltiga
 * platser. Esc, eller att släppa utanför en giltig plats, avbryter utan ändring.
 * <p>
 * Allt körs på EDT. Auto-scroll-timern och Esc-bindningen finns bara under dragningen och tas
 * alltid bort när den slutar, vare sig den släpps eller avbryts.
 */
final class RowDragController
{
	// Så nära över- eller underkanten som listan börjar scrolla, och hur fort.
	private static final int EDGE_PX = 28;
	private static final int SCROLL_STEP_PX = 14;
	private static final int SCROLL_DELAY_MS = 30;
	private static final Color LINE_COLOR = ColorScheme.BRAND_ORANGE;
	private static final KeyStroke ESCAPE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	private static final String CANCEL_ACTION = "gielinorGoalsCancelRowDrag";

	private final JComponent list;
	private final GoalManager goalManager;
	// Körs när dragningen är slut, t.ex. för en ombyggnad som väntade under den.
	private final Runnable onEnd;

	@Nullable
	private GoalRow dragged;
	@Nullable
	private Component source;
	// Alla rader i listans ordning, och de synliga syskonen (samma förälder-instans).
	private List<GoalRow> rows = List.of();
	private List<GoalRow> siblings = List.of();
	// Index i rows för varje syskons första och sista rad (syskonet och dess utfällda delträd).
	private int[] blockStart = new int[0];
	private int[] blockEnd = new int[0];
	// Luckan musen pekar på, -1 om platsen inte är giltig.
	private int gap = -1;
	@Nullable
	private Point lastScreen;
	@Nullable
	private Timer scrollTimer;

	RowDragController(JComponent list, GoalManager goalManager, Runnable onEnd)
	{
		this.list = list;
		this.goalManager = goalManager;
		this.onEnd = onEnd;
		// Vanlig Swing-tangentbindning på listan i stället för en global tangentlyssnare.
		// Den gör ingenting när ingen rad dras.
		list.getActionMap().put(CANCEL_ACTION, new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				cancel();
			}
		});
	}

	boolean isActive()
	{
		return dragged != null;
	}

	boolean isDragging(GoalRow row)
	{
		return dragged == row;
	}

	/**
	 * Musknappen trycktes ned på radens grepp.
	 */
	void start(GoalRow row, MouseEvent e)
	{
		if (isActive() || !SwingUtilities.isLeftMouseButton(e) || !goalManager.isEditable())
		{
			return;
		}
		dragged = row;
		source = e.getComponent();
		collectRows(row);
		row.setLifted(true);

		// Esc avbryter bara under dragningen, och bara när fokus ligger i RuneLite-fönstrets Swing-delar.
		list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ESCAPE, CANCEL_ACTION);
		scrollTimer = new Timer(SCROLL_DELAY_MS, ev -> autoScroll());
		update(e);
	}

	void drag(MouseEvent e)
	{
		if (isActive())
		{
			update(e);
		}
	}

	void release(MouseEvent e)
	{
		if (!isActive())
		{
			return;
		}
		update(e);
		GoalRow row = dragged;
		int target = gap;
		List<String> visible = siblingIds();
		end();
		if (target < 0 || DropPlacement.changesNothing(visible, row.getGoalId(), target))
		{
			return;
		}
		String before = DropPlacement.beforeId(goalManager.getTree(), row.getParentId(), row.getGoalId(), visible,
			target);
		goalManager.moveWithinParent(row.getParentId(), row.getGoalId(), before);
	}

	/**
	 * Avbryter utan ändring, t.ex. för Esc eller när raden tas bort.
	 */
	void cancel()
	{
		if (isActive())
		{
			end();
		}
	}

	/**
	 * Ritar linjen för den giltiga platsen; anropas från listans paintChildren.
	 */
	void paint(Graphics g)
	{
		if (!isActive() || gap < 0)
		{
			return;
		}
		int y = lineY(gap);
		int x = dragged.getIndentPx();
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(LINE_COLOR);
		g2.fillRect(x + 4, y - 1, list.getWidth() - x - 4, 2);
		g2.fillOval(x, y - 3, 6, 6);
		g2.dispose();
	}

	private void collectRows(GoalRow row)
	{
		List<GoalRow> all = new ArrayList<>();
		for (Component component : list.getComponents())
		{
			if (component instanceof GoalRow)
			{
				all.add((GoalRow) component);
			}
		}
		List<GoalRow> same = new ArrayList<>();
		List<Integer> starts = new ArrayList<>();
		List<Integer> ends = new ArrayList<>();
		for (int i = 0; i < all.size(); i++)
		{
			GoalRow candidate = all.get(i);
			if (candidate.getParentRow() != row.getParentRow())
			{
				continue;
			}
			int end = i;
			while (end + 1 < all.size() && all.get(end + 1).getDepth() > candidate.getDepth())
			{
				end++;
			}
			same.add(candidate);
			starts.add(i);
			ends.add(end);
		}
		rows = all;
		siblings = same;
		blockStart = starts.stream().mapToInt(Integer::intValue).toArray();
		blockEnd = ends.stream().mapToInt(Integer::intValue).toArray();
	}

	private List<String> siblingIds()
	{
		List<String> ids = new ArrayList<>(siblings.size());
		siblings.forEach(row -> ids.add(row.getGoalId()));
		return ids;
	}

	private void update(MouseEvent e)
	{
		Point screen = e.getLocationOnScreen();
		lastScreen = screen;
		refreshGap();
		JViewport viewport = viewport();
		if (viewport != null && scrollTimer != null)
		{
			Point inViewport = new Point(screen);
			SwingUtilities.convertPointFromScreen(inViewport, viewport);
			boolean nearEdge = inViewport.y < EDGE_PX || inViewport.y > viewport.getHeight() - EDGE_PX;
			if (nearEdge && !scrollTimer.isRunning())
			{
				scrollTimer.start();
			}
		}
	}

	// Räknar om luckan från senaste muspositionen, t.ex. efter en auto-scroll.
	private void refreshGap()
	{
		if (lastScreen == null)
		{
			return;
		}
		Point point = new Point(lastScreen);
		SwingUtilities.convertPointFromScreen(point, list);
		int newGap = gapAt(point);
		if (newGap != gap)
		{
			gap = newGap;
			list.repaint();
		}
		Cursor cursor = gap >= 0 ? Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR) : noDropCursor();
		list.setCursor(cursor);
		if (source != null)
		{
			source.setCursor(cursor);
		}
	}

	/**
	 * Luckan under punkten (i listans koordinater): övre halvan av ett syskons rad ger platsen
	 * före syskonet, nedre halvan av sista raden i syskonets block (syskonet och dess utfällda
	 * delträd) platsen efter. Inuti ett delträd, under en annan förälder eller utanför den
	 * synliga delen av listan finns ingen giltig plats.
	 */
	private int gapAt(Point point)
	{
		Rectangle visible = list.getVisibleRect();
		if (rows.isEmpty() || point.x < 0 || point.x > list.getWidth() || point.y < visible.y
			|| point.y > visible.y + visible.height)
		{
			return -1;
		}
		int index = rowIndexAt(point.y);
		if (index < 0)
		{
			return -1;
		}
		GoalRow row = rows.get(index);
		boolean upperHalf = point.y < row.getY() + row.getHeight() / 2;
		for (int k = 0; k < siblings.size(); k++)
		{
			if (index == blockStart[k] && upperHalf)
			{
				return k;
			}
			if (index == blockEnd[k] && !upperHalf)
			{
				return k + 1;
			}
		}
		return -1;
	}

	// Raden vid y; mellanrummet mellan två rader räknas till raden under.
	private int rowIndexAt(int y)
	{
		for (int i = 0; i < rows.size(); i++)
		{
			GoalRow row = rows.get(i);
			if (y < row.getY() + row.getHeight())
			{
				return i;
			}
		}
		return -1;
	}

	private int lineY(int gapIndex)
	{
		if (gapIndex < siblings.size())
		{
			return Math.max(1, rows.get(blockStart[gapIndex]).getY() - 1);
		}
		GoalRow last = rows.get(blockEnd[siblings.size() - 1]);
		return last.getY() + last.getHeight() + 1;
	}

	private void autoScroll()
	{
		JViewport viewport = viewport();
		if (!isActive() || viewport == null || lastScreen == null)
		{
			stopTimer();
			return;
		}
		Point inViewport = new Point(lastScreen);
		SwingUtilities.convertPointFromScreen(inViewport, viewport);
		int step = inViewport.y < EDGE_PX ? -SCROLL_STEP_PX
			: inViewport.y > viewport.getHeight() - EDGE_PX ? SCROLL_STEP_PX : 0;
		Point position = viewport.getViewPosition();
		int max = Math.max(0, viewport.getView().getHeight() - viewport.getHeight());
		int y = Math.max(0, Math.min(max, position.y + step));
		if (step == 0 || y == position.y)
		{
			stopTimer();
			return;
		}
		viewport.setViewPosition(new Point(position.x, y));
		refreshGap();
	}

	@Nullable
	private JViewport viewport()
	{
		return (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, list);
	}

	private void stopTimer()
	{
		if (scrollTimer != null)
		{
			scrollTimer.stop();
		}
	}

	// Städar alltid: timer, Esc-lyssnare, markören och den lyfta raden.
	private void end()
	{
		stopTimer();
		scrollTimer = null;
		list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(ESCAPE);
		if (dragged != null)
		{
			dragged.setLifted(false);
		}
		if (source != null)
		{
			source.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		}
		list.setCursor(null);
		dragged = null;
		source = null;
		rows = List.of();
		siblings = List.of();
		gap = -1;
		lastScreen = null;
		list.repaint();
		onEnd.run();
	}

	private static Cursor noDropCursor()
	{
		Cursor cursor = DragSource.DefaultMoveNoDrop;
		return cursor != null ? cursor : Cursor.getDefaultCursor();
	}
}
