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
import com.gielinorgoals.ProfileGuard;
import com.gielinorgoals.GielinorGoalsConfig;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.persistence.LoadResult;
import com.gielinorgoals.template.TemplateCatalog;
import com.gielinorgoals.template.TemplateInserter;
import com.gielinorgoals.template.TemplateNode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ProgressBar;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/**
 * Sidopanelen med två flikar: Active (inmatning och målträdet) och Completed (loggen
 * över klara mål). Trädet byggs om vid varje ändring, vilket är enkelt och räcker gott
 * för några hundra mål; Completed-fliken bygger bara om när målen faktiskt ändrats.
 */
public class GielinorGoalsPanel extends PluginPanel
{
	private final GoalManager goalManager;
	private final GielinorGoalsConfig config;
	private final ConfigManager configManager;
	private final DialogServices services;

	private final JTextField newGoalField = new JTextField();
	private final JButton addButton = new JButton("Add goal...");
	private final JButton templateButton = new JButton("Add from template...");
	private final TemplateCatalog templateCatalog;
	private final JLabel summaryLabel = new JLabel();
	private final ProgressBar summaryBar = new ProgressBar();
	private final JCheckBox hideCompletedBox = new JCheckBox("Hide completed");
	private final JButton collapseAllButton = new JButton("Collapse all");
	private final JButton moreButton = new JButton("...");
	private final JButton mapButton = new JButton("Map");
	// Sätts av pluginet, som äger kartfönstret.
	private Runnable openMap = () ->
	{
	};
	// Vad listan senast byggdes för. Är det oförändrat har bara spelläget ändrats (t.ex. XP),
	// och då uppdateras raderna i stället för att byggas om.
	@Nullable
	private List<Object> builtFor;
	private final JLabel statusLabel = new JLabel();
	private final JLabel bankLabel = new JLabel();
	private final JPanel listPanel = new JPanel(new DynamicGridLayout(0, 1, 0, 2))
	{
		@Override
		protected void paintChildren(java.awt.Graphics g)
		{
			super.paintChildren(g);
			// Linjen för var en dragen rad hamnar ritas ovanpå raderna.
			if (rowDrag != null)
			{
				rowDrag.paint(g);
			}
		}
	};
	// Dra och släpp i listan; skapas i konstruktorn när listan finns.
	private RowDragController rowDrag;
	// En ombyggnad som kom medan en rad drogs; görs när dragningen är slut.
	private boolean rebuildAfterDrag;
	private final CompletedTab completedTab;
	private final MaterialTabGroup tabGroup;
	private final MaterialTab activeTab;
	private final MaterialTab completedTabButton;

	// Målet som visas från Completed-fliken; markeras i trädet tills man byter flik.
	@Nullable
	private String highlightId;
	private boolean scrollToHighlight;

	@Inject
	GielinorGoalsPanel(GoalManager goalManager, GielinorGoalsConfig config, ConfigManager configManager,
		DialogServices services, TemplateCatalog templateCatalog)
	{
		super(false);
		this.templateCatalog = templateCatalog;
		this.goalManager = goalManager;
		this.config = config;
		this.configManager = configManager;
		this.services = services;

		setLayout(new BorderLayout(0, 6));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel activeContent = new JPanel(new BorderLayout(0, 6));
		activeContent.setOpaque(false);
		activeContent.add(buildActiveControls(), BorderLayout.NORTH);
		activeContent.add(buildList(), BorderLayout.CENTER);
		completedTab = new CompletedTab(goalManager, config, services, this::showInTree);

		JPanel display = new JPanel(new BorderLayout());
		display.setOpaque(false);
		tabGroup = new MaterialTabGroup(display);
		activeTab = new MaterialTab("Active", tabGroup, activeContent);
		completedTabButton = new MaterialTab("Completed", tabGroup, completedTab);
		completedTabButton.setOnSelectEvent(() ->
		{
			highlightId = null;
			completedTab.refresh();
			return true;
		});
		tabGroup.addTab(activeTab);
		tabGroup.addTab(completedTabButton);
		tabGroup.select(activeTab);

		JPanel top = new JPanel(new BorderLayout(0, 6));
		top.setOpaque(false);
		top.add(buildHeader(), BorderLayout.NORTH);
		top.add(tabGroup, BorderLayout.SOUTH);

		add(top, BorderLayout.NORTH);
		add(display, BorderLayout.CENTER);

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
		templateButton.setFocusPainted(false);
		templateButton.setToolTipText("Quest cape, diary cape, base levels, Barrows gloves and more");
		templateButton.addActionListener(e -> openTemplateDialog());

		summaryLabel.setFont(FontManager.getRunescapeSmallFont());
		summaryLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		summaryBar.setLeftLabel("");
		summaryBar.setRightLabel("");
		summaryBar.setCenterLabel("");
		summaryBar.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		summaryBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		summaryBar.setPreferredSize(new Dimension(0, 8));

		mapButton.setFocusPainted(false);
		mapButton.setToolTipText("Open a map of how your goals connect");
		mapButton.addActionListener(e -> openMap.run());

		moreButton.setFocusPainted(false);
		moreButton.setToolTipText("Import and export goals");
		moreButton.addActionListener(e -> showMoreMenu());
		JPanel titleButtons = new JPanel(new BorderLayout(4, 0));
		titleButtons.setOpaque(false);
		titleButtons.add(moreButton, BorderLayout.WEST);
		titleButtons.add(mapButton, BorderLayout.EAST);

		JPanel titleRow = new JPanel(new BorderLayout());
		titleRow.setOpaque(false);
		titleRow.add(title, BorderLayout.WEST);
		titleRow.add(titleButtons, BorderLayout.EAST);

		// Summeringen har en egen rad; titelraden är för smal för både den och kartknappen.
		JPanel summaryRow = new JPanel(new BorderLayout());
		summaryRow.setOpaque(false);
		summaryRow.add(summaryLabel, BorderLayout.EAST);

		JPanel header = new JPanel(new DynamicGridLayout(0, 1, 0, 5));
		header.setOpaque(false);
		header.add(titleRow);
		header.add(summaryRow);
		header.add(summaryBar);
		return header;
	}

