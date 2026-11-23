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

import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.template.GoalTemplate;
import com.gielinorgoals.template.TemplateNode;
import com.gielinorgoals.template.TemplateParameter;
import com.gielinorgoals.template.TemplatePlanner;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * Dialogen "Add from template...": välj en template, ställ in parametrar, se förslaget
 * som ett träd och bocka ur delar. Varje nod visar om den redan är klar (✓) eller kopplas
 * till ett befintligt mål. Inget läggs till förrän användaren bekräftar.
 */
final class TemplateDialog
{
	private static final int INDENT_PX = 14;
	private static final int PREVIEW_WIDTH = 380;
	// Ungefär tre templates åt gången; resten nås genom att skrolla.
	private static final int LIST_HEIGHT = 150;
	// Textbredd i listan: dialogens bredd minus ikon, marginaler och skrollist.
	private static final int LIST_TEXT_WIDTH = 290;
	private static final Icon NO_ICON = new ImageIcon(new java.awt.image.BufferedImage(1, 1,
		java.awt.image.BufferedImage.TYPE_INT_ARGB));

	private final List<GoalTemplate> templates;
	private final GoalTree tree;
	private final GameData data;

	private final JList<GoalTemplate> templateList;
	private final Map<GoalTemplate, Icon> templateIcons = new HashMap<>();
	private final JPanel parameterPanel = new JPanel(new DynamicGridLayout(0, 2, 6, 4));
	private final Map<String, JSpinner> spinners = new HashMap<>();
	private final JPanel previewPanel = new JPanel(new DynamicGridLayout(0, 1, 0, 1));
	private final JLabel summaryLabel = new JLabel(" ");

	@Nullable
	private TemplateNode current;
	// Varje nods rad i förhandsvisningen och dess förälder i förslaget.
	private final Map<TemplateNode, PreviewRow> rows = new IdentityHashMap<>();

	private TemplateDialog(List<GoalTemplate> templates, GoalTree tree, GameData data, GoalIcons icons)
	{
		this.templates = templates;
		this.tree = tree;
		this.data = data;
		this.templateList = new JList<>(templates.toArray(new GoalTemplate[0]));

		for (GoalTemplate template : templates)
		{
			Requirement iconRequirement = template.getIconRequirement();
			if (iconRequirement != null)
			{
				icons.loadImage(iconRequirement, image -> SwingUtilities.invokeLater(() ->
				{
					templateIcons.put(template, new ImageIcon(image));
					templateList.repaint();
				}));
			}
		}
	}

	/**
	 * @return förslaget med användarens urbockningar, eller null om användaren avbröt
	 */
	@Nullable
	static TemplateNode show(Component parent, List<GoalTemplate> templates, GoalTree tree, GameData data,
		GoalIcons icons)
	{
		if (templates.isEmpty())
		{
			return null;
		}
		TemplateDialog dialog = new TemplateDialog(templates, tree, data, icons);
		JPanel content = dialog.buildContent();
		int choice = JOptionPane.showOptionDialog(parent, content, "Add from template",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Add", "Cancel"}, "Add");
		return choice == 0 ? dialog.current : null;
	}

	private JPanel buildContent()
	{
		templateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// Raderna har olika höjd när beskrivningen bryts; ingen fast radhöjd.
		templateList.setFixedCellHeight(-1);
		templateList.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
			{
				GoalTemplate template = (GoalTemplate) value;
				// Fast textbredd, så att långa beskrivningar bryts i stället för att kapas.
				String text = "<html><body style='width:" + LIST_TEXT_WIDTH + "px'><b>" + escapeHtml(template.getName())
					+ "</b><br><font color='#a0a0a0'>" + escapeHtml(template.getDescription()) + "</font></body></html>";
				JLabel label = (JLabel) super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
				label.setIcon(templateIcons.getOrDefault(template, NO_ICON));
				label.setIconTextGap(8);
				label.setBorder(new EmptyBorder(4, 4, 4, 4));
				return label;
			}
		});
		templateList.addListSelectionListener(e ->
		{
			if (!e.getValueIsAdjusting())
			{
				selectTemplate(templateList.getSelectedValue());
			}
		});

		JScrollPane previewScroll = new JScrollPane(previewPanel);
		previewScroll.setPreferredSize(new Dimension(PREVIEW_WIDTH, 300));
		previewScroll.getVerticalScrollBar().setUnitIncrement(16);
		previewScroll.setBorder(BorderFactory.createTitledBorder("Preview"));

		JPanel top = new JPanel(new BorderLayout(0, 6));
		// Listan får en fast höjd och skrollar lodrätt när det finns fler templates än som syns.
		JScrollPane listScroll = new JScrollPane(templateList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		listScroll.setPreferredSize(new Dimension(PREVIEW_WIDTH, LIST_HEIGHT));
		listScroll.getVerticalScrollBar().setUnitIncrement(16);
		top.add(listScroll, BorderLayout.CENTER);
		top.add(parameterPanel, BorderLayout.SOUTH);

		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.add(top, BorderLayout.NORTH);
		content.add(previewScroll, BorderLayout.CENTER);
		content.add(summaryLabel, BorderLayout.SOUTH);

		templateList.setSelectedIndex(0);
		return content;
	}

