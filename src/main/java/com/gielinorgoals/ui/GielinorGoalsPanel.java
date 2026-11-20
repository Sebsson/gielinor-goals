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
import com.gielinorgoals.GielinorGoalsConfig;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.persistence.LoadResult;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ProgressBar;

/**
 * Sidopanelen: inmatning överst och målträdet under. Hela listan byggs om vid varje
 * ändring, vilket är enkelt och räcker gott för några hundra mål.
 */
public class GielinorGoalsPanel extends PluginPanel
{
	private final GoalManager goalManager;
	private final GielinorGoalsConfig config;
	private final ConfigManager configManager;
	private final DialogServices services;

	private final JTextField newGoalField = new JTextField();
	private final JButton addButton = new JButton("Add goal...");
	private final JLabel summaryLabel = new JLabel();
	private final ProgressBar summaryBar = new ProgressBar();
	private final JCheckBox hideCompletedBox = new JCheckBox("Hide completed");
	private final JLabel statusLabel = new JLabel();
	private final JLabel bankLabel = new JLabel();
	private final JPanel listPanel = new JPanel(new DynamicGridLayout(0, 1, 0, 2));

	@Inject
	GielinorGoalsPanel(GoalManager goalManager, GielinorGoalsConfig config, ConfigManager configManager,
		DialogServices services)
	{
		super(false);
		this.goalManager = goalManager;
		this.config = config;
		this.configManager = configManager;
		this.services = services;

		setLayout(new BorderLayout(0, 6));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(buildHeader(), BorderLayout.NORTH);
		add(buildList(), BorderLayout.CENTER);

		rebuild();
	}

	private JPanel buildHeader()
	{
		JLabel title = new JLabel("Gielinor Goals");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);

		// FlatLaf, som RuneLite använder, visar platshållartext med den här egenskapen.
		newGoalField.putClientProperty("JTextField.placeholderText", "Quick add, press Enter");
		newGoalField.setToolTipText("Type a goal and press Enter to add it as a manual goal");
		newGoalField.addActionListener(e -> addRootGoal());
		addButton.setFocusPainted(false);
		addButton.setToolTipText("Add a skill, item, quest, diary or combat achievement goal");
		addButton.addActionListener(e -> openNewGoalDialog());

		summaryLabel.setFont(FontManager.getRunescapeSmallFont());
		summaryLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		summaryBar.setLeftLabel("");
		summaryBar.setRightLabel("");
		summaryBar.setCenterLabel("");
		summaryBar.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		summaryBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		summaryBar.setPreferredSize(new Dimension(0, 8));

		JPanel titleRow = new JPanel(new BorderLayout());
		titleRow.setOpaque(false);
		titleRow.add(title, BorderLayout.WEST);
		titleRow.add(summaryLabel, BorderLayout.EAST);

