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
import com.gielinorgoals.model.GoalProgress;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.template.TemplateUpdates;
import java.awt.Component;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import net.runelite.client.util.LinkBrowser;

/**
 * Menyn och åtgärderna för ett mål, delade mellan panelens rader och kartan. Dialogerna
 * öppnas över {@code owner}, så att de hamnar vid raden eller kartfönstret.
 */
final class GoalActions
{
	private final Component owner;
	private final Goal goal;
	private final GoalTree tree;
	private final GoalManager goalManager;
	private final DialogServices services;

	GoalActions(Component owner, Goal goal, GoalTree tree, GoalManager goalManager, DialogServices services)
	{
		this.owner = owner;
		this.goal = goal;
		this.tree = tree;
		this.goalManager = goalManager;
		this.services = services;
	}

	/**
	 * Var menyn visas. I panelen finns en förälder (raden ligger under en av dem) och
	 * sorteringsval; på kartan finns ingen ordning att ändra, men alla föräldrar syns.
	 */
	static final class Context
	{
		@Nullable
		final String parentId;
		@Nullable
		final String grandparentId;
		final boolean ordering;
		final boolean showOnMap;

		private Context(@Nullable String parentId, @Nullable String grandparentId, boolean ordering, boolean showOnMap)
		{
			this.parentId = parentId;
			this.grandparentId = grandparentId;
			this.ordering = ordering;
			this.showOnMap = showOnMap;
		}

		static Context panel(@Nullable String parentId, @Nullable String grandparentId)
		{
			return new Context(parentId, grandparentId, true, true);
		}

		static Context map()
		{
			return new Context(null, null, false, false);
		}
	}

	JPopupMenu menu(Context context)
	{
		String id = goal.getId();
		List<Goal> parents = tree.getParents(id);
		JPopupMenu menu = new JPopupMenu();
		menu.add(item("Add sub-goal", true, this::addSubGoal));
		menu.add(item("Edit", true, this::edit));
		GoalProgress progress = goalManager.getProgress(id);
		// Ett uppfyllt automatiskt mål skulle bara bockas av igen, så valet är låst då.
		boolean lockedByGame = progress != null && progress.isComplete() || goalManager.isDoneByOtherGoal(goal);
		menu.add(item(goal.isCompleted() ? "Mark as not done" : "Mark as done", !lockedByGame,
			() -> goalManager.setCompleted(id, !goal.isCompleted())));

		if (context.ordering)
		{
			String parentId = context.parentId;
			menu.addSeparator();
			menu.add(item("Move up", tree.canMoveUp(parentId, id), () -> goalManager.moveUp(parentId, id)));
			menu.add(item("Move down", tree.canMoveDown(parentId, id), () -> goalManager.moveDown(parentId, id)));
			menu.add(item("Make sub-goal of goal above", tree.canIndent(parentId, id),
				() -> goalManager.indent(parentId, id)));
			menu.add(item("Move out one level", tree.canOutdent(context.grandparentId, parentId, id),
				() -> goalManager.outdent(context.grandparentId, parentId, id)));
		}

		menu.addSeparator();
		if (goal.getRequirement() instanceof KillCountRequirement)
		{
			menu.add(item("Set value...", true, this::setCounterValue));
		}
		if (goal.getTemplateId() != null)
		{
			menu.add(item("Update from template...", true, this::updateFromTemplate));
		}
		String wikiUrl = WikiLinks.url(goal);
		if (wikiUrl != null)
		{
			menu.add(item("Open on wiki", true, () -> LinkBrowser.browse(wikiUrl)));
		}
		if (context.showOnMap)
		{
			menu.add(item("Show on map", true, () -> services.showOnMap(id)));
		}
		menu.add(item("Link to another goal...", true, this::linkToAnotherGoal));
		if (parents.size() > 1)
		{
			// I panelen: föräldern raden ligger under. På kartan: varje förälder.
			for (Goal parent : parents)
			{
				if (context.parentId == null || context.parentId.equals(parent.getId()))
				{
					menu.add(item("Remove from " + parent.getDisplayTitle(), true,
						() -> goalManager.unlink(parent.getId(), id)));
				}
			}
		}
		JCheckBoxMenuItem counterItem = new JCheckBoxMenuItem("Show as counter", goal.isAggregate());
		counterItem.setToolTipText("Show how many sub-goals are done, e.g. 142/170, and collapse the list");
		counterItem.addActionListener(e -> goalManager.setAggregate(id, counterItem.isSelected()));
		menu.add(counterItem);
		menu.addSeparator();
		menu.add(item("Delete", true, this::delete));
		return menu;
	}

	/**
	 * Om profilen bytts medan en dialog var öppen används inte resultatet; användaren får veta
	 * varför inget hände.
	 */
	static boolean stillSameProfile(Component owner, ProfileGuard guard)
	{
		if (guard.isCurrent())
		{
			return true;
		}
		JOptionPane.showMessageDialog(owner, "You switched account while the dialog was open, so nothing was changed.",
			"Gielinor Goals", JOptionPane.INFORMATION_MESSAGE);
		return false;
	}

