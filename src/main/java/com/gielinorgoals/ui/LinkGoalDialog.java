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

import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.Value;
import net.runelite.client.ui.ColorScheme;

/**
 * Dialog för "Link to another goal...": välj ett mål som det här målet också ska ligga
 * under. Mål som inte går att välja (det skulle bli en cykel, eller kopplingen finns
 * redan) visas gråade med förklaring, så att man ser varför.
 */
final class LinkGoalDialog
{
	@Value
	static class Option
	{
		Goal goal;
		// Null om målet går att välja, annars varför inte.
		@Nullable
		String blockedReason;

		boolean isSelectable()
		{
			return blockedReason == null;
		}
	}

	private LinkGoalDialog()
	{
	}

	/**
	 * @return id för den nya föräldern, eller null om användaren avbröt
	 */
	@Nullable
	static String show(Component parent, GoalTree tree, Goal goal)
	{
		List<Option> options = options(tree, goal);
		DefaultListModel<Option> model = new DefaultListModel<>();
		JList<Option> list = new JList<>(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setVisibleRowCount(12);
		list.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> jList, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
			{
				Option option = (Option) value;
				String text = option.getGoal().getDisplayTitle();
				if (!option.isSelectable())
				{
					text += " (" + option.getBlockedReason() + ")";
				}
				JLabel label = (JLabel) super.getListCellRendererComponent(jList, text, index,
					isSelected && option.isSelectable(), cellHasFocus);
				if (!option.isSelectable())
				{
					label.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
				}
				return label;
			}
		});
		// Gråade mål går inte att markera.
		list.addListSelectionListener(e ->
		{
			Option selected = list.getSelectedValue();
			if (selected != null && !selected.isSelectable())
			{
				list.clearSelection();
			}
		});

		JTextField filter = new JTextField();
		filter.putClientProperty("JTextField.placeholderText", "Filter goals");
		filter.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				fill(model, options, filter.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				fill(model, options, filter.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				fill(model, options, filter.getText());
			}
		});
		fill(model, options, "");

		JScrollPane scroll = new JScrollPane(list);
		scroll.setPreferredSize(new Dimension(300, 240));
		JPanel content = new JPanel(new BorderLayout(0, 6));
		content.add(new JLabel("<html>Also show \"" + escapeHtml(goal.getDisplayTitle())
			+ "\" under which goal?</html>"), BorderLayout.NORTH);
		JPanel center = new JPanel(new BorderLayout(0, 4));
		center.add(filter, BorderLayout.NORTH);
		center.add(scroll, BorderLayout.CENTER);
		content.add(center, BorderLayout.CENTER);

		while (true)
		{
			int choice = JOptionPane.showConfirmDialog(parent, content, "Link to another goal",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (choice != JOptionPane.OK_OPTION)
			{
				return null;
			}
			Option selected = list.getSelectedValue();
			if (selected != null && selected.isSelectable())
			{
				return selected.getGoal().getId();
			}
			JOptionPane.showMessageDialog(parent, "Pick a goal in the list.", "Link to another goal",
				JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * Alla mål utom målet självt, i visningsordning, med orsak om de inte går att välja.
	 */
	static List<Option> options(GoalTree tree, Goal goal)
	{
		List<Option> options = new ArrayList<>();
		for (Goal candidate : tree.flatten())
		{
			if (candidate.getId().equals(goal.getId()))
			{
				continue;
			}
			String reason = null;
			if (!tree.canLink(candidate.getId(), goal.getId()))
			{
				reason = candidate.getChildIds().contains(goal.getId())
					? "already linked"
					: "sub-goal of this goal";
			}
			options.add(new Option(candidate, reason));
		}
		return options;
	}

	private static void fill(DefaultListModel<Option> model, List<Option> options, String query)
	{
		String needle = query.trim().toLowerCase(Locale.ROOT);
		model.clear();
		for (Option option : options)
		{
			if (needle.isEmpty() || option.getGoal().getDisplayTitle().toLowerCase(Locale.ROOT).contains(needle))
			{
				model.addElement(option);
			}
		}
	}

	private static String escapeHtml(String text)
	{
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;");
	}
}
