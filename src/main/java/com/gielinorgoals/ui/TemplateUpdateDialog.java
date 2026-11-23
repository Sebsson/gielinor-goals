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
import com.gielinorgoals.template.TemplateInserter;
import com.gielinorgoals.template.TemplateNode;
import com.gielinorgoals.template.TemplateUpdates;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import lombok.Value;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * "Update from template...": visar bara det som är nytt i templaten, ibockat, och mål som
 * inte längre finns i templaten, urbockade. Ingenting tas bort utan att användaren väljer det.
 */
final class TemplateUpdateDialog
{
	private static final int INDENT_PX = 14;

	@Value
	static class Selection
	{
		List<TemplateUpdates.Addition> additions;
		List<String> removeGoalIds;
		// Nycklar för tillägg som användaren bockade ur; de föreslås inte igen.
		List<String> declinedKeys;
	}

	private final TemplateUpdates.Plan plan;
	private final List<Goal> existingWithRequirement = new ArrayList<>();
	private final Map<TemplateNode, JCheckBox> nodeBoxes = new IdentityHashMap<>();
	private final Map<TemplateNode, TemplateNode> parents = new IdentityHashMap<>();
	private final List<JCheckBox> removeBoxes = new ArrayList<>();
	private final JLabel summary = new JLabel(" ");

	private TemplateUpdateDialog(TemplateUpdates.Plan plan, GoalTree tree)
	{
		this.plan = plan;
		for (Goal goal : tree.flatten())
		{
			if (goal.getRequirement() != null)
			{
				existingWithRequirement.add(goal);
			}
		}
	}

	/**
	 * @return användarens val, eller null om användaren avbröt eller inget finns att uppdatera
	 */
	@Nullable
	static Selection show(Component parent, TemplateUpdates.Plan plan, GoalTree tree)
	{
		if (plan.isEmpty())
		{
			JOptionPane.showMessageDialog(parent, "This goal is up to date with its template.", "Update from template",
				JOptionPane.INFORMATION_MESSAGE);
			return null;
		}
		TemplateUpdateDialog dialog = new TemplateUpdateDialog(plan, tree);
		int choice = JOptionPane.showOptionDialog(parent, dialog.buildContent(), "Update from template",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Update", "Cancel"}, "Update");
		return choice == 0 ? dialog.selection() : null;
	}

	private JPanel buildContent()
	{
		JPanel rows = new JPanel(new DynamicGridLayout(0, 1, 0, 1));
		if (!plan.getAdditions().isEmpty())
		{
			rows.add(heading("New in the template"));
			for (TemplateUpdates.Addition addition : plan.getAdditions())
			{
				addRows(rows, addition.getNode(), null, 0);
			}
		}
		if (!plan.getRemoved().isEmpty())
		{
			rows.add(heading("No longer in the template"));
			for (Goal goal : plan.getRemoved())
			{
				// Urbockat som standard: inget tas bort utan att användaren väljer det.
				JCheckBox box = new JCheckBox("Remove \"" + goal.getDisplayTitle() + "\", which is no longer in the template",
					false);
				box.setFocusPainted(false);
				box.addActionListener(e -> updateSummary());
				removeBoxes.add(box);
				rows.add(box);
			}
		}

		JScrollPane scroll = new JScrollPane(rows);
		scroll.setPreferredSize(new Dimension(380, 300));
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.setBorder(BorderFactory.createEmptyBorder());

		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.add(new JLabel("<html>Changes for \"" + plan.getRoot().getDisplayTitle() + "\" since it was added:</html>"),
			BorderLayout.NORTH);
		content.add(scroll, BorderLayout.CENTER);
		content.add(summary, BorderLayout.SOUTH);
		updateSummary();
		return content;
	}

	private void addRows(JPanel rows, TemplateNode node, @Nullable TemplateNode parent, int depth)
	{
		parents.put(node, parent);
		JCheckBox box = new JCheckBox(node.getDisplayTitle(), node.isIncluded());
		box.setFocusPainted(false);
		box.addActionListener(e ->
		{
			node.setIncluded(box.isSelected());
			updateEnabled();
			updateSummary();
		});
		nodeBoxes.put(node, box);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBorder(new EmptyBorder(0, depth * INDENT_PX, 0, 4));
		row.add(box, BorderLayout.CENTER);
		if (TemplateInserter.findExisting(existingWithRequirement, node.getRequirement()) != null)
		{
			JLabel status = new JLabel("already exists");
			status.setIcon(GoalRow.LINK_ICON);
			status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			status.setToolTipText("You already have this goal; it is linked in instead of copied");
			row.add(status, BorderLayout.EAST);
		}
		rows.add(row);
		for (TemplateNode child : node.getChildren())
		{
			addRows(rows, child, node, depth + 1);
		}
	}

	// Delmål under en urbockad nod kan inte väljas.
	private void updateEnabled()
	{
		for (Map.Entry<TemplateNode, JCheckBox> entry : nodeBoxes.entrySet())
		{
			entry.getValue().setEnabled(ancestorsIncluded(entry.getKey()));
		}
	}

	private boolean ancestorsIncluded(TemplateNode node)
	{
		TemplateNode parent = parents.get(node);
		while (parent != null)
		{
			if (!parent.isIncluded())
			{
				return false;
			}
			parent = parents.get(parent);
		}
		return true;
	}

	private void updateSummary()
	{
		int adds = 0;
		for (TemplateNode node : nodeBoxes.keySet())
		{
			if (node.isIncluded() && ancestorsIncluded(node))
			{
				adds++;
			}
		}
		int removes = 0;
		for (JCheckBox box : removeBoxes)
		{
			removes += box.isSelected() ? 1 : 0;
		}
		summary.setText("Adds " + adds + (adds == 1 ? " goal" : " goals") + ", removes " + removes);
	}

	private Selection selection()
	{
		List<TemplateUpdates.Addition> chosen = new ArrayList<>();
		List<String> declined = new ArrayList<>();
		for (TemplateUpdates.Addition addition : plan.getAdditions())
		{
			if (addition.getNode().isIncluded())
			{
				chosen.add(addition);
				collectDeclined(addition.getNode(), declined);
			}
			else
			{
				declined.add(addition.getNode().getKey());
			}
		}
		List<String> remove = new ArrayList<>();
		for (int i = 0; i < removeBoxes.size(); i++)
		{
			if (removeBoxes.get(i).isSelected())
			{
				remove.add(plan.getRemoved().get(i).getId());
			}
		}
		return new Selection(chosen, remove, declined);
	}

	private static void collectDeclined(TemplateNode node, List<String> out)
	{
		for (TemplateNode child : node.getChildren())
		{
			if (!child.isIncluded())
			{
				out.add(child.getKey());
			}
			else
			{
				collectDeclined(child, out);
			}
		}
	}

	private static JLabel heading(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setBorder(new EmptyBorder(6, 0, 2, 0));
		return label;
	}
}
