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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Söker bland alla föremål i spelets cache, även otradeable (viktigt för ironmen,
 * till skillnad från ItemManager.search som bara täcker GE-föremål).
 * Namnindexet byggs en gång på klienttråden vid första sökningen, i portioner så att
 * spelet inte hackar medan alla föremål gås igenom.
 */
@Singleton
public class ItemSearch
{
	static final int MAX_RESULTS = 50;
	// Föremål per klientvarv när indexet byggs.
	static final int INDEX_CHUNK = 2000;

	@Value
	public static class ItemOption
	{
		int id;
		String name;

		@Override
		public String toString()
		{
			return name;
		}
	}

	private final Client client;
	private final ClientThread clientThread;
	private final ItemManager itemManager;

	// Byggs och läses bara på klienttråden.
	private List<ItemOption> index;
	private IndexBuilder builder;

	@Inject
	ItemSearch(Client client, ClientThread clientThread, ItemManager itemManager)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.itemManager = itemManager;
	}

	/**
	 * Söker asynkront; resultatet levereras på EDT.
	 */
	public void search(String query, Consumer<List<ItemOption>> callback)
	{
		String needle = query.trim().toLowerCase(Locale.ROOT);
		if (needle.isEmpty())
		{
			callback.accept(Collections.emptyList());
			return;
		}

		// Körs om varje klientvarv tills indexet är klart (false = försök igen nästa varv).
		clientThread.invokeLater(() ->
		{
			if (index == null)
			{
				if (builder == null)
				{
					builder = new IndexBuilder(client.getItemCount(), this::entry);
				}
				if (!builder.step(INDEX_CHUNK))
				{
					return false;
				}
				index = builder.result();
				builder = null;
			}
			List<ItemOption> results = filter(index, needle);
			SwingUtilities.invokeLater(() -> callback.accept(results));
			return true;
		});
	}

	public AsyncBufferedImage getImage(int itemId)
	{
		return itemManager.getImage(itemId);
	}

	// Ett föremål som det ser ut i indexet, eller null om det inte ska vara med.
	@javax.annotation.Nullable
	private String entry(int id)
	{
		ItemComposition composition = itemManager.getItemComposition(id);
		String name = composition.getName();
		// Hoppa över noterade föremål, placeholders och namnlösa poster.
		if (name == null || name.isEmpty() || "null".equalsIgnoreCase(name)
			|| composition.getNote() != -1 || composition.getPlaceholderTemplateId() != -1)
		{
			return null;
		}
		return name;
	}

	/**
	 * Bygger namnindexet en portion i taget. Samma namn finns ofta på många id:n (t.ex.
	 * quest-versioner); lägsta id vinner.
	 */
	static final class IndexBuilder
	{
		private final int count;
		private final java.util.function.IntFunction<String> names;
		private final List<ItemOption> options = new ArrayList<>();
		private final Set<String> seenNames = new HashSet<>();
		private int next;

		/**
		 * @param names föremålets namn, eller null om det inte ska vara med
		 */
		IndexBuilder(int count, java.util.function.IntFunction<String> names)
		{
			this.count = count;
			this.names = names;
		}

		/**
		 * @return true när alla föremål är genomgångna
		 */
		boolean step(int budget)
		{
			int end = Math.min(count, next + budget);
			for (; next < end; next++)
			{
				String name = names.apply(next);
				if (name != null && seenNames.add(name.toLowerCase(Locale.ROOT)))
				{
					options.add(new ItemOption(next, name));
				}
			}
			return next >= count;
		}

		List<ItemOption> result()
		{
			return Collections.unmodifiableList(options);
		}
	}

	static List<ItemOption> filter(List<ItemOption> index, String needle)
	{
		return SearchRanking.rank(index, ItemOption::getName, needle, MAX_RESULTS);
	}
}
