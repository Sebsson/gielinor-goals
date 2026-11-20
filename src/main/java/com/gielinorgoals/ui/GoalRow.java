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
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalProgress;
import com.gielinorgoals.model.GoalTree;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.annotation.Nullable;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.ProgressBar;

/**
 * En rad i mållistan: expandera, bocka av, titel och en meny med övriga åtgärder.
 */
class GoalRow extends JPanel
{
	private static final int INDENT_PX = 12;
	// Ungefärlig textbredd i px på rotnivå; minskar med djupet.
	private static final int BASE_TEXT_WIDTH = 120;
	private static final int MIN_TEXT_WIDTH = 50;
	// Upplösning för progressbaren (promille).
	private static final int PROGRESS_STEPS = 1000;
	// Plats för typikonen (föremål är 36 px breda, övriga 16 px).
	private static final int ICON_WIDTH = 24;

	private final Goal goal;
	@Nullable
	private final GoalProgress progress;
	private final GoalTree tree;
	private final GoalManager goalManager;
	private final DialogServices services;
	private final boolean editable;

	GoalRow(Goal goal, @Nullable GoalProgress progress, int depth, GoalTree tree, GoalManager goalManager,
		DialogServices services, boolean editable)
	{
		this.goal = goal;
		this.progress = progress;
		this.tree = tree;
		this.goalManager = goalManager;
		this.services = services;
		this.editable = editable;

		setLayout(new BorderLayout(4, 0));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		// Indrag för delmål, sedan en färgad kant som visar status, sedan luft runt innehållet.
		setBorder(new CompoundBorder(
			new EmptyBorder(0, depth * INDENT_PX, 0, 0),
			new CompoundBorder(new MatteBorder(0, 3, 0, 0, statusColor()), new EmptyBorder(5, 4, 5, 4))));

		JPanel left = new JPanel(new BorderLayout(2, 0));
		left.setOpaque(false);
		left.add(buildExpandToggle(), BorderLayout.WEST);
		left.add(buildCheckBox(), BorderLayout.CENTER);

		JPanel center = new JPanel(new BorderLayout(0, 3));
		center.setOpaque(false);
		center.add(buildTitle(depth), BorderLayout.CENTER);
		if (progress != null && !goal.isCompleted())
		{
			center.add(buildProgressBar(progress), BorderLayout.SOUTH);
		}

		add(left, BorderLayout.WEST);
		add(center, BorderLayout.CENTER);
		add(buildMenuButton(), BorderLayout.EAST);

		MouseAdapter rowMouse = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				maybeShowMenu(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				maybeShowMenu(e);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				// Musen kan ha gått in i en av radens egna komponenter.
				if (getMousePosition(true) == null)
				{
					setBackground(ColorScheme.DARKER_GRAY_COLOR);
				}
			}
		};
		addMouseListener(rowMouse);
		center.addMouseListener(rowMouse);
	}

	// Grön för klara, orange för automatiska mål på väg, grå för manuella.
	private Color statusColor()
	{
		if (goal.isCompleted())
		{
			return ColorScheme.PROGRESS_COMPLETE_COLOR;
		}
		return goal.getRequirement() != null ? ColorScheme.BRAND_ORANGE : ColorScheme.MEDIUM_GRAY_COLOR;
	}

	private JLabel buildExpandToggle()
	{
		// RuneScape-typsnittet saknar pilsymboler, därför vanliga tecken.
		JLabel toggle = new JLabel(goal.hasChildren() ? (goal.isExpanded() ? "-" : "+") : "");
		toggle.setPreferredSize(new Dimension(8, 16));
		toggle.setHorizontalAlignment(SwingConstants.CENTER);
		toggle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		if (goal.hasChildren())
		{
			toggle.setToolTipText(goal.isExpanded() ? "Collapse" : "Expand");
			toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			toggle.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (SwingUtilities.isLeftMouseButton(e))
					{
						goalManager.toggleExpanded(goal.getId());
					}
				}
			});
		}
		return toggle;
	}

	private JCheckBox buildCheckBox()
	{
		JCheckBox checkBox = new JCheckBox();
		checkBox.setOpaque(false);
		checkBox.setFocusPainted(false);
		checkBox.setSelected(goal.isCompleted());

		// Ett uppfyllt automatiskt mål skulle bara bockas av igen, så rutan låses.
		boolean lockedByGame = progress != null && progress.isComplete();
		checkBox.setEnabled(editable && !lockedByGame);
		if (lockedByGame)
		{
			checkBox.setToolTipText("Completed automatically");
		}
		else
		{
			checkBox.setToolTipText(goal.isCompleted() ? "Mark as not done" : "Mark as done");
		}
		checkBox.addActionListener(e -> goalManager.setCompleted(goal.getId(), checkBox.isSelected()));
		return checkBox;
	}

	private static ProgressBar buildProgressBar(GoalProgress progress)
	{
		ProgressBar bar = new ProgressBar();
		bar.setMaximumValue(PROGRESS_STEPS);
		bar.setValue((int) Math.round(progress.fraction() * PROGRESS_STEPS));
		bar.setCenterLabel(progress.getLabel());
		bar.setLeftLabel("");
		bar.setRightLabel("");
		bar.setForeground(progress.isComplete()
			? ColorScheme.PROGRESS_COMPLETE_COLOR
			: ColorScheme.PROGRESS_INPROGRESS_COLOR);
		bar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bar.setPreferredSize(new Dimension(0, 12));
		return bar;
	}

	private JLabel buildTitle(int depth)
	{
		int width = Math.max(MIN_TEXT_WIDTH, BASE_TEXT_WIDTH - depth * INDENT_PX);
		String text = escapeHtml(goal.getDisplayTitle());
		if (goal.hasChildren())
		{
			text += " <font color='#a0a0a0'>(" + countCompletedChildren() + "/" + goal.getChildren().size() + ")</font>";
		}
		if (goal.isCompleted())
		{
			text = "<strike>" + text + "</strike>";
		}

		if (goal.getRequirement() != null)
		{
			// Ikonen tar plats, så texten får krympa lika mycket.
			width = Math.max(MIN_TEXT_WIDTH, width - ICON_WIDTH);
		}
		JLabel title = new JLabel("<html><body style='width:" + width + "px'>" + text + "</body></html>");
		title.setIconTextGap(4);
		services.getIcons().apply(goal.getRequirement(), title);
		title.setFont(FontManager.getRunescapeSmallFont());
		title.setForeground(goal.isCompleted() ? ColorScheme.MEDIUM_GRAY_COLOR : Color.WHITE);
		if (goal.getRequirement() instanceof DiaryRequirement && !goal.hasChildren() && !goal.isCompleted())
		{
			title.setToolTipText("Open the " + ((DiaryRequirement) goal.getRequirement()).getArea().getDisplayName()
				+ " diary journal in game to add its tasks as sub-goals.");
		}
		if (goal.getNote() != null)
		{
			title.setToolTipText("<html>" + escapeHtml(goal.getNote()).replace("\n", "<br>") + "</html>");
		}

		title.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (editable && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
				{
					edit();
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				maybeShowMenu(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				maybeShowMenu(e);
			}
		});
		return title;
	}

	private JLabel buildMenuButton()
	{
		JLabel button = new JLabel("...");
		button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		button.setToolTipText("More options");
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setVisible(editable);
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				buildMenu().show(button, 0, button.getHeight());
			}
		});
		return button;
	}

	private void maybeShowMenu(MouseEvent e)
	{
		if (editable && e.isPopupTrigger())
		{
			buildMenu().show(e.getComponent(), e.getX(), e.getY());
		}
	}

	private JPopupMenu buildMenu()
	{
		String id = goal.getId();
		JPopupMenu menu = new JPopupMenu();
		menu.add(item("Add sub-goal", true, this::addSubGoal));
		menu.add(item("Edit", true, this::edit));
		menu.addSeparator();
		menu.add(item("Move up", tree.canMoveUp(id), () -> goalManager.moveUp(id)));
		menu.add(item("Move down", tree.canMoveDown(id), () -> goalManager.moveDown(id)));
		menu.add(item("Make sub-goal of goal above", tree.canIndent(id), () -> goalManager.indent(id)));
		menu.add(item("Move out one level", tree.canOutdent(id), () -> goalManager.outdent(id)));
		menu.addSeparator();
		menu.add(item("Delete", true, this::delete));
		return menu;
	}

	private static JMenuItem item(String text, boolean enabled, Runnable action)
	{
		JMenuItem item = new JMenuItem(text);
		item.setEnabled(enabled);
		item.addActionListener(e -> action.run());
		return item;
	}

	private void addSubGoal()
	{
		GoalEditDialog.Result result = GoalEditDialog.show(this, services, "Add sub-goal", null, "", null, null);
		if (result != null)
		{
			goalManager.addGoal(goal.getId(), result.getTitle(), result.getNote(), result.getRequirement());
		}
	}

	private void edit()
	{
		GoalEditDialog.Result result = GoalEditDialog.show(this, services, "Edit goal", goal.getId(), goal.getTitle(), goal.getNote(),
			goal.getRequirement());
		if (result != null)
		{
			goalManager.editGoal(goal.getId(), result.getTitle(), result.getNote(), result.getRequirement());
		}
	}

	private void delete()
	{
		// Bekräfta bara när delmål försvinner också; ett enskilt mål är lätt att skapa igen.
		if (goal.hasChildren())
		{
			int choice = JOptionPane.showConfirmDialog(this,
				"Delete \"" + goal.getDisplayTitle() + "\" and its " + goal.getChildren().size() + " sub-goal(s)?",
				"Delete goal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (choice != JOptionPane.YES_OPTION)
			{
				return;
			}
		}
		goalManager.removeGoal(goal.getId());
	}

	// Användarens text får inte tolkas som HTML i JLabel.
	private static String escapeHtml(String text)
	{
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;");
	}

	private int countCompletedChildren()
	{
		int count = 0;
		for (Goal child : goal.getChildren())
		{
			if (child.isCompleted())
			{
				count++;
			}
		}
		return count;
	}
}