	// Panelmenyn: import och export av mål.
	private void showMoreMenu()
	{
		boolean editable = goalManager.isEditable();
		javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
		javax.swing.JMenuItem importItem = new javax.swing.JMenuItem("Import...");
		importItem.setEnabled(editable);
		importItem.addActionListener(e -> ImportDialog.show(this, goalManager, services));
		javax.swing.JMenuItem exportItem = new javax.swing.JMenuItem("Export all goals...");
		exportItem.setEnabled(editable && !goalManager.getTree().getRoots().isEmpty());
		exportItem.addActionListener(e -> ShareDialog.show(this, "Export all goals", "all your goals",
			services.getShareCodec().encode(com.gielinorgoals.template.TemplateExport.exportAll(goalManager.getTree()),
				com.gielinorgoals.template.ShareCodec.Kind.GOALS), "all goals"));
		menu.add(importItem);
		menu.add(exportItem);
		menu.show(moreButton, 0, moreButton.getHeight());
	}

	private JPanel buildActiveControls()
	{
		hideCompletedBox.setOpaque(false);
		hideCompletedBox.setFocusPainted(false);
		hideCompletedBox.addActionListener(e -> configManager.setConfiguration(
			GielinorGoalsConfig.GROUP, GielinorGoalsConfig.HIDE_COMPLETED_KEY, hideCompletedBox.isSelected()));

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		bankLabel.setFont(FontManager.getRunescapeSmallFont());
		bankLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel controls = new JPanel(new DynamicGridLayout(0, 1, 0, 5));
		controls.setOpaque(false);
		controls.add(addButton);
		controls.add(templateButton);
		controls.add(newGoalField);

		// "Collapse all" på samma rad som "Hide completed", så att den inte tar egen plats.
		collapseAllButton.setFocusPainted(false);
		collapseAllButton.setToolTipText("Collapse every goal with sub-goals");
		collapseAllButton.addActionListener(e -> goalManager.collapseAll());
		JPanel viewRow = new JPanel(new BorderLayout(6, 0));
		viewRow.setOpaque(false);
		viewRow.add(hideCompletedBox, BorderLayout.CENTER);
		viewRow.add(collapseAllButton, BorderLayout.EAST);
		controls.add(viewRow);
		controls.add(statusLabel);
		controls.add(bankLabel);
		return controls;
	}

