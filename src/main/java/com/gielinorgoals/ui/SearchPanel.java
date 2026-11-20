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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Sökfält som söker medan man skriver, en resultatlista och vilket objekt som är valt.
 * Själva sökningen görs av en {@code searcher} som får söktexten och lämnar resultatet
 * på EDT (den kan köra på klienttråden emellan). Med {@code minQueryLength} 0 visas
 * hela listan direkt, vilket passar korta listor som quests och diaries.
 */
class SearchPanel<T> extends JPanel
{
	private static final int SEARCH_DELAY_MS = 250;

	private final BiConsumer<String, Consumer<List<T>>> searcher;
	private final Function<T, String> label;
	private final int minQueryLength;

	private final JTextField searchField = new JTextField(14);
	private final DefaultListModel<T> resultModel = new DefaultListModel<>();
	private final JList<T> resultList = new JList<>(resultModel);
	private final JLabel hintLabel = new JLabel(" ");
	private final Timer searchTimer;
	@Nullable
	private T selected;
	private String baseHint = " ";
	private boolean lastSearchEmpty;
	private Consumer<T> onSelect = value ->
	{
	};
	// Räknas upp vid varje sökning så att gamla svar som kommer sent ignoreras.
	private int searchGeneration;

	/**
	 * @param icon ikon per objekt i listan, eller null för objekt utan ikon
	 */
	SearchPanel(String placeholder, BiConsumer<String, Consumer<List<T>>> searcher, Function<T, String> label,
		Function<T, Icon> icon, int minQueryLength)
	{
		super(new BorderLayout(0, 4));
		this.searcher = searcher;
		this.label = label;
		this.minQueryLength = minQueryLength;

		// FlatLaf, som RuneLite använder, visar platshållartext med den här egenskapen.
		searchField.putClientProperty("JTextField.placeholderText", placeholder);
		searchField.putClientProperty("JTextField.showClearButton", true);

		searchTimer = new Timer(SEARCH_DELAY_MS, e -> runSearch());
		searchTimer.setRepeats(false);
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				searchTimer.restart();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				searchTimer.restart();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				searchTimer.restart();
			}
		});
		searchField.addActionListener(e -> runSearch());

		resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		resultList.setVisibleRowCount(7);
		resultList.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
			{
				@SuppressWarnings("unchecked")
				T item = (T) value;
				JLabel cell = (JLabel) super.getListCellRendererComponent(list, label.apply(item), index,
					isSelected, cellHasFocus);
				cell.setIcon(icon.apply(item));
				return cell;
			}
		});
		resultList.addListSelectionListener(e ->
		{
			if (!e.getValueIsAdjusting() && resultList.getSelectedValue() != null)
			{
				selected = resultList.getSelectedValue();
				updateHint();
				onSelect.accept(selected);
			}
		});
		JScrollPane resultScroll = new JScrollPane(resultList);
		resultScroll.setPreferredSize(new Dimension(280, 150));

		hintLabel.setFont(FontManager.getRunescapeSmallFont());
		hintLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		add(searchField, BorderLayout.NORTH);
		add(resultScroll, BorderLayout.CENTER);
		add(hintLabel, BorderLayout.SOUTH);
	}

	@Nullable
	T getSelected()
	{
		return selected;
	}

	/**
	 * Förval vid redigering. Objektet läggs överst och markeras, även om det inte
	 * skulle matcha en sökning (t.ex. en quest som redan är klar).
	 */
	void preselect(T value)
	{
		selected = value;
		resultModel.clear();
		resultModel.addElement(value);
		resultList.setSelectedIndex(0);
		updateHint();
	}

	void setOnSelect(Consumer<T> onSelect)
	{
		this.onSelect = onSelect;
	}

	/**
	 * Kör om sökningen med nuvarande text, t.ex. när status för klara mål har laddats.
	 */
	void refresh()
	{
		runSearch();
	}

	/**
	 * Hjälptext som visas när inget är valt, t.ex. att klara quests är dolda.
	 */
	void setHint(String text)
	{
		baseHint = text;
		updateHint();
	}

	private void updateHint()
	{
		if (selected != null)
		{
			hintLabel.setText("Selected: " + label.apply(selected));
		}
		else if (lastSearchEmpty)
		{
			hintLabel.setText("Nothing found.");
		}
		else
		{
			hintLabel.setText(baseHint);
		}
	}

	private void runSearch()
	{
		searchTimer.stop();
		String query = searchField.getText().trim();
		if (query.length() < minQueryLength)
		{
			return;
		}

		int generation = ++searchGeneration;
		searcher.accept(query, results ->
		{
			if (generation != searchGeneration)
			{
				return;
			}

			T previous = selected;
			resultModel.clear();
			results.forEach(resultModel::addElement);
			if (previous != null && results.contains(previous))
			{
				resultList.setSelectedValue(previous, true);
			}
			lastSearchEmpty = results.isEmpty();
			updateHint();
		});
	}

	/**
	 * Ritar om listan, t.ex. när en ikon har laddats klart.
	 */
	void repaintList()
	{
		resultList.repaint();
	}
}