	private void selectTemplate(@Nullable GoalTemplate template)
	{
		parameterPanel.removeAll();
		spinners.clear();
		if (template != null)
		{
			for (TemplateParameter parameter : template.getParameters())
			{
				JSpinner spinner = new JSpinner(new SpinnerNumberModel(parameter.getDefaultValue(),
					parameter.getMin(), parameter.getMax(), 1));
				// Förhandsvisningen följer med direkt när värdet ändras.
				spinner.addChangeListener(e -> rebuildPreview(template));
				spinners.put(parameter.getKey(), spinner);
				parameterPanel.add(new JLabel(parameter.getLabel() + " (" + parameter.getMin() + "-"
					+ parameter.getMax() + ")"));
				parameterPanel.add(spinner);
			}
		}
		parameterPanel.revalidate();
		rebuildPreview(template);
	}

	private void rebuildPreview(@Nullable GoalTemplate template)
	{
		previewPanel.removeAll();
		rows.clear();
		current = null;
		if (template != null)
		{
			Map<String, Integer> values = new HashMap<>();
			spinners.forEach((key, spinner) -> values.put(key, (Integer) spinner.getValue()));
			current = template.build(values);
			addRows(current, null, 0);
		}
		refreshStatuses();
		previewPanel.revalidate();
		previewPanel.repaint();
	}

	private void addRows(TemplateNode node, @Nullable TemplateNode parent, int depth)
	{
		PreviewRow row = new PreviewRow(node, parent, depth);
		rows.put(node, row);
		previewPanel.add(row.panel);
		for (TemplateNode child : node.getChildren())
		{
			addRows(child, node, depth + 1);
		}
	}

	// Räknar om förslaget (billigt) och uppdaterar markeringar och sammanfattning.
	private void refreshStatuses()
	{
		if (current == null)
		{
			summaryLabel.setText(" ");
			return;
		}
		TemplatePlanner.Plan plan = TemplatePlanner.plan(tree, current, data);
		for (PreviewRow row : rows.values())
		{
			row.update(plan);
		}
		summaryLabel.setText(plan.summary());
	}

	private final class PreviewRow
	{
		private final TemplateNode node;
		@Nullable
		private final TemplateNode parent;
		private final JPanel panel = new JPanel(new BorderLayout(6, 0));
		private final JCheckBox checkBox;
		private final JLabel status = new JLabel();

		private PreviewRow(TemplateNode node, @Nullable TemplateNode parent, int depth)
		{
			this.node = node;
			this.parent = parent;
			checkBox = new JCheckBox(node.getDisplayTitle(), node.isIncluded());
			checkBox.setFocusPainted(false);
			if (parent == null)
			{
				// Roten är själva templaten och går inte att bocka ur.
				checkBox.setEnabled(false);
				checkBox.setSelected(true);
			}
			checkBox.addActionListener(e ->
			{
				node.setIncluded(checkBox.isSelected());
				refreshStatuses();
			});
			status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			panel.setBorder(new EmptyBorder(0, depth * INDENT_PX, 0, 4));
			panel.add(checkBox, BorderLayout.CENTER);
			panel.add(status, BorderLayout.EAST);
		}

		private void update(TemplatePlanner.Plan plan)
		{
			// Delmål under en urbockad eller inkopplad nod läggs inte till och kan inte väljas.
			boolean parentAdds = parent == null || plan.statusOf(parent) == TemplatePlanner.Status.NEW;
			checkBox.setEnabled(parent != null && parentAdds);
			checkBox.setToolTipText(!parentAdds && parent != null && plan.statusOf(parent) == TemplatePlanner.Status.LINKED
				? "The existing goal keeps its own sub-goals"
				: null);

			TemplatePlanner.Status nodeStatus = plan.statusOf(node);
			boolean done = plan.isDone(node);
			status.setIcon(null);
			status.setToolTipText(null);
			switch (nodeStatus)
			{
				case LINKED:
					status.setIcon(GoalRow.LINK_ICON);
					status.setText(done ? "already exists ✓" : "already exists");
					status.setToolTipText("You already have this goal; it is linked in instead of copied");
					break;
				case DUPLICATE:
					status.setIcon(GoalRow.LINK_ICON);
					status.setText("listed above");
					break;
				case NEW:
					status.setText(done ? "✓ done" : "");
					status.setForeground(done ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
					break;
				default:
					status.setText("");
					break;
			}
			if (nodeStatus != TemplatePlanner.Status.NEW)
			{
				status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
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