	/**
	 * Byter till Active-fliken och markerar målet, med förfäderna utfällda.
	 */
	private void showInTree(String goalId)
	{
		if (config.hideCompleted())
		{
			JOptionPane.showMessageDialog(this,
				"Completed goals are hidden in the tree. Untick \"Hide completed\" to see this goal there.",
				"Show in tree", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		tabGroup.select(activeTab);
		highlightId = goalId;
		scrollToHighlight = true;
		// reveal() bygger om via lyssnaren bara om något fälldes ut, därför en egen rebuild.
		goalManager.reveal(goalId);
		rebuild();
	}

	private JScrollPane buildList()
	{
		listPanel.setOpaque(false);
		rowDrag = new RowDragController(listPanel, goalManager, this::afterDrag);

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

	// Ombyggnaden som väntade medan en rad drogs.
	private void afterDrag()
	{
		if (rebuildAfterDrag)
		{
			rebuildAfterDrag = false;
			rebuild();
		}
	}

	private void addRootGoal()
	{
		goalManager.addGoal(null, newGoalField.getText(), null, null);
		newGoalField.setText("");
		newGoalField.requestFocusInWindow();
	}

	private void openNewGoalDialog()
	{
		ProfileGuard guard = ProfileGuard.of(goalManager);
		// Text som redan skrivits i fältet följer med som titel.
		GoalEditDialog.Result result = GoalEditDialog.show(this, services, "New goal", null,
			newGoalField.getText().trim(), null, null, false);
		if (result != null && GoalActions.stillSameProfile(this, guard))
		{
			GoalEditDialog.addGoal(goalManager, services, null, result);
			newGoalField.setText("");
		}
	}

	private void openTemplateDialog()
	{
		ProfileGuard guard = ProfileGuard.of(goalManager);
		TemplateNode template = TemplateDialog.show(this, templateCatalog::getTemplates, services.getCustomTemplates(),
			services.getShareCodec(), goalManager.getTree(),
			services.currentGameData(), services.getIcons(), services.getQuestRequirements(),
			services.getAccountStatus().isIronman(), services.getTemplateBuilder(), services.getCompletionLookup());
		// Templaten planerades mot målen i den profil som var laddad när dialogen öppnades.
		if (template == null || !GoalActions.stillSameProfile(this, guard))
		{
			return;
		}
		TemplateInserter.Result result = goalManager.insertTemplate(template);
		if (result != null)
		{
			// Den nya roten markeras och scrollas fram i trädet.
			showInTree(result.getRoot().getId());
		}
	}

	public void setOpenMap(Runnable openMap)
	{
		this.openMap = openMap;
	}

	/**
	 * Bygger om listan från modellen, eller uppdaterar bara progressen om inget annat har
	 * ändrats. Anropas på EDT.
	 */
	public void rebuild()
	{
		// Raderna byts inte ut mitt i en dragning; ombyggnaden görs när den är slut.
		if (rowDrag != null && rowDrag.isActive())
		{
			rebuildAfterDrag = true;
			return;
		}
		boolean editable = goalManager.isEditable();
		newGoalField.setEnabled(editable);
		addButton.setEnabled(editable);
		templateButton.setEnabled(editable);
		collapseAllButton.setEnabled(editable);
		hideCompletedBox.setSelected(config.hideCompleted());

		GoalTree tree = goalManager.getTree();
		List<Object> key = Arrays.asList(goalManager.getRevision(), goalManager.getProfileKey(), editable,
			config.hideCompleted(), highlightId, tree);
		if (key.equals(builtFor) && !scrollToHighlight && updateRows())
		{
			updateStatus(tree);
			updateBankStatus(tree);
			completedTab.refresh();
			return;
		}
		builtFor = key;

		listPanel.removeAll();
		Map<String, List<Goal>> parents = parentsOf(tree);
		GoalRow highlighted = addRows(tree, tree.getRoots(), null, null, null, 0, editable, parents);

		updateStatus(tree);
		updateSummary(tree);
		updateBankStatus(tree);
		listPanel.revalidate();
		listPanel.repaint();
		completedTab.refresh();

		if (scrollToHighlight && highlighted != null)
		{
			scrollToHighlight = false;
			// Efter layouten, när raden har fått sin plats.
			SwingUtilities.invokeLater(() -> listPanel.scrollRectToVisible(highlighted.getBounds()));
		}
	}

	/**
	 * Ett delat delmål visas under varje förälder; raden får veta vilken förälder (och
	 * förälderns förälder) den visas under, så att flyttningar bara gäller den kopplingen.
	 *
	 * @return första raden för det markerade målet, om den lades till
	 */
	// Bara progressen på befintliga rader; false om någon rad behöver byggas om.
	private boolean updateRows()
	{
		for (Component component : listPanel.getComponents())
		{
			if (component instanceof GoalRow)
			{
				GoalRow row = (GoalRow) component;
				if (!row.updateProgress(goalManager.getProgress(row.getGoalId())))
				{
					return false;
				}
			}
		}
		return true;
	}

	// Alla mål → deras föräldrar i lagringsordning, en genomgång i stället för en per rad.
	private static Map<String, List<Goal>> parentsOf(GoalTree tree)
	{
		Map<String, List<Goal>> parents = new HashMap<>();
		for (Goal goal : tree.flatten())
		{
			for (String childId : goal.getChildIds())
			{
				parents.computeIfAbsent(childId, id -> new ArrayList<>()).add(goal);
			}
		}
		return parents;
	}

	@Nullable
	private GoalRow addRows(GoalTree tree, List<Goal> goals, @Nullable String parentId,
		@Nullable String grandparentId, @Nullable GoalRow parentRow, int depth, boolean editable,
		Map<String, List<Goal>> parents)
	{
		GoalRow highlighted = null;
		for (Goal goal : goals)
		{
			if (config.hideCompleted() && goal.isCompleted())
			{
				continue;
			}

			boolean isHighlighted = goal.getId().equals(highlightId);
			GoalRow row = new GoalRow(goal, parentId, grandparentId, parentRow, goalManager.getProgress(goal.getId()),
				depth, tree, goalManager, services, editable, isHighlighted && highlighted == null,
				parents.getOrDefault(goal.getId(), List.of()), rowDrag);
			listPanel.add(row);
			if (isHighlighted && highlighted == null)
			{
				highlighted = row;
			}
			if (goal.isExpanded())
			{
				GoalRow below = addRows(tree, tree.getChildren(goal), goal.getId(), parentId, row, depth + 1,
					editable, parents);
				highlighted = highlighted != null ? highlighted : below;
			}
		}
		return highlighted;
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
