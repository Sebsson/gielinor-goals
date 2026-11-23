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

import com.gielinorgoals.map.MapGraph;
import com.gielinorgoals.map.MapLayout;
import com.gielinorgoals.model.ActivityCounters;
import com.gielinorgoals.model.DryMeter;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalProgress;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.KillCountRequirement;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;

/**
 * Ritar målkartan: noder och kanter från en färdig {@link MapLayout}, med panorering,
 * zoom kring pekaren och markering av förfäder och ättlingar vid hovring.
 *
 * <p>Ingen layout räknas här; {@link #setData} får en färdig layout. Målningen ritar bara det
 * som syns, och ikoner och avkortade titlar cachas tills målen ändras. Används bara på EDT.
 */
final class GoalMapCanvas extends JComponent
{
	static final double MIN_SCALE = 0.2;
	static final double MAX_SCALE = 2.5;
	private static final double FIT_MARGIN = 40;
	private static final int ICON_SIZE = 16;
	private static final int PAD = 6;
	private static final int BADGE = 14;
	// Under den här zoomen syns inte text ändå, så den hoppas över.
	private static final double TEXT_MIN_SCALE = 0.45;

	private static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
	private static final Color NODE_FILL = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color NODE_BORDER = ColorScheme.MEDIUM_GRAY_COLOR;
	private static final Color DONE_BORDER = new Color(212, 175, 55);
	private static final Color EDGE = new Color(150, 150, 150);
	private static final Color EDGE_DONE = new Color(90, 90, 90);
	private static final Color HIGHLIGHT = ColorScheme.BRAND_ORANGE;
	private static final Color GLOW = new Color(255, 152, 31);
	private static final Color BAR_BACKGROUND = new Color(30, 30, 30);
	private static final int PULSE_MS = 1400;
	private static final int PULSE_FRAME_MS = 30;

	private final GoalIcons icons;
	private final Function<String, GoalProgress> progress;
	private final ActivityCountersSource counters;

	@Nullable
	private GoalTree tree;
	@Nullable
	private MapGraph graph;
	@Nullable
	private MapLayout layout;
	private Set<String> nextGoals = Collections.emptySet();
	private final Map<String, Goal> goals = new HashMap<>();
	// Mål med flera föräldrar (i hela grafen, inte bara på kartan); räknas när datan byts.
	private final Map<String, Integer> parentCounts = new HashMap<>();

	// Kartkoordinat → skärm: skärm = karta * scale + translate.
	private double scale = 1;
	private double translateX;
	private double translateY;
	private boolean fitted;

	@Nullable
	private String hoverId;
	private Set<String> related = Collections.emptySet();
	@Nullable
	private String selectedId;
	// Målet som ska stå i mitten tills användaren själv panorerar eller zoomar; centreras igen
	// när ytan ändrar storlek, t.ex. när detaljpanelen visas eller fönstret just öppnats.
	@Nullable
	private String centerTargetId;

	private final Map<String, Image> iconCache = new HashMap<>();
	private final Set<String> iconsLoading = new HashSet<>();
	// Vad ikonen laddades för (kravet eller den egna ikonen), så att bara ändrade mål läses om.
	private final Map<String, Object> iconSource = new HashMap<>();
	private final Map<String, String> titleCache = new HashMap<>();
	private final Font titleFont = FontManager.getRunescapeFont();
	private final Font smallFont = FontManager.getRunescapeSmallFont();

	private Consumer<String> onToggle = id ->
	{
	};
	// Klick väljer (null = klick utanför noderna), dubbelklick öppnar, högerklick ger menyn.
	private Consumer<String> onSelect = id ->
	{
	};
	private Consumer<String> onOpen = id ->
	{
	};
	private java.util.function.BiConsumer<String, Point> onMenu = (id, point) ->
	{
	};

	// Mål som just klarades → när pulsen började. Timern går bara medan någon puls lever.
	private final Map<String, Long> pulses = new HashMap<>();
	private final Timer pulseTimer;

	/**
	 * Räknarnas nuvarande värden, för "1,240 kc" på andra raden.
	 */
	interface ActivityCountersSource
	{
		ActivityCounters get();
	}

