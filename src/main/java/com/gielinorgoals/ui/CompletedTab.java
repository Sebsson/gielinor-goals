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
import com.gielinorgoals.GoalManager;
import com.gielinorgoals.model.CompletedLog;
import com.gielinorgoals.model.DryMeter;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalProgress;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * Completed-fliken: en logg över alla klara mål, grupperad per datum. Listan byggs
 * bara om när målen sparas (GoalManager-revisionen), när filter eller sortering
 * byts, när config ändras eller när dagen byts — inte när bara progressen ändras.
 */
class CompletedTab extends JPanel
{
	private static final int TEXT_WIDTH = 140;
	private static final int ICON_WIDTH = 24;
	private static final String APPROX_TOOLTIP = "Completed before tracking / found at login";

	private final GoalManager goalManager;
	private final GielinorGoalsConfig config;
	private final DialogServices services;
	private final Consumer<String> showInTree;

	private final JComboBox<CompletedLog.TypeFilter> typeBox = new JComboBox<>(CompletedLog.TypeFilter.values());
	private final JComboBox<String> sortBox = new JComboBox<>(new String[]{"Newest first", "Oldest first"});
	private final JCheckBox rootsOnlyBox = new JCheckBox("Only main goals");
	private final JPanel listPanel = new JPanel(new DynamicGridLayout(0, 1, 0, 2));
	private final JLabel statsLabel = new JLabel();

	// Vad listan senast byggdes från, för att slippa bygga om i onödan.
	private Object builtFrom;
	// RuneScape-typsnittet saknar många tecken; faller tillbaka på ~ om ≈ inte finns.
	private final String approxPrefix;

	CompletedTab(GoalManager goalManager, GielinorGoalsConfig config, DialogServices services,
		Consumer<String> showInTree)
	{
		this.goalManager = goalManager;
		this.config = config;
		this.services = services;
		this.showInTree = showInTree;
		this.approxPrefix = FontManager.getRunescapeSmallFont().canDisplay('≈') ? "≈ " : "~ ";

		setLayout(new BorderLayout(0, 6));
		setOpaque(false);

		statsLabel.setFont(FontManager.getRunescapeSmallFont());
		statsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JPanel top = new JPanel(new BorderLayout(0, 6));
		top.setOpaque(false);
		top.add(statsLabel, BorderLayout.NORTH);
		top.add(buildFilters(), BorderLayout.CENTER);

		add(top, BorderLayout.NORTH);
		add(buildList(), BorderLayout.CENTER);
	}

	private JPanel buildFilters()
	{
		typeBox.setToolTipText("Show only one kind of goal");
		typeBox.setMaximumRowCount(CompletedLog.TypeFilter.values().length);
		sortBox.setToolTipText("Sort by completion date");
		rootsOnlyBox.setOpaque(false);
		rootsOnlyBox.setFocusPainted(false);
		rootsOnlyBox.setToolTipText("Hide sub-goals");

		typeBox.addActionListener(e -> refresh());
		sortBox.addActionListener(e -> refresh());
		rootsOnlyBox.addActionListener(e -> refresh());

		JPanel filters = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		filters.setOpaque(false);
		filters.add(typeBox);
		filters.add(sortBox);
		filters.add(rootsOnlyBox);
		return filters;
	}

