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
import com.gielinorgoals.model.CompletedLog;
import com.gielinorgoals.model.DryMeter;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalEvaluator;
import com.gielinorgoals.model.GoalProgress;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.KillCountRequirement;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Panelen till höger i kartfönstret med allt om det valda målet: krav, progress,
 * föräldrar (klickbara), räknare, torrhetsmätare, anteckning och när det klarades.
 * Byggs om när ett mål väljs och när målen ändras. Används bara på EDT.
 */
final class GoalMapDetails extends JPanel
{
	static final int WIDTH = 270;
	private static final int TEXT_WIDTH = WIDTH - 40;
	private static final Color LINK = new Color(110, 170, 255);

	private final GoalManager goalManager;
	private final JPanel content = new JPanel();
	private final Consumer<String> showGoal;
	private final Consumer<Goal> edit;

	@Nullable
	private String goalId;
	// Texten som visas just nu; panelen byggs bara om när den ändras. Annars skulle en länk
	// bytas ut mitt i ett klick när kartan uppdateras varje tick (t.ex. av XP).
	@Nullable
	private String shownText;
	// Panelen som byggs; en ny för varje show() så att den kan jämföras med den som visas.
	private JPanel target;

	/**
	 * @param showGoal centrerar kartan på ett mål, t.ex. en förälder
	 * @param edit öppnar redigeringsdialogen
	 */
	GoalMapDetails(GoalManager goalManager, Consumer<String> showGoal, Consumer<Goal> edit)
	{
		this.goalManager = goalManager;
		this.showGoal = showGoal;
		this.edit = edit;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR));
		setPreferredSize(new Dimension(WIDTH, 0));

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		content.setBorder(new EmptyBorder(10, 12, 10, 12));
		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(content, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(top, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add(scroll, BorderLayout.CENTER);
		setVisible(false);
	}

	@Nullable
	String getGoalId()
	{
		return goalId;
	}

	/**
	 * Visar målet, eller döljer panelen om målet är null eller inte finns längre.
	 */
	void show(@Nullable String id)
	{
		GoalTree tree = goalManager.getTree();
		Goal goal = id == null ? null : tree.find(id);
		goalId = goal == null ? null : id;
		if (goal == null)
		{
			content.removeAll();
			shownText = null;
			setVisible(false);
			revalidate();
			return;
		}

		target = new JPanel();
		build(tree, goal);
		String text = id + "\n" + goalManager.isEditable() + "\n" + textOf(target);
		if (text.equals(shownText) && isVisible())
		{
			return;
		}
		shownText = text;
		content.removeAll();
		for (java.awt.Component component : target.getComponents())
		{
			content.add(component);
		}
		setVisible(true);
		revalidate();
		repaint();
	}

	private void build(GoalTree tree, Goal goal)
	{
		JLabel title = text(goal.getDisplayTitle(), FontManager.getRunescapeBoldFont(), Color.WHITE);
		target.add(title);
		target.add(text(goal.getRequirement() == null ? (goal.isAggregate() ? "Counter of its sub-goals" : "Manual goal")
			: goal.getRequirement().describe(), FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR));

		section("Progress");
		GoalProgress progress = goalManager.getProgress(goal.getId());
		if (goal.isAggregate())
		{
			GoalProgress counter = GoalEvaluator.aggregateProgress(tree, goal);
			line(counter.getLabel() + " sub-goals done");
		}
		if (progress != null && goal.getRequirement() != null)
		{
			for (String part : goal.getRequirement().describeProgress(progress).split("\n"))
			{
				line(part);
			}
		}
		else if (!goal.isAggregate())
		{
			line(goal.isCompleted() ? "Done" : "Not done yet");
		}

		if (goal.getRequirement() instanceof KillCountRequirement)
		{
			line(UiText.counterSource(goalManager.getCounters()
				.get(((KillCountRequirement) goal.getRequirement()).counterKey())));
		}

		if (DryMeter.sourceOf(goal) != null)
		{
			section("Drop");
			line("Drop rate " + goal.getDropSource().describe());
			long kc = goalManager.getCounters().value(goal.getDropSource().counterKey());
			addIfPresent(DryMeter.rowText(goal, kc));
			addIfPresent(DryMeter.chanceText(goal, kc));
			addIfPresent(DryMeter.completedText(goal));
			if (kc < 0 && !goal.isCompleted())
			{
				line("Kill count not known yet");
			}
		}

		List<Goal> parents = tree.getParents(goal.getId());
		if (!parents.isEmpty())
		{
			section(parents.size() == 1 ? "Part of" : "Part of " + parents.size() + " goals");
			for (Goal parent : parents)
			{
				target.add(link(parent.getDisplayTitle(), () -> showGoal.accept(parent.getId())));
			}
		}

		if (goal.getNote() != null && !goal.getNote().trim().isEmpty())
		{
			section("Note");
			target.add(text(goal.getNote(), FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR));
		}

		if (goal.isCompleted() && goal.getCompletedAt() != null)
		{
			section("Completed");
			String date = CompletedLog.formatDate(goal.getCompletedAt(), ZoneId.systemDefault());
			line((goal.isCompletedAtApprox() ? "About " : "") + date);
			String took = CompletedLog.timeToComplete(goal);
			if (took != null)
			{
				line(took);
			}
		}
		else if (goal.getCreatedAt() != null)
		{
			section("Added");
			line(CompletedLog.formatDate(goal.getCreatedAt(), ZoneId.systemDefault()));
		}

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttons.setOpaque(false);
		buttons.setAlignmentX(LEFT_ALIGNMENT);
		buttons.setBorder(new EmptyBorder(12, 0, 0, 0));
		String wiki = WikiLinks.url(goal);
		JButton wikiButton = new JButton("Open on wiki");
		wikiButton.setFocusPainted(false);
		wikiButton.setEnabled(wiki != null);
		wikiButton.addActionListener(e -> net.runelite.client.util.LinkBrowser.browse(wiki));
		JButton editButton = new JButton("Edit");
		editButton.setFocusPainted(false);
		editButton.setEnabled(goalManager.isEditable());
		editButton.addActionListener(e -> edit.accept(goal));
		buttons.add(wikiButton);
		buttons.add(javax.swing.Box.createHorizontalStrut(6));
		buttons.add(editButton);
		target.add(buttons);
	}

	private void section(String name)
	{
		JLabel label = text(name, FontManager.getRunescapeSmallFont(), ColorScheme.BRAND_ORANGE);
		label.setBorder(new EmptyBorder(10, 0, 2, 0));
		target.add(label);
	}

	private void line(String value)
	{
		target.add(text(value, FontManager.getRunescapeSmallFont(), Color.WHITE));
	}

	private void addIfPresent(@Nullable String value)
	{
		if (value != null)
		{
			line(value);
		}
	}

	private JComponent link(String value, Runnable action)
	{
		JLabel label = text(value, FontManager.getRunescapeSmallFont(), LINK);
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.setToolTipText("Show on the map");
		label.addMouseListener(new MouseAdapter()
		{
			// Vid nedtryckning, inte vid klick: ett klick räknas inte om musen rör sig lite
			// mellan tryck och släpp, eller om panelen hinner uppdateras emellan.
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (javax.swing.SwingUtilities.isLeftMouseButton(e))
				{
					action.run();
				}
			}
		});
		return label;
	}

	// Radbryts inom panelens bredd; användarens text tolkas inte som HTML.
	private static JLabel text(String value, java.awt.Font font, Color color)
	{
		JLabel label = new JLabel("<html><body style='width:" + TEXT_WIDTH + "px'>"
			+ UiText.escapeHtml(value).replace("\n", "<br>") + "</body></html>");
		label.setFont(font);
		label.setForeground(color);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	// För tester: det som visas just nu.
	java.awt.Component[] componentsForTest()
	{
		return content.getComponents();
	}

	// För tester: texten i panelen.
	String textForTest()
	{
		return textOf(content);
	}

	private static String textOf(JPanel panel)
	{
		StringBuilder all = new StringBuilder();
		for (java.awt.Component component : panel.getComponents())
		{
			if (component instanceof JLabel)
			{
				all.append(((JLabel) component).getText().replaceAll("<[^>]+>", "")).append('\n');
			}
		}
		return all.toString();
	}
}