		hideCompletedBox.setOpaque(false);
		hideCompletedBox.setFocusPainted(false);
		hideCompletedBox.addActionListener(e -> configManager.setConfiguration(
			GielinorGoalsConfig.GROUP, GielinorGoalsConfig.HIDE_COMPLETED_KEY, hideCompletedBox.isSelected()));

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		bankLabel.setFont(FontManager.getRunescapeSmallFont());
		bankLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel header = new JPanel(new DynamicGridLayout(0, 1, 0, 5));
		header.setOpaque(false);
		header.add(titleRow);
		header.add(summaryBar);
		header.add(addButton);
		header.add(newGoalField);
		header.add(hideCompletedBox);
		header.add(statusLabel);
		header.add(bankLabel);
		return header;
	}

	private JScrollPane buildList()
	{
		listPanel.setOpaque(false);

		// Wrappern håller raderna uppe i panelen i stället för att sträcka ut dem.
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(listPanel, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(wrapper);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		return scrollPane;
	}

	private void addRootGoal()
	{
		goalManager.addGoal(null, newGoalField.getText(), null, null);
		newGoalField.setText("");
		newGoalField.requestFocusInWindow();
	}

	private void openNewGoalDialog()
	{
		// Text som redan skrivits i fältet följer med som titel.
		GoalEditDialog.Result result = GoalEditDialog.show(this, services, "New goal", null,
			newGoalField.getText().trim(), null, null);
		if (result != null)
		{
			goalManager.addGoal(null, result.getTitle(), result.getNote(), result.getRequirement());
			newGoalField.setText("");
		}
	}

	/**
	 * Bygger om hela listan från modellen. Anropas på EDT.
	 */
	public void rebuild()
	{
		boolean editable = goalManager.isEditable();
		newGoalField.setEnabled(editable);
		addButton.setEnabled(editable);
		hideCompletedBox.setSelected(config.hideCompleted());

		listPanel.removeAll();
		GoalTree tree = goalManager.getTree();
		addRows(tree, tree.getRoots(), 0, editable);

		updateStatus(tree);
		updateSummary(tree);
		updateBankStatus(tree);
		listPanel.revalidate();
		listPanel.repaint();
	}

	private void addRows(GoalTree tree, List<Goal> goals, int depth, boolean editable)
	{
		for (Goal goal : goals)
		{
			if (config.hideCompleted() && goal.isCompleted())
			{
				continue;
			}

			listPanel.add(new GoalRow(goal, goalManager.getProgress(goal.getId()), depth, tree, goalManager,
				services, editable));
			if (goal.isExpanded())
			{
				addRows(tree, goal.getChildren(), depth + 1, editable);
			}
		}
	}

	private void updateSummary(GoalTree tree)
	{
		List<Goal> all = tree.flatten();
		int done = 0;
		for (Goal goal : all)
		{
			if (goal.isCompleted())
			{
				done++;
			}
		}

		boolean visible = !all.isEmpty();
		summaryLabel.setVisible(visible);
		summaryBar.setVisible(visible);
		summaryLabel.setText(done + " / " + all.size() + " done");
		summaryBar.setMaximumValue(Math.max(1, all.size()));
		summaryBar.setValue(done);
	}

	// Visas bara när det finns föremålsmål, eftersom banken då påverkar räkningen.
	private void updateBankStatus(GoalTree tree)
	{
		boolean hasItemGoals = false;
		for (Goal goal : tree.flatten())
		{
			if (goal.getRequirement() instanceof ItemRequirement)
			{
				hasItemGoals = true;
				break;
			}
		}

		if (!hasItemGoals || !goalManager.hasGameData())
		{
			bankLabel.setVisible(false);
			return;
		}

		Long seenAt = goalManager.getBankSeenAt();
		String text = seenAt == null
			? "Open your bank once so banked items are counted."
			: "Bank last seen " + formatAge(System.currentTimeMillis() - seenAt) + ".";
		bankLabel.setText("<html>" + text + "</html>");
		bankLabel.setVisible(true);
	}

	static String formatAge(long millis)
	{
		long minutes = Math.max(0, millis) / 60_000;
		if (minutes < 1)
		{
			return "just now";
		}
		if (minutes < 60)
		{
			return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
		}
		long hours = minutes / 60;
		if (hours < 24)
		{
			return hours + (hours == 1 ? " hour ago" : " hours ago");
		}
		long days = hours / 24;
		return days + (days == 1 ? " day ago" : " days ago");
	}

	private void updateStatus(GoalTree tree)
	{
		String text;
		if (!goalManager.hasProfile())
		{
			text = "Log in to see the goals for your account.";
		}
		else if (goalManager.getLoadStatus() == LoadResult.Status.NEWER_VERSION)
		{
			text = "These goals were saved by a newer version of Gielinor Goals. Update the plugin to edit them.";
		}
		else if (goalManager.getLoadStatus() == LoadResult.Status.CORRUPT)
		{
			text = "Saved goals could not be read. A backup of the old data was kept.";
		}
		else if (tree.isEmpty())
		{
			text = "No goals yet. Use \"Add goal...\" for skills, items, quests, diaries and combat achievements, or quick add a note-style goal.";
		}
		else
		{
			text = "Tip: right-click a goal to add sub-goals, edit or reorder.";
		}

		// HTML gör att texten radbryts i den smala panelen.
		statusLabel.setText("<html>" + text + "</html>");
	}
}