	private JScrollPane buildList()
	{
		listPanel.setOpaque(false);
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

	/**
	 * Bygger om listan om något den beror på har ändrats. Anropas på EDT.
	 */
	void refresh()
	{
		ZoneId zone = ZoneId.systemDefault();
		List<Object> key = List.of(goalManager.getRevision(), goalManager.isEditable(),
			typeBox.getSelectedIndex(), sortBox.getSelectedIndex(), rootsOnlyBox.isSelected(),
			config.showTimeToComplete(), LocalDate.now(zone), zone);
		if (Objects.equals(key, builtFrom))
		{
			return;
		}
		builtFrom = key;

		long now = System.currentTimeMillis();
		List<CompletedLog.Section> sections = CompletedLog.build(goalManager.getTree(),
			(CompletedLog.TypeFilter) typeBox.getSelectedItem(), rootsOnlyBox.isSelected(),
			sortBox.getSelectedIndex() == 0, now, zone);
		updateStats(CompletedLog.stats(goalManager.getTree(), now, zone), now, zone);

		listPanel.removeAll();
		if (sections.isEmpty())
		{
			boolean anyCompleted = goalManager.getTree().flatten().stream().anyMatch(Goal::isCompleted);
			listPanel.add(smallLabel(anyCompleted
				? "<html>No completed goals match the filter.</html>"
				: "<html>No completed goals yet. They show up here with the date they were done.</html>",
				ColorScheme.LIGHT_GRAY_COLOR));
		}
		for (CompletedLog.Section section : sections)
		{
			JLabel header = new JLabel(section.getHeader() + " (" + section.getEntries().size() + ")");
			header.setFont(FontManager.getRunescapeBoldFont());
			header.setForeground(ColorScheme.BRAND_ORANGE);
			header.setBorder(new EmptyBorder(6, 0, 2, 0));
			listPanel.add(header);
			for (CompletedLog.Entry entry : section.getEntries())
			{
				listPanel.add(buildRow(entry, zone));
			}
		}
		listPanel.revalidate();
		listPanel.repaint();
	}

	// "42 goals done · 7 this month · Latest: Fire cape (2 days ago)" och snittid om den finns.
	private void updateStats(CompletedLog.Stats stats, long now, ZoneId zone)
	{
		if (stats.getTotal() == 0)
		{
			statsLabel.setVisible(false);
			return;
		}

		StringBuilder text = new StringBuilder("<html>")
			.append(stats.getTotal()).append(stats.getTotal() == 1 ? " goal done" : " goals done")
			.append(" \u00b7 ").append(stats.getThisMonth()).append(" this month");
		Goal latest = stats.getLatest();
		if (latest != null)
		{
			text.append(" \u00b7 Latest: ").append(UiText.escapeHtml(latest.getDisplayTitle()))
				.append(" (").append(CompletedLog.daysAgo(latest.getCompletedAt(), now, zone)).append(")");
		}
		if (stats.getAverageMs() != null)
		{
			text.append("<br>Average time per goal: ").append(CompletedLog.formatDuration(stats.getAverageMs()));
		}
		statsLabel.setText(text.append("</html>").toString());
		statsLabel.setVisible(true);
	}

	private JPanel buildRow(CompletedLog.Entry entry, ZoneId zone)
	{
		Goal goal = entry.getGoal();
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new CompoundBorder(new MatteBorder(0, 3, 0, 0, ColorScheme.PROGRESS_COMPLETE_COLOR),
			new EmptyBorder(5, 4, 5, 4)));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(ICON_WIDTH, 16));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.TOP);
		services.getIcons().apply(goal, icon);

		JPanel text = new JPanel(new DynamicGridLayout(0, 1, 0, 1));
		text.setOpaque(false);
		JLabel name = new JLabel("<html><body style='width:" + TEXT_WIDTH + "px'>"
			+ UiText.escapeHtml(goal.getDisplayTitle()) + "</body></html>");
		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(Color.WHITE);
		text.add(name);

		if (entry.getParents() != null)
		{
			text.add(smallLabel("<html><body style='width:" + TEXT_WIDTH + "px'>in "
				+ UiText.escapeHtml(entry.getParents()) + "</body></html>", ColorScheme.MEDIUM_GRAY_COLOR));
		}

		JLabel date = smallLabel(dateText(goal, zone), ColorScheme.LIGHT_GRAY_COLOR);
		if (goal.isCompletedAtApprox())
		{
			date.setToolTipText(APPROX_TOOLTIP);
		}
		text.add(date);
		String obtained = DryMeter.completedText(goal);
		if (obtained != null)
		{
			text.add(smallLabel(obtained, ColorScheme.LIGHT_GRAY_COLOR));
		}

		row.add(icon, BorderLayout.WEST);
		row.add(text, BorderLayout.CENTER);

		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				maybeShowMenu(e, goal);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				maybeShowMenu(e, goal);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (row.getMousePosition(true) == null)
				{
					row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				}
			}
		};
		for (JComponent component : new JComponent[]{row, icon, text, name, date})
		{
			component.addMouseListener(mouse);
		}
		return row;
	}

	private String dateText(Goal goal, ZoneId zone)
	{
		if (goal.getCompletedAt() == null)
		{
			return "Date unknown";
		}
		String text = CompletedLog.formatDate(goal.getCompletedAt(), zone);
		if (goal.isCompletedAtApprox())
		{
			text = approxPrefix + text;
		}
		String took = config.showTimeToComplete() ? CompletedLog.timeToComplete(goal) : null;
		return took == null ? text : text + " · " + took;
	}

	private void maybeShowMenu(MouseEvent e, Goal goal)
	{
		if (!e.isPopupTrigger())
		{
			return;
		}

		JPopupMenu menu = new JPopupMenu();
		JMenuItem show = new JMenuItem("Show in tree");
		show.addActionListener(a -> showInTree.accept(goal.getId()));
		menu.add(show);
		String wikiUrl = WikiLinks.url(goal);
		if (wikiUrl != null)
		{
			JMenuItem wiki = new JMenuItem("Open on wiki");
			wiki.addActionListener(a -> net.runelite.client.util.LinkBrowser.browse(wikiUrl));
			menu.add(wiki);
		}

		// Ett uppfyllt automatiskt mål skulle bara bockas av igen direkt.
		GoalProgress progress = goalManager.getProgress(goal.getId());
		boolean lockedByGame = progress != null && progress.isComplete();
		JMenuItem undo = new JMenuItem(lockedByGame ? "Mark as not done (still met in game)" : "Mark as not done");
		undo.setEnabled(goalManager.isEditable() && !lockedByGame);
		undo.addActionListener(a -> goalManager.setCompleted(goal.getId(), false));
		menu.add(undo);

		Component source = e.getComponent();
		menu.show(source, e.getX(), e.getY());
	}

	private static JLabel smallLabel(String text, Color color)
	{
		JLabel label = new JLabel(text);
		Font font = FontManager.getRunescapeSmallFont();
		label.setFont(font);
		label.setForeground(color);
		return label;
	}
}
