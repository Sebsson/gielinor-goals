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
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.template.CustomTemplates;
import com.gielinorgoals.template.GoalTemplate;
import com.gielinorgoals.template.TemplateExport;
import com.gielinorgoals.template.TemplateNode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * "Save as template...": sparar ett mål och hela delgrafen under det som en egen template.
 * Kommer målet från en egen template kan den skrivas över eller sparas som en ny.
 */
final class SaveTemplateDialog
{
	private static final int INDENT_PX = 14;

	private SaveTemplateDialog()
	{
	}

	static void show(Component owner, Goal goal, GoalManager goalManager, CustomTemplates custom)
	{
		ProfileGuard guard = ProfileGuard.of(goalManager);
		TemplateExport.Result export = TemplateExport.export(goalManager.getTree(), goal, goal.getDisplayTitle(), "",
			List.of());
		GoalTemplate source = CustomTemplates.isCustomId(goal.getTemplateId()) ? custom.find(goal.getTemplateId()) : null;

		JTextField name = new JTextField(source != null ? source.getName() : goal.getDisplayTitle(), 28);
		JTextField description = new JTextField(source != null ? source.getDescription() : "", 28);
		JTextField tags = new JTextField(source != null ? String.join(", ", source.getTags()) : "", 28);
		JPanel content = content(export, name, description, tags);

		Object[] options = source != null
			? new Object[]{"Save over '" + source.getName() + "'", "Save as new", "Cancel"}
			: new Object[]{"Save", "Cancel"};
		while (true)
		{
			int choice = JOptionPane.showOptionDialog(owner, content, "Save as template", JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			boolean overwrite = source != null && choice == 0;
			boolean asNew = source != null ? choice == 1 : choice == 0;
			if (!overwrite && !asNew)
			{
				return;
			}
			if (!GoalActions.stillSameProfile(owner, guard))
			{
				return;
			}
			TemplateExport.Result named = TemplateExport.export(goalManager.getTree(), goal, name.getText().trim(),
				description.getText().trim(), parseTags(tags.getText()));
			try
			{
				String id;
				if (overwrite)
				{
					id = source.getId();
					custom.replace(id, named);
				}
				else
				{
					id = custom.add(named);
				}
				// Målet kopplas till den sparade templaten, utom när det kommer från en inbyggd
				// template; då behålls den kopplingen.
				if (goal.getTemplateId() == null || CustomTemplates.isCustomId(goal.getTemplateId()))
				{
					goalManager.adoptTemplate(goal.getId(), id, named.getKeys());
				}
				JOptionPane.showMessageDialog(owner, "Saved '" + named.getName() + "' under My templates.",
					"Save as template", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			catch (CustomTemplates.TemplateException e)
			{
				JOptionPane.showMessageDialog(owner, e.getMessage(), "Save as template", JOptionPane.WARNING_MESSAGE);
			}
		}
	}

	/**
	 * Kommaseparerade taggar, utan tomma.
	 */
	static List<String> parseTags(String text)
	{
		List<String> tags = new ArrayList<>();
		for (String tag : text.split(","))
		{
			String trimmed = tag.trim();
			if (!trimmed.isEmpty() && !tags.contains(trimmed))
			{
				tags.add(trimmed);
			}
		}
		return tags;
	}

	private static JPanel content(TemplateExport.Result export, JTextField name, JTextField description,
		JTextField tags)
	{
		JPanel fields = new JPanel(new DynamicGridLayout(0, 2, 6, 4));
		fields.add(new JLabel("Name"));
		fields.add(name);
		fields.add(new JLabel("Description"));
		fields.add(description);
		fields.add(new JLabel("Tags (comma separated)"));
		fields.add(tags);

		JPanel preview = new JPanel(new DynamicGridLayout(0, 1, 0, 1));
		@Nullable TemplateNode root = buildPreview(export);
		if (root != null)
		{
			addRows(preview, root, 0);
		}
		JScrollPane scroll = new JScrollPane(preview);
		scroll.setPreferredSize(new Dimension(380, 260));
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.setBorder(BorderFactory.createTitledBorder("What is saved"));

		String text = export.getNodeCount() + (export.getNodeCount() == 1 ? " goal" : " goals")
			+ ". Not saved: done status, dates, counters and kill count.";
		if (export.getConvertedXpGains() > 0)
		{
			text += " XP gain goals are saved as manual goals, since their start XP belongs to this account.";
		}
		JLabel summary = new JLabel("<html><body style='width:360px'>" + UiText.escapeHtml(text) + "</body></html>");
		summary.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.add(fields, BorderLayout.NORTH);
		content.add(scroll, BorderLayout.CENTER);
		content.add(summary, BorderLayout.SOUTH);
		return content;
	}

	@Nullable
	private static TemplateNode buildPreview(TemplateExport.Result export)
	{
		try
		{
			return export.preview();
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	static void addRows(JPanel panel, TemplateNode node, int depth)
	{
		String text = node.getDisplayTitle() + (node.getRef() != null ? "  (shared, listed above)" : "");
		JLabel label = new JLabel(text);
		label.setBorder(new EmptyBorder(1, 4 + depth * INDENT_PX, 1, 4));
		if (node.getRef() != null)
		{
			label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		}
		panel.add(label);
		for (TemplateNode child : node.getChildren())
		{
			addRows(panel, child, depth + 1);
		}
	}
}