	void addSubGoal()
	{
		ProfileGuard guard = ProfileGuard.of(goalManager);
		GoalEditDialog.Result result = GoalEditDialog.show(owner, services, "Add sub-goal", null, "", null, null, false);
		if (result != null && stillSameProfile(owner, guard))
		{
			GoalEditDialog.addGoal(goalManager, services, goal.getId(), result);
		}
	}

	void edit()
	{
		ProfileGuard guard = ProfileGuard.of(goalManager);
		GoalEditDialog.Result result = GoalEditDialog.show(owner, services, "Edit goal", goal.getId(), goal.getTitle(),
			goal.getNote(), goal.getRequirement(), goal.isAggregate(), goal.getDropSource());
		if (result != null && stillSameProfile(owner, guard))
		{
			goalManager.editGoal(goal.getId(), result.getTitle(), result.getNote(), result.getRequirement(),
				result.isAggregate(), result.getDropSource());
			GoalEditDialog.applyStartCount(goalManager, result);
		}
	}

	void openWiki()
	{
		String url = WikiLinks.url(goal);
		if (url != null)
		{
			LinkBrowser.browse(url);
		}
	}

	// För alla KC-mål, t.ex. för att rätta ett fel eller ange ett startvärde.
	private void setCounterValue()
	{
		KillCountRequirement kc = (KillCountRequirement) goal.getRequirement();
		long current = goalManager.getCounters().value(kc.counterKey());
		ProfileGuard guard = ProfileGuard.of(goalManager);
		Object input = JOptionPane.showInputDialog(owner, "Current count for " + kc.getActivityName() + ":",
			"Set value", JOptionPane.PLAIN_MESSAGE, null, null, current < 0 ? "" : String.valueOf(current));
		// Räknarna hör till profilen; ett nytt konto ska inte få det gamla kontots värde.
		if (input == null || !stillSameProfile(owner, guard))
		{
			return;
		}
		try
		{
			long value = Long.parseLong(input.toString().replace(",", "").replace(" ", "").trim());
			if (value < 0)
			{
				throw new NumberFormatException();
			}
			goalManager.setCounter(kc.counterKey(), value);
		}
		catch (NumberFormatException e)
		{
			JOptionPane.showMessageDialog(owner, "Enter a whole number of 0 or more.", "Set value",
				JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void updateFromTemplate()
	{
		TemplateUpdates.Plan plan = services.getTemplateUpdates().plan(tree, goal);
		if (plan == null)
		{
			JOptionPane.showMessageDialog(owner, "The template this goal came from is no longer available.",
				"Update from template", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		ProfileGuard guard = ProfileGuard.of(goalManager);
		TemplateUpdateDialog.Selection selection = TemplateUpdateDialog.show(owner, plan, tree);
		if (selection != null && stillSameProfile(owner, guard))
		{
			goalManager.applyTemplateUpdate(goal.getId(), selection.getAdditions(), selection.getRemoveGoalIds(),
				selection.getDeclinedKeys());
		}
	}

	private void linkToAnotherGoal()
	{
		ProfileGuard guard = ProfileGuard.of(goalManager);
		String newParentId = LinkGoalDialog.show(owner, tree, goal);
		if (newParentId != null && stillSameProfile(owner, guard))
		{
			goalManager.link(newParentId, goal.getId());
		}
	}

	private void delete()
	{
		// Bekräfta när mer än det här målet försvinner: andra föräldrar eller delmål.
		// Ett enskilt mål utan delmål är lätt att skapa igen.
		int parentCount = tree.getParents(goal.getId()).size();
		boolean shared = parentCount > 1;
		if (shared || goal.hasChildren())
		{
			StringBuilder message = new StringBuilder("<html>Delete \"")
				.append(escapeHtml(goal.getDisplayTitle())).append("\" completely?");
			if (shared)
			{
				message.append("<br>It is under ").append(parentCount)
					.append(" goals and will be removed from all of them.");
			}
			if (goal.hasChildren())
			{
				message.append("<br>Its sub-goals are deleted too, except those that are also under another goal.");
			}
			ProfileGuard guard = ProfileGuard.of(goalManager);
			int choice = JOptionPane.showConfirmDialog(owner, message.append("</html>").toString(),
				"Delete goal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (choice != JOptionPane.YES_OPTION || !stillSameProfile(owner, guard))
			{
				return;
			}
		}
		goalManager.removeGoal(goal.getId());
	}

	private static JMenuItem item(String text, boolean enabled, Runnable action)
	{
		JMenuItem item = new JMenuItem(text);
		item.setEnabled(enabled);
		item.addActionListener(e -> action.run());
		return item;
	}

	// Användarens text får inte tolkas som HTML i JLabel.
	private static String escapeHtml(String text)
	{
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;");
	}
}