	GoalMapCanvas(GoalIcons icons, Function<String, GoalProgress> progress, ActivityCountersSource counters)
	{
		this.icons = icons;
		this.progress = progress;
		this.counters = counters;
		setOpaque(true);
		setPreferredSize(new Dimension(900, 600));
		pulseTimer = new Timer(PULSE_FRAME_MS, e -> stepPulses());
		ToolTipManager.sharedInstance().registerComponent(this);

		MouseAdapter mouse = new MouseAdapter()
		{
			private Point pressed;
			private Point last;
			private boolean dragged;

			@Override
			public void mousePressed(MouseEvent e)
			{
				pressed = e.getPoint();
				last = e.getPoint();
				dragged = false;
				maybeMenu(e);
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				String id = nodeAt(e.getPoint());
				if (id != null && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
				{
					onOpen.accept(id);
				}
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (last == null)
				{
					return;
				}
				if (!dragged && pressed.distance(e.getPoint()) < 4)
				{
					return;
				}
				dragged = true;
				centerTargetId = null;
				translateX += e.getX() - last.x;
				translateY += e.getY() - last.y;
				last = e.getPoint();
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (!dragged && SwingUtilities.isLeftMouseButton(e))
				{
					click(e.getPoint());
				}
				maybeMenu(e);
				pressed = null;
				last = null;
			}

			@Override
			public void mouseMoved(MouseEvent e)
			{
				setHover(nodeAt(e.getPoint()));
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setHover(null);
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				zoomAt(e.getPoint(), Math.pow(1.12, -e.getPreciseWheelRotation()));
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		addMouseWheelListener(mouse);
	}

	void setOnToggle(Consumer<String> onToggle)
	{
		this.onToggle = onToggle;
	}

	void setOnSelect(Consumer<String> onSelect)
	{
		this.onSelect = onSelect;
	}

	void setOnOpen(Consumer<String> onOpen)
	{
		this.onOpen = onOpen;
	}

	void setOnMenu(java.util.function.BiConsumer<String, Point> onMenu)
	{
		this.onMenu = onMenu;
	}

	private void maybeMenu(MouseEvent e)
	{
		if (!e.isPopupTrigger())
		{
			return;
		}
		String id = nodeAt(e.getPoint());
		if (id != null)
		{
			setSelected(id);
			onSelect.accept(id);
			onMenu.accept(id, e.getPoint());
		}
	}

	/**
	 * Låter nyss klarade mål pulsera en kort stund.
	 */
	void pulse(Set<String> goalIds)
	{
		long now = System.currentTimeMillis();
		for (String id : goalIds)
		{
			pulses.put(id, now);
		}
		if (!pulses.isEmpty() && !pulseTimer.isRunning())
		{
			pulseTimer.start();
		}
	}

	void stopPulses()
	{
		pulseTimer.stop();
		pulses.clear();
	}

	boolean isPulsing()
	{
		return pulseTimer.isRunning();
	}

	private void stepPulses()
	{
		long now = System.currentTimeMillis();
		pulses.values().removeIf(start -> now - start > PULSE_MS);
		if (pulses.isEmpty())
		{
			pulseTimer.stop();
		}
		repaint();
	}

	/**
	 * Ny layout eller nya mål. Ikoner och titlar läses om, eftersom ett mål kan ha ändrats.
	 */
	void setData(GoalTree tree, MapGraph graph, MapLayout layout, Set<String> nextGoals, boolean goalsChanged)
	{
		boolean newLayout = this.layout != layout;
		this.tree = tree;
		this.graph = graph;
		this.layout = layout;
		this.nextGoals = nextGoals;
		goals.clear();
		graph.getNodes().forEach(goal -> goals.put(goal.getId(), goal));
		parentCounts.clear();
		for (Goal goal : tree.flatten())
		{
			for (String childId : goal.getChildIds())
			{
				parentCounts.merge(childId, 1, Integer::sum);
			}
		}
		if (goalsChanged)
		{
			// Bara ikoner för mål som tagits bort eller fått ett annat krav eller en annan ikon;
			// övriga behålls, så att kartan inte laddar om allt vid varje sparning.
			iconSource.keySet().removeIf(id ->
			{
				Goal goal = goals.get(id);
				boolean stale = goal == null || !java.util.Objects.equals(iconSource.get(id), iconSourceOf(goal));
				if (stale)
				{
					iconCache.remove(id);
					iconsLoading.remove(id);
				}
				return stale;
			});
			titleCache.clear();
		}
		if (newLayout)
		{
			if (hoverId != null && layout.node(hoverId) == null)
			{
				hoverId = null;
			}
			related = hoverId == null ? Collections.emptySet() : relatedTo(hoverId);
		}
		if (!fitted && getWidth() > 0)
		{
			fitToWindow();
		}
		repaint();
	}

	@Nullable
	String getSelectedId()
	{
		return selectedId;
	}

	void setSelected(@Nullable String id)
	{
		selectedId = id;
		repaint();
	}

	/**
	 * Skalar och flyttar så att hela kartan syns, men förstorar aldrig över 100 %.
	 */
	void fitToWindow()
	{
		if (layout == null || getWidth() <= 0 || getHeight() <= 0)
		{
			return;
		}
		fitted = true;
		centerTargetId = null;
		double w = layout.getWidth() + 2 * FIT_MARGIN;
		double h = layout.getHeight() + 2 * FIT_MARGIN;
		scale = clamp(Math.min(getWidth() / w, getHeight() / h), MIN_SCALE, 1.0);
		translateX = (getWidth() - layout.getWidth() * scale) / 2;
		translateY = Math.max(FIT_MARGIN * scale, (getHeight() - layout.getHeight() * scale) / 2);
		repaint();
	}

	/**
	 * Flyttar kartan så att målet hamnar mitt i fönstret.
	 */
	boolean centerOn(String goalId)
	{
		MapLayout.Node node = layout == null ? null : layout.node(goalId);
		if (node == null)
		{
			return false;
		}
		fitted = true;
		scale = Math.max(scale, 0.8);
		centerTargetId = goalId;
		applyCenter();
		return true;
	}

	private void applyCenter()
	{
		MapLayout.Node node = layout == null || centerTargetId == null ? null : layout.node(centerTargetId);
		if (node == null)
		{
			centerTargetId = null;
			return;
		}
		translateX = getWidth() / 2.0 - node.centerX() * scale;
		translateY = getHeight() / 2.0 - (node.getY() + node.getHeight() / 2) * scale;
		repaint();
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		super.setBounds(x, y, width, height);
		if (width <= 0 || layout == null)
		{
			return;
		}
		if (centerTargetId != null)
		{
			applyCenter();
		}
		else if (!fitted)
		{
			fitToWindow();
		}
	}

	// För tester: kartpunkten mitt i ytan.
	Point2D worldCenterForTest()
	{
		return toWorld(new Point(getWidth() / 2, getHeight() / 2));
	}

	private void zoomAt(Point at, double factor)
	{
		double next = clamp(scale * factor, MIN_SCALE, MAX_SCALE);
		// Kartpunkten under pekaren står still.
		translateX = at.x - (at.x - translateX) * (next / scale);
		translateY = at.y - (at.y - translateY) * (next / scale);
		scale = next;
		fitted = true;
		centerTargetId = null;
		repaint();
	}

	private void click(Point point)
	{
		String id = nodeAt(point);
		if (id == null || graph == null || layout == null)
		{
			setSelected(null);
			onSelect.accept(null);
			return;
		}
		MapLayout.Node node = layout.node(id);
		Point2D world = toWorld(point);
		if (hasToggle(id) && toggleBounds(node).contains(world))
		{
			onToggle.accept(id);
			return;
		}
		setSelected(id);
		onSelect.accept(id);
	}

	private void setHover(@Nullable String id)
	{
		if (java.util.Objects.equals(id, hoverId))
		{
			return;
		}
		hoverId = id;
		related = id == null ? Collections.emptySet() : relatedTo(id);
		repaint();
	}

	// Målet, alla förfäder och alla ättlingar bland de synliga.
	private Set<String> relatedTo(String id)
	{
		Set<String> result = new HashSet<>();
		result.add(id);
		walk(id, true, result);
		walk(id, false, result);
		return result;
	}

	private void walk(String start, boolean up, Set<String> into)
	{
		if (graph == null)
		{
			return;
		}
		Deque<String> queue = new ArrayDeque<>();
		queue.add(start);
		while (!queue.isEmpty())
		{
			String id = queue.poll();
			for (String next : up ? graph.parentsOf(id) : graph.childrenOf(id))
			{
				if (into.add(next))
				{
					queue.add(next);
				}
			}
		}
	}

	@Nullable
	private String nodeAt(Point point)
	{
		if (layout == null)
		{
			return null;
		}
		Point2D world = toWorld(point);
		for (MapLayout.Node node : layout.getNodes())
		{
			if (world.getX() >= node.getX() && world.getX() <= node.getX() + node.getWidth()
				&& world.getY() >= node.getY() && world.getY() <= node.getY() + node.getHeight())
			{
				return node.getGoalId();
			}
		}
		return null;
	}

	private Point2D toWorld(Point point)
	{
		return new Point2D.Double((point.x - translateX) / scale, (point.y - translateY) / scale);
	}

	@Override
	public String getToolTipText(MouseEvent event)
	{
		String id = nodeAt(event.getPoint());
		Goal goal = id == null ? null : goals.get(id);
		if (goal == null)
		{
			return null;
		}
		StringBuilder html = new StringBuilder("<html><b>").append(UiText.escapeHtml(goal.getDisplayTitle())).append("</b>");
		GoalProgress shown = progress.apply(id);
		if (shown != null && !goal.isCompleted())
		{
			html.append("<br>").append(UiText.escapeHtml(shown.getLabel()));
		}
		if (goal.isCompleted())
		{
			html.append("<br>Done");
		}
		if (parents(id) > 1)
		{
			html.append("<br><i>Under ").append(parents(id)).append(" goals</i>");
		}
		if (graph != null && graph.isCollapsed(id))
		{
			html.append("<br><i>Click the arrow to show its sub-goals</i>");
		}
		return html.append("</html>").toString();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setColor(BACKGROUND);
			g.fillRect(0, 0, getWidth(), getHeight());
			if (layout == null || graph == null)
			{
				return;
			}
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

			Rectangle clip = g.getClipBounds() == null ? new Rectangle(0, 0, getWidth(), getHeight()) : g.getClipBounds();
			Rectangle2D visible = new Rectangle2D.Double((clip.x - translateX) / scale, (clip.y - translateY) / scale,
				clip.width / scale, clip.height / scale);

			AffineTransform screen = g.getTransform();
			g.translate(translateX, translateY);
			g.scale(scale, scale);

			for (MapLayout.Edge edge : layout.getEdges())
			{
				if (edgeVisible(edge, visible))
				{
					paintEdge(g, edge);
				}
			}
			for (MapLayout.Node node : layout.getNodes())
			{
				if (visible.intersects(node.getX() - 8, node.getY() - 8, node.getWidth() + 16, node.getHeight() + 16))
				{
					paintNode(g, node);
				}
			}
			g.setTransform(screen);
		}
		finally
		{
			g.dispose();
		}
	}

	private static boolean edgeVisible(MapLayout.Edge edge, Rectangle2D visible)
	{
		double minX = Math.min(edge.getX1(), edge.getX2());
		double maxX = Math.max(edge.getX1(), edge.getX2());
		return visible.intersects(minX, edge.getY1(), Math.max(1, maxX - minX), Math.max(1, edge.getY2() - edge.getY1()));
	}

	private void paintEdge(Graphics2D g, MapLayout.Edge edge)
	{
		Goal child = goals.get(edge.getToId());
		boolean onPath = hoverId != null && related.contains(edge.getFromId()) && related.contains(edge.getToId());
		Color color = onPath ? HIGHLIGHT : child != null && child.isCompleted() ? EDGE_DONE : EDGE;
		float alpha = hoverId != null && !onPath ? 0.15f : 1f;

		// Mjuk S-kurva; kanter över flera lager blir längre men lika mjuka.
		double dy = edge.getY2() - edge.getY1();
		Path2D path = new Path2D.Double();
		path.moveTo(edge.getX1(), edge.getY1());
		path.curveTo(edge.getX1(), edge.getY1() + dy * 0.5, edge.getX2(), edge.getY2() - dy * 0.5,
			edge.getX2(), edge.getY2());

		Composite before = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		g.setColor(color);
		g.setStroke(new BasicStroke(onPath ? 2.2f : 1.4f));
		g.draw(path);
		g.setComposite(before);
	}

	private void paintNode(Graphics2D g, MapLayout.Node node)
	{
		Goal goal = goals.get(node.getGoalId());
		if (goal == null)
		{
			return;
		}
		String id = goal.getId();
		double x = node.getX();
		double y = node.getY();
		double w = node.getWidth();
		double h = node.getHeight();
		RoundRectangle2D box = new RoundRectangle2D.Double(x, y, w, h, 10, 10);

		float alpha = 1f;
		if (hoverId != null && !related.contains(id))
		{
			alpha = 0.2f;
		}
		else if (goal.isCompleted())
		{
			alpha = 0.7f;
		}
		Composite before = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

		// "Nästa mål": en diskret glöd utanför kanten.
		if (nextGoals.contains(id) && !goal.isCompleted())
		{
			for (int i = 3; i >= 1; i--)
			{
				g.setColor(new Color(GLOW.getRed(), GLOW.getGreen(), GLOW.getBlue(), 28 * (4 - i)));
				g.setStroke(new BasicStroke(i * 2.5f));
				g.draw(new RoundRectangle2D.Double(x - i, y - i, w + 2 * i, h + 2 * i, 12 + i, 12 + i));
			}
		}

		g.setColor(NODE_FILL);
		g.fill(box);
		boolean selected = id.equals(selectedId);
		boolean hovered = id.equals(hoverId);
		g.setColor(selected || hovered ? HIGHLIGHT : goal.isCompleted() ? DONE_BORDER : NODE_BORDER);
		g.setStroke(new BasicStroke(selected ? 2.5f : goal.isCompleted() || hovered ? 1.8f : 1f));
		g.draw(box);

		Image icon = icon(goal);
		if (icon != null)
		{
			g.drawImage(icon, (int) x + PAD, (int) y + PAD, ICON_SIZE, ICON_SIZE, null);
		}

		// Märken uppe till höger: pil för ihopfällbara, bock för klara, länk för delade.
		double badgeX = x + w - PAD - BADGE;
		if (hasToggle(id))
		{
			paintToggle(g, toggleBounds(node), graph != null && graph.isCollapsed(id));
			badgeX -= BADGE + 2;
		}
		if (goal.isCompleted())
		{
			paintCheck(g, badgeX, y + PAD);
			badgeX -= BADGE + 2;
		}
		if (parents(id) > 1)
		{
			g.drawImage(GoalRow.LINK_ICON.getImage(), (int) badgeX + 1, (int) y + PAD + 1, 12, 12, null);
			badgeX -= BADGE + 2;
		}

		GoalProgress shown = progress.apply(id);
		if (scale >= TEXT_MIN_SCALE)
		{
			double textX = x + PAD + ICON_SIZE + 6;
			int titleWidth = (int) (badgeX + BADGE - textX - 2);
			g.setFont(titleFont);
			g.setColor(Color.WHITE);
			g.drawString(title(goal, titleWidth), (float) textX, (float) (y + PAD + 13));

			String second = secondLine(goal, shown);
			if (second != null)
			{
				g.setFont(smallFont);
				g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
				g.drawString(second, (float) textX, (float) (y + PAD + 28));
			}
		}

		if (shown != null)
		{
			double barX = x + PAD;
			double barY = y + h - PAD - 5;
			double barW = w - 2 * PAD;
			g.setColor(BAR_BACKGROUND);
			g.fill(new Rectangle2D.Double(barX, barY, barW, 5));
			g.setColor(goal.isCompleted() ? ColorScheme.PROGRESS_COMPLETE_COLOR : ProgressColors.forFraction(shown.fraction()));
			g.fill(new Rectangle2D.Double(barX, barY, barW * Math.max(0, Math.min(1, shown.fraction())), 5));
		}
		g.setComposite(before);

		Long pulseStart = pulses.get(id);
		if (pulseStart != null)
		{
			double t = Math.min(1, (System.currentTimeMillis() - pulseStart) / (double) PULSE_MS);
			double grow = 4 + t * 18;
			float fade = (float) (1 - t);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, fade)));
			g.setColor(DONE_BORDER);
			g.setStroke(new BasicStroke(3f));
			g.draw(new RoundRectangle2D.Double(x - grow, y - grow, w + 2 * grow, h + 2 * grow, 14 + grow, 14 + grow));
			g.setComposite(before);
		}
	}

	private static void paintToggle(Graphics2D g, Rectangle2D bounds, boolean collapsed)
	{
		// RuneScape-typsnittet saknar ▸/▾, så pilen ritas som en triangel.
		double cx = bounds.getCenterX();
		double cy = bounds.getCenterY();
		Path2D arrow = new Path2D.Double();
		if (collapsed)
		{
			arrow.moveTo(cx - 3, cy - 5);
			arrow.lineTo(cx + 4, cy);
			arrow.lineTo(cx - 3, cy + 5);
		}
		else
		{
			arrow.moveTo(cx - 5, cy - 3);
			arrow.lineTo(cx + 5, cy - 3);
			arrow.lineTo(cx, cy + 4);
		}
		arrow.closePath();
		g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		g.fill(arrow);
	}

	private static void paintCheck(Graphics2D g, double x, double y)
	{
		Path2D check = new Path2D.Double();
		check.moveTo(x + 2, y + 7);
		check.lineTo(x + 6, y + 11);
		check.lineTo(x + 12, y + 3);
		g.setColor(DONE_BORDER);
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(check);
	}

	private int parents(String id)
	{
		return parentCounts.getOrDefault(id, 0);
	}

	private boolean hasToggle(String id)
	{
		Goal goal = goals.get(id);
		return goal != null && goal.isAggregate() && goal.hasChildren();
	}

	private static Rectangle2D toggleBounds(MapLayout.Node node)
	{
		return new Rectangle2D.Double(node.getX() + node.getWidth() - PAD - BADGE, node.getY() + PAD - 2, BADGE + 4, BADGE + 4);
	}

	// "142/170" för räknarmål, "1,240 kc" för KC- och drop-mål, annars kravets progress.
	@Nullable
	private String secondLine(Goal goal, @Nullable GoalProgress shown)
	{
		if (goal.isAggregate() && shown != null)
		{
			return shown.getLabel();
		}
		if (goal.getRequirement() instanceof KillCountRequirement)
		{
			long kc = counters.get().value(((KillCountRequirement) goal.getRequirement()).counterKey());
			return kc < 0 ? null : QuantityFormatter.formatNumber(kc) + (
				((KillCountRequirement) goal.getRequirement()).getActivityType() == KillCountRequirement.ActivityType.MANUAL
					? "" : " kc");
		}
		if (DryMeter.sourceOf(goal) != null)
		{
			long kc = counters.get().value(goal.getDropSource().counterKey());
			return kc < 0 ? null : QuantityFormatter.formatNumber(kc) + " kc";
		}
		return shown == null || goal.isCompleted() ? null : shown.getLabel();
	}

	private String title(Goal goal, int width)
	{
		String key = goal.getId() + ":" + width;
		String cached = titleCache.get(key);
		if (cached != null)
		{
			return cached;
		}
		FontMetrics metrics = getFontMetrics(titleFont);
		String full = goal.getDisplayTitle();
		String result = full;
		if (metrics.stringWidth(full) > width)
		{
			int end = full.length();
			while (end > 0 && metrics.stringWidth(full.substring(0, end) + "…") > width)
			{
				end--;
			}
			result = full.substring(0, end).trim() + "…";
		}
		titleCache.put(key, result);
		return result;
	}

	@Nullable
	private Image icon(Goal goal)
	{
		String id = goal.getId();
		Image cached = iconCache.get(id);
		if (cached != null || !GoalIcons.hasIcon(goal) || !iconsLoading.add(id))
		{
			return cached;
		}
		iconSource.put(id, iconSourceOf(goal));
		Consumer<BufferedImage> store = image -> SwingUtilities.invokeLater(() ->
		{
			if (iconsLoading.contains(id))
			{
				iconCache.put(id, scaled(image));
				repaint();
			}
		});
		if (goal.getRequirement() != null)
		{
			icons.loadImage(goal.getRequirement(), store);
		}
		else
		{
			icons.loadImage(goal.getIcon(), store);
		}
		return null;
	}

	@Nullable
	private static Object iconSourceOf(Goal goal)
	{
		return goal.getRequirement() != null ? goal.getRequirement() : goal.getIcon();
	}

	// Föremålsbilder är 36x32; de skalas en gång till dubbel ikonstorlek (skarpa vid inzoomning)
	// i stället för vid varje målning.
	private static Image scaled(BufferedImage image)
	{
		if (image.getWidth() == ICON_SIZE * 2 && image.getHeight() == ICON_SIZE * 2)
		{
			return image;
		}
		int size = ICON_SIZE * 2;
		BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		double ratio = Math.min((double) size / image.getWidth(), (double) size / image.getHeight());
		int w = (int) Math.round(image.getWidth() * ratio);
		int h = (int) Math.round(image.getHeight() * ratio);
		g.drawImage(image, (size - w) / 2, (size - h) / 2, w, h, null);
		g.dispose();
		return out;
	}

	private static double clamp(double value, double min, double max)
	{
		return Math.max(min, Math.min(max, value));
	}

	// För tester.
	double getScale()
	{
		return scale;
	}

	Set<String> getRelated()
	{
		return related;
	}

	void hoverForTest(@Nullable String id)
	{
		setHover(id);
	}

	List<MapLayout.Node> nodesForTest()
	{
		return layout == null ? Collections.emptyList() : layout.getNodes();
	}
}
