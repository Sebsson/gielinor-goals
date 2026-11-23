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
import com.gielinorgoals.model.DropSource;
import com.gielinorgoals.model.DryMeter;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalEvaluator;
import com.gielinorgoals.model.GoalProgress;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ActivityCounters;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.template.TemplateUpdates;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
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
import net.runelite.client.util.LinkBrowser;
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
	// Bakgrund för målet som visas från Completed-fliken.
	private static final Color HIGHLIGHT_COLOR = new Color(74, 60, 30);
	// Liten länkikon för delmål som finns under flera föräldrar; ritas en gång.
	static final ImageIcon LINK_ICON = new ImageIcon(drawLinkIcon());

	private final Goal goal;
	// Föräldern raden visas under och dess förälder; null betyder rotnivå.
	@Nullable
	private final String parentId;
	@Nullable
	private final String grandparentId;
	@Nullable
	private final GoalProgress progress;
	private final GoalTree tree;
	private final GoalManager goalManager;
	private final DialogServices services;
	private final boolean editable;
	private final Color baseBackground;
	// Alla föräldrar till målet, inte bara den raden visas under.
	private final List<Goal> parents;
	// Räknaren för ett aggregerat mål ("142/170"), annars null.
	@Nullable
	private final GoalProgress counter;

	GoalRow(Goal goal, @Nullable String parentId, @Nullable String grandparentId, @Nullable GoalProgress progress,
		int depth, GoalTree tree, GoalManager goalManager, DialogServices services, boolean editable,
		boolean highlighted)
	{
		this.goal = goal;
		this.parentId = parentId;
		this.grandparentId = grandparentId;
		this.progress = progress;
		this.tree = tree;
		this.goalManager = goalManager;
		this.services = services;
		this.editable = editable;
		this.baseBackground = highlighted ? HIGHLIGHT_COLOR : ColorScheme.DARKER_GRAY_COLOR;
		this.parents = tree.getParents(goal.getId());
		this.counter = goal.isAggregate() ? GoalEvaluator.aggregateProgress(tree, goal) : null;

		setLayout(new BorderLayout(4, 0));
		setBackground(baseBackground);
		// Indrag för delmål, sedan en färgad kant som visar status, sedan luft runt innehållet.
		setBorder(new CompoundBorder(
			new EmptyBorder(0, depth * INDENT_PX, 0, 0),
			new CompoundBorder(new MatteBorder(0, 3, 0, 0, statusColor()), new EmptyBorder(5, 4, 5, 4))));

		JPanel left = new JPanel(new BorderLayout(2, 0));
		left.setOpaque(false);
		left.add(buildExpandToggle(), BorderLayout.WEST);
		left.add(buildCheckBox(), BorderLayout.CENTER);

		// Samma hover-text på hela raden, så att siffrorna syns var man än håller musen.
		String tooltip = buildTooltip();
		setToolTipText(tooltip);

		JPanel center = new JPanel(new BorderLayout(0, 3));
		center.setOpaque(false);
		center.setToolTipText(tooltip);
		center.add(buildTitle(depth, tooltip), BorderLayout.CENTER);
		// Räknarmål visar hur många delmål som är klara i stället för kravets progress.
		GoalProgress shown = counter != null ? counter : progress;
		if (shown != null && !goal.isCompleted())
		{
			ProgressBar bar = buildProgressBar(shown, counter != null);
			bar.setToolTipText(tooltip);
			center.add(bar, BorderLayout.SOUTH);
		}

		JPanel right = new JPanel(new BorderLayout(3, 0));
		right.setOpaque(false);
		if (parents.size() > 1)
		{
			JLabel link = new JLabel(LINK_ICON);
			link.setToolTipText("Also under: " + otherParentTitles());
			right.add(link, BorderLayout.WEST);
		}
		right.add(buildMenuButton(), BorderLayout.EAST);
		// Egna räknare räknas direkt på raden.
		if (editable && isManualCounter())
		{
			right.add(buildCounterButtons(), BorderLayout.CENTER);
		}

		add(left, BorderLayout.WEST);
		add(center, BorderLayout.CENTER);
		add(right, BorderLayout.EAST);

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
					setBackground(baseBackground);
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

	/**
	 * @param counterColors räknarmål färgas som overlayen (vitt, gult, grönt)
	 */
	private static ProgressBar buildProgressBar(GoalProgress progress, boolean counterColors)
	{
		ProgressBar bar = new ProgressBar();
		bar.setMaximumValue(PROGRESS_STEPS);
		bar.setValue((int) Math.round(progress.fraction() * PROGRESS_STEPS));
		bar.setCenterLabel(progress.getLabel());
		bar.setLeftLabel("");
		bar.setRightLabel("");
		if (counterColors)
		{
			bar.setForeground(ProgressColors.forFraction(progress.fraction()));
		}
		else
		{
			bar.setForeground(progress.isComplete()
				? ColorScheme.PROGRESS_COMPLETE_COLOR
				: ColorScheme.PROGRESS_INPROGRESS_COLOR);
		}
		bar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bar.setPreferredSize(new Dimension(0, 12));
		return bar;
	}

	private JLabel buildTitle(int depth, String tooltip)
	{
		int width = Math.max(MIN_TEXT_WIDTH, BASE_TEXT_WIDTH - depth * INDENT_PX);
		String text = escapeHtml(goal.getDisplayTitle());
		if (hasTemplateUpdate())
		{
			text += " <font color='#ff9800'>(update)</font>";
		}
		// Räknarmål har siffrorna i progressbaren i stället.
		if (goal.hasChildren() && counter == null)
		{
			text += " <font color='#a0a0a0'>(" + countCompletedChildren() + "/" + goal.getChildIds().size() + ")</font>";
		}
		if (goal.isCompleted())
		{
			text = "<strike>" + text + "</strike>";
		}
		String dry = DryMeter.rowText(goal, dropKc());
		if (dry != null)
		{
			text += "<br><font color='#a0a0a0'>" + escapeHtml(dry) + "</font>";
		}

		if (GoalIcons.hasIcon(goal))
		{
			// Ikonen tar plats, så texten får krympa lika mycket.
			width = Math.max(MIN_TEXT_WIDTH, width - ICON_WIDTH);
		}
		JLabel title = new JLabel("<html><body style='width:" + width + "px'>" + text + "</body></html>");
		title.setIconTextGap(4);
		services.getIcons().apply(goal, title);
		title.setFont(FontManager.getRunescapeSmallFont());
		title.setForeground(goal.isCompleted() ? ColorScheme.MEDIUM_GRAY_COLOR : Color.WHITE);
		title.setToolTipText(tooltip);

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
		menu.add(item("Move up", tree.canMoveUp(parentId, id), () -> goalManager.moveUp(parentId, id)));
		menu.add(item("Move down", tree.canMoveDown(parentId, id), () -> goalManager.moveDown(parentId, id)));
		menu.add(item("Make sub-goal of goal above", tree.canIndent(parentId, id),
			() -> goalManager.indent(parentId, id)));
		menu.add(item("Move out one level", tree.canOutdent(grandparentId, parentId, id),
			() -> goalManager.outdent(grandparentId, parentId, id)));
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
		menu.add(item("Link to another goal...", true, this::linkToAnotherGoal));
		if (parentId != null && parents.size() > 1)
		{
			Goal parent = tree.find(parentId);
			menu.add(item("Remove from " + (parent == null ? "this goal" : parent.getDisplayTitle()), true,
				() -> goalManager.unlink(parentId, id)));
		}
		JCheckBoxMenuItem counterItem = new JCheckBoxMenuItem("Show as counter", goal.isAggregate());
		counterItem.setToolTipText("Show how many sub-goals are done, e.g. 142/170, and collapse the list");
		counterItem.addActionListener(e -> goalManager.setAggregate(id, counterItem.isSelected()));
		menu.add(counterItem);
		menu.addSeparator();
		menu.add(item("Delete", true, this::delete));
		return menu;
	}

	// Templaten byggs om bara när målen har ändrats; resultatet cachas per revision.
	private boolean hasTemplateUpdate()
	{
		return goal.getTemplateId() != null
			&& services.getTemplateUpdates().hasUpdate(tree, goal, goalManager.getRevision());
	}

	private void updateFromTemplate()
	{
		TemplateUpdates.Plan plan = services.getTemplateUpdates().plan(tree, goal);
		if (plan == null)
		{
			JOptionPane.showMessageDialog(this, "The template this goal came from is no longer available.",
				"Update from template", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		TemplateUpdateDialog.Selection selection = TemplateUpdateDialog.show(this, plan, tree);
		if (selection != null)
		{
			goalManager.applyTemplateUpdate(goal.getId(), selection.getAdditions(), selection.getRemoveGoalIds(),
				selection.getDeclinedKeys());
		}
	}

	private boolean isManualCounter()
	{
		return goal.getRequirement() instanceof KillCountRequirement
			&& ((KillCountRequirement) goal.getRequirement()).getActivityType() == KillCountRequirement.ActivityType.MANUAL;
	}

	private JPanel buildCounterButtons()
	{
		String key = ((KillCountRequirement) goal.getRequirement()).counterKey();
		JPanel buttons = new JPanel(new BorderLayout(4, 0));
		buttons.setOpaque(false);
		buttons.add(counterButton("-", "Count one less", () -> goalManager.adjustCounter(key, -1)), BorderLayout.WEST);
		buttons.add(counterButton("+", "Count one more", () -> goalManager.adjustCounter(key, 1)), BorderLayout.EAST);
		return buttons;
	}

	private static JLabel counterButton(String text, String tooltip, Runnable action)
	{
		JLabel button = new JLabel(text);
		button.setFont(FontManager.getRunescapeBoldFont());
		button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		button.setToolTipText(tooltip);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					action.run();
				}
			}
		});
		return button;
	}

	// För alla KC-mål, t.ex. för att rätta ett fel eller ange ett startvärde.
	private void setCounterValue()
	{
		KillCountRequirement kc = (KillCountRequirement) goal.getRequirement();
		long current = goalManager.getCounters().value(kc.counterKey());
		Object input = JOptionPane.showInputDialog(this, "Current count for " + kc.getActivityName() + ":",
			"Set value", JOptionPane.PLAIN_MESSAGE, null, null, current < 0 ? "" : String.valueOf(current));
		if (input == null)
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
			JOptionPane.showMessageDialog(this, "Enter a whole number of 0 or more.", "Set value",
				JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void linkToAnotherGoal()
	{
		String newParentId = LinkGoalDialog.show(this, tree, goal);
		if (newParentId != null)
		{
			goalManager.link(newParentId, goal.getId());
		}
	}

	// T.ex. "From chat, 2 minutes ago" eller att värdet inte är känt än.
	private String counterSource()
	{
		ActivityCounters.Counter counter = goalManager.getCounters()
			.get(((KillCountRequirement) goal.getRequirement()).counterKey());
		if (counter == null)
		{
			return "Count not known yet";
		}
		return "From " + counter.getSource().getLabel() + ", "
			+ GielinorGoalsPanel.formatAge(System.currentTimeMillis() - counter.getUpdatedAt());
	}

	// Räknarens värde för målets drop-källa, -1 om okänt eller om det inte finns någon källa.
	private long dropKc()
	{
		DropSource source = DryMeter.sourceOf(goal);
		return source == null ? -1 : goalManager.getCounters().value(source.counterKey());
	}

	// Föräldrarna utom den raden visas under, t.ex. "Barrows gloves, Quest cape".
	private String otherParentTitles()
	{
		List<String> titles = new ArrayList<>();
		for (Goal parent : parents)
		{
			if (!parent.getId().equals(parentId))
			{
				titles.add(parent.getDisplayTitle());
			}
		}
		return String.join(", ", titles);
	}

	private static BufferedImage drawLinkIcon()
	{
		BufferedImage image = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		g.setStroke(new BasicStroke(1.6f));
		// Två länkar i en kedja, snett över varandra.
		g.rotate(-Math.PI / 4, 6, 6);
		g.drawRoundRect(0, 4, 7, 4, 4, 4);
		g.drawRoundRect(5, 4, 7, 4, 4, 4);
		g.dispose();
		return image;
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
		GoalEditDialog.Result result = GoalEditDialog.show(this, services, "Add sub-goal", null, "", null, null, false);
		if (result != null)
		{
			GoalEditDialog.addGoal(goalManager, services, goal.getId(), result);
		}
	}

	private void edit()
	{
		GoalEditDialog.Result result = GoalEditDialog.show(this, services, "Edit goal", goal.getId(), goal.getTitle(), goal.getNote(),
			goal.getRequirement(), goal.isAggregate(), goal.getDropSource());
		if (result != null)
		{
			goalManager.editGoal(goal.getId(), result.getTitle(), result.getNote(), result.getRequirement(),
				result.isAggregate(), result.getDropSource());
			GoalEditDialog.applyStartCount(goalManager, result);
		}
	}

	private void delete()
	{
		// Bekräfta när mer än den här raden försvinner: andra föräldrar eller delmål.
		// Ett enskilt mål utan delmål är lätt att skapa igen.
		boolean shared = parents.size() > 1;
		if (shared || goal.hasChildren())
		{
			StringBuilder message = new StringBuilder("<html>Delete \"")
				.append(escapeHtml(goal.getDisplayTitle())).append("\" completely?");
			if (shared)
			{
				message.append("<br>It is under ").append(parents.size())
					.append(" goals and will be removed from all of them.");
			}
			if (goal.hasChildren())
			{
				message.append("<br>Its sub-goals are deleted too, except those that are also under another goal.");
			}
			int choice = JOptionPane.showConfirmDialog(this, message.append("</html>").toString(),
				"Delete goal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (choice != JOptionPane.YES_OPTION)
			{
				return;
			}
		}
		goalManager.removeGoal(goal.getId());
	}

	/**
	 * Hover-text: målets fulla namn, exakta siffror för progressen, diary-tips och anteckning.
	 * Byggs en gång när raden skapas.
	 */
	private String buildTooltip()
	{
		StringBuilder html = new StringBuilder("<html><b>").append(escapeHtml(goal.getDisplayTitle())).append("</b>");

		String numbers = progress != null && goal.getRequirement() != null
			? goal.getRequirement().describeProgress(progress)
			: null;
		if (counter != null)
		{
			html.append("<br>").append(counter.getLabel()).append(" sub-goals done");
		}
		if (numbers != null)
		{
			html.append("<br>").append(escapeHtml(numbers).replace("\n", "<br>"));
		}
		if (goal.getRequirement() instanceof KillCountRequirement)
		{
			html.append("<br><i>").append(counterSource()).append("</i>");
		}
		DropSource drop = DryMeter.sourceOf(goal);
		if (drop != null && !goal.isCompleted())
		{
			html.append("<br>Drop rate ").append(escapeHtml(drop.describe()));
			String chance = DryMeter.chanceText(goal, dropKc());
			html.append("<br>").append(chance != null ? escapeHtml(chance)
				: "<i>Kill count not known yet</i>");
		}
		if (hasTemplateUpdate())
		{
			html.append("<br><i>The template has changed. Right-click and choose \"Update from template...\".</i>");
		}
		if (parents.size() > 1)
		{
			html.append("<br><i>Also under: ").append(escapeHtml(otherParentTitles())).append("</i>");
		}

		if (goal.getRequirement() instanceof DiaryRequirement && !goal.hasChildren() && !goal.isCompleted())
		{
			html.append("<br><i>Open the ")
				.append(escapeHtml(((DiaryRequirement) goal.getRequirement()).getArea().getDisplayName()))
				.append(" diary journal in game to add its tasks as sub-goals.</i>");
		}
		if (goal.getNote() != null)
		{
			html.append("<br><br>").append(escapeHtml(goal.getNote()).replace("\n", "<br>"));
		}
		return html.append("</html>").toString();
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
		for (Goal child : tree.getChildren(goal))
		{
			if (child.isCompleted())
			{
				count++;
			}
		}
		return count;
	}
}
