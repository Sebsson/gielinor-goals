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
import com.gielinorgoals.map.MapGraph;
import com.gielinorgoals.map.MapLayout;
import com.gielinorgoals.map.MapLayoutCache;
import com.gielinorgoals.map.MapView;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalProgress;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.NextGoals;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import lombok.Value;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Fönstret med målkartan. Det finns bara ett: "Map" i panelen visar det eller tar fram
 * det befintliga. Stängning döljer det; pluginet disposar det vid avstängning.
 *
 * <p>Vyn (rot, utfällning, "Dölj klara") lever bara så länge fönstret finns och påverkar
 * inte panelen. {@link #refresh()} anropas vid varje ändring i målen men gör något bara när
 * fönstret syns och något faktiskt har ändrats. Används bara på EDT.
 */
public final class GoalMapWindow extends JFrame
{
	private static final int SAVE_DELAY_MS = 500;
	private static final int NEXT_GOALS = 5;

	private final GoalManager goalManager;
	private final GielinorGoalsConfig config;
	private final ConfigManager configManager;
	private final MapLayoutCache cache = new MapLayoutCache();
	private final GoalMapCanvas canvas;

	private final JComboBox<RootOption> rootBox = new JComboBox<>();
	private final JTextField searchField = new JTextField(16);
	private final JCheckBox hideCompletedBox = new JCheckBox("Hide completed");
	private final JButton fitButton = new JButton("Fit to window");
	private final JLabel statusLabel = new JLabel(" ");
	private final Timer saveTimer;

	@Nullable
	private MapView view;
	// Senast visade läge, så att samma läge inte målas om vid varje tick.
	private long shownRevision = Long.MIN_VALUE;
	@Nullable
	private MapLayout shownLayout;
	private long shownSignature;
	private boolean updatingRoots;
	private String lastQuery = "";
	private int searchIndex;

	@Value
	private static class RootOption
	{
		@Nullable
		String goalId;
		String title;

		@Override
		public String toString()
		{
			return title;
		}
	}

	private static final RootOption ALL_GOALS = new RootOption(null, "All goals");

	public GoalMapWindow(GoalManager goalManager, GielinorGoalsConfig config, ConfigManager configManager,
		GoalIcons icons, BufferedImage windowIcon)
	{
		super("Gielinor Goals - Map");
		this.goalManager = goalManager;
		this.config = config;
		this.configManager = configManager;
		setIconImage(windowIcon);
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		canvas = new GoalMapCanvas(icons, goalManager::getProgress, goalManager::getCounters);
		canvas.setOnToggle(this::toggle);

		JPanel content = new JPanel(new BorderLayout());
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.add(buildToolbar(), BorderLayout.NORTH);
		content.add(canvas, BorderLayout.CENTER);
		setContentPane(content);

		saveTimer = new Timer(SAVE_DELAY_MS, e -> saveBounds());
		saveTimer.setRepeats(false);
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentMoved(ComponentEvent e)
			{
				saveTimer.restart();
			}

			@Override
			public void componentResized(ComponentEvent e)
			{
				saveTimer.restart();
			}
		});
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveBounds();
			}
		});
		restoreBounds();
	}

	private JPanel buildToolbar()
	{
		rootBox.setToolTipText("Which goal to map");
		rootBox.addActionListener(e ->
		{
			if (updatingRoots || view == null)
			{
				return;
			}
			RootOption option = (RootOption) rootBox.getSelectedItem();
			view = view.withRoot(option == null ? null : option.getGoalId());
			refresh(true);
		});

		searchField.putClientProperty("JTextField.placeholderText", "Search, press Enter");
		searchField.setToolTipText("Find a goal by name; press Enter again for the next match");
		searchField.addActionListener(e -> search());

		hideCompletedBox.setOpaque(false);
		hideCompletedBox.setFocusPainted(false);
		hideCompletedBox.addActionListener(e ->
		{
			if (view != null)
			{
				view = view.withHideCompleted(hideCompletedBox.isSelected());
				refresh(false);
			}
		});

		fitButton.setFocusPainted(false);
		fitButton.addActionListener(e -> canvas.fitToWindow());

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
		toolbar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR));
		toolbar.add(new JLabel("Show"));
		toolbar.add(rootBox);
		toolbar.add(searchField);
		toolbar.add(hideCompletedBox);
		toolbar.add(fitButton);
		toolbar.add(statusLabel);
		return toolbar;
	}

	/**
	 * Visar fönstret, eller tar fram det om det redan är öppet.
	 */
	public void open()
	{
		if (!isVisible())
		{
			setVisible(true);
		}
		if (getExtendedState() == ICONIFIED)
		{
			setExtendedState(NORMAL);
		}
		toFront();
		requestFocus();
		refresh(false);
	}

	/**
	 * Stänger fönstret för gott, t.ex. när pluginet stängs av.
	 */
	public void close()
	{
		saveTimer.stop();
		if (isVisible())
		{
			saveBounds();
		}
		dispose();
	}

	/**
	 * Anropas vid varje ändring i målen. Layouten räknas om bara om målen eller vyn ändrats,
	 * och kartan målas om bara om något som syns har ändrats.
	 */
	public void refresh()
	{
		refresh(false);
	}

	private void refresh(boolean fit)
	{
		if (!isVisible())
		{
			return;
		}
		GoalTree tree = goalManager.getTree();
		updateRoots(tree);
		if (view == null)
		{
			RootOption selected = (RootOption) rootBox.getSelectedItem();
			view = MapView.of(selected == null ? null : selected.getGoalId());
		}

		long revision = goalManager.getRevision();
		MapLayout layout = cache.get(tree, revision, view);
		MapGraph graph = Objects.requireNonNull(cache.getGraph());
		Set<String> next = new HashSet<>();
		NextGoals.selectGoals(tree, NEXT_GOALS).forEach(goal -> next.add(goal.getId()));
		long signature = signature(graph, next);

		boolean goalsChanged = revision != shownRevision;
		if (!goalsChanged && layout == shownLayout && signature == shownSignature && !fit)
		{
			return;
		}
		shownRevision = revision;
		shownLayout = layout;
		shownSignature = signature;
		canvas.setData(tree, graph, layout, next, goalsChanged);
		if (fit)
		{
			canvas.fitToWindow();
		}
		statusLabel.setText(graph.size() + " goals");
	}

	// Allt som syns på noderna och som kan ändras utan att målen sparas: progress och räknare.
	private long signature(MapGraph graph, Set<String> next)
	{
		long hash = next.hashCode();
		for (Goal goal : graph.getNodes())
		{
			GoalProgress progress = goalManager.getProgress(goal.getId());
			hash = hash * 31 + (progress == null ? 0 : progress.getLabel().hashCode());
			hash = hash * 31 + (goal.isCompleted() ? 1 : 0);
			if (goal.getRequirement() instanceof KillCountRequirement)
			{
				hash = hash * 31 + goalManager.getCounters().value(((KillCountRequirement) goal.getRequirement()).counterKey());
			}
			if (goal.getDropSource() != null)
			{
				hash = hash * 31 + goalManager.getCounters().value(goal.getDropSource().counterKey());
			}
		}
		return hash;
	}

	// Rotlistan byggs om bara när rötterna eller deras namn har ändrats.
	private void updateRoots(GoalTree tree)
	{
		List<RootOption> options = new ArrayList<>();
		for (Goal root : tree.getRoots())
		{
			options.add(new RootOption(root.getId(), root.getDisplayTitle()));
		}
		options.add(ALL_GOALS);

		List<RootOption> current = new ArrayList<>();
		for (int i = 0; i < rootBox.getItemCount(); i++)
		{
			current.add(rootBox.getItemAt(i));
		}
		if (current.equals(options))
		{
			return;
		}

		RootOption selected = (RootOption) rootBox.getSelectedItem();
		updatingRoots = true;
		try
		{
			rootBox.removeAllItems();
			options.forEach(rootBox::addItem);
			RootOption keep = selected == null ? null : find(options, selected.getGoalId());
			rootBox.setSelectedItem(keep != null ? keep : options.get(0));
		}
		finally
		{
			updatingRoots = false;
		}
		RootOption now = (RootOption) rootBox.getSelectedItem();
		if (view != null && now != null && !Objects.equals(view.getRootId(), now.getGoalId()))
		{
			view = view.withRoot(now.getGoalId());
		}
	}

	@Nullable
	private static RootOption find(List<RootOption> options, @Nullable String goalId)
	{
		for (RootOption option : options)
		{
			if (Objects.equals(option.getGoalId(), goalId))
			{
				return option;
			}
		}
		return null;
	}

	private void toggle(String goalId)
	{
		MapGraph graph = cache.getGraph();
		if (view == null || graph == null)
		{
			return;
		}
		view = view.withExpanded(goalId, graph.isCollapsed(goalId));
		refresh(false);
	}

	// Träffar bland de synliga i kartans ordning; Enter igen går till nästa. Ett mål som inte
	// syns i vyn tas fram med reveal().
	private void search()
	{
		String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
		MapGraph graph = cache.getGraph();
		if (query.isEmpty() || graph == null)
		{
			canvas.setSelected(null);
			return;
		}
		if (!query.equals(lastQuery))
		{
			lastQuery = query;
			searchIndex = 0;
		}

		List<Goal> matches = new ArrayList<>();
		for (Goal goal : goalManager.getTree().flatten())
		{
			if (goal.getDisplayTitle().toLowerCase(Locale.ROOT).contains(query))
			{
				matches.add(goal);
			}
		}
		if (matches.isEmpty())
		{
			statusLabel.setText("No goal matches \"" + searchField.getText().trim() + "\"");
			canvas.setSelected(null);
			return;
		}
		// Synliga först, så att Enter stannar på kartan man tittar på så länge det går.
		matches.sort((a, b) -> Boolean.compare(!graph.contains(a.getId()), !graph.contains(b.getId())));
		Goal match = matches.get(searchIndex % matches.size());
		searchIndex++;
		reveal(match.getId());
		statusLabel.setText("Match " + ((searchIndex - 1) % matches.size() + 1) + " of " + matches.size());
	}

	/**
	 * Visar målet på kartan: byter rot och fäller ut vägen dit om det behövs, och centrerar
	 * och markerar det.
	 */
	public void reveal(String goalId)
	{
		GoalTree tree = goalManager.getTree();
		Goal goal = tree.find(goalId);
		if (goal == null)
		{
			return;
		}
		MapGraph graph = cache.getGraph();
		if (view == null || graph == null || !graph.contains(goalId))
		{
			// En väg uppåt via första föräldern; DAG:en gör att den alltid tar slut i en rot.
			List<String> path = new ArrayList<>();
			Goal current = goal;
			while (true)
			{
				List<Goal> parents = tree.getParents(current.getId());
				if (parents.isEmpty())
				{
					break;
				}
				current = parents.get(0);
				path.add(current.getId());
			}
			MapView next = (view == null ? MapView.of(current.getId()) : view.withRoot(current.getId()));
			for (String id : path)
			{
				next = next.withExpanded(id, true);
			}
			if (next.isHideCompleted() && goal.isCompleted())
			{
				next = next.withHideCompleted(false);
				hideCompletedBox.setSelected(false);
			}
			view = next;
			selectRoot(current.getId());
			refresh(false);
		}
		canvas.setSelected(goalId);
		canvas.centerOn(goalId);
	}

	private void selectRoot(String rootId)
	{
		for (int i = 0; i < rootBox.getItemCount(); i++)
		{
			if (Objects.equals(rootBox.getItemAt(i).getGoalId(), rootId))
			{
				updatingRoots = true;
				try
				{
					rootBox.setSelectedIndex(i);
				}
				finally
				{
					updatingRoots = false;
				}
				return;
			}
		}
	}

	private void saveBounds()
	{
		if (!isDisplayable() || getExtendedState() != NORMAL)
		{
			return;
		}
		Rectangle b = getBounds();
		String value = b.x + "," + b.y + "," + b.width + "," + b.height;
		if (!value.equals(config.mapWindowBounds()))
		{
			configManager.setConfiguration(GielinorGoalsConfig.GROUP, GielinorGoalsConfig.MAP_WINDOW_BOUNDS_KEY, value);
		}
	}

	// Sparad position används bara om den syns på någon skärm, t.ex. inte om en skärm kopplats ur.
	private void restoreBounds()
	{
		Rectangle saved = parseBounds(config.mapWindowBounds());
		if (saved != null && onScreen(saved))
		{
			setBounds(saved);
			return;
		}
		setSize(new Dimension(1100, 720));
		setLocationRelativeTo(null);
	}

	@Nullable
	static Rectangle parseBounds(@Nullable String value)
	{
		if (value == null || value.isEmpty())
		{
			return null;
		}
		String[] parts = value.split(",");
		if (parts.length != 4)
		{
			return null;
		}
		try
		{
			Rectangle bounds = new Rectangle(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
				Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim()));
			return bounds.width >= 300 && bounds.height >= 200 ? bounds : null;
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	private static boolean onScreen(Rectangle bounds)
	{
		if (GraphicsEnvironment.isHeadless())
		{
			return false;
		}
		for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
		{
			Rectangle screen = device.getDefaultConfiguration().getBounds();
			Rectangle overlap = screen.intersection(bounds);
			if (!overlap.isEmpty() && overlap.width >= 100 && overlap.height >= 50)
			{
				return true;
			}
		}
		return false;
	}

	// För tester.
	GoalMapCanvas getCanvas()
	{
		return canvas;
	}
}
