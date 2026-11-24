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
package com.gielinorgoals.template;

import com.gielinorgoals.GielinorGoalsConfig;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Användarens egna templates. De sparas globalt i pluginets config (utan RS-profil), så att
 * de finns på alla konton, i samma format som de handplockade. Bara EDT.
 */
@Slf4j
@Singleton
public class CustomTemplates
{
	public static final String CONFIG_KEY = "customTemplates";
	static final String CORRUPT_BACKUP_KEY = "customTemplatesCorrupt";
	public static final String ID_PREFIX = "custom:";
	public static final int MAX_TEMPLATES = 100;
	public static final int MAX_NODES = 2000;
	// Hela den sparade texten, alla templates tillsammans.
	public static final int MAX_TOTAL_CHARS = 1_000_000;
	public static final int MAX_NAME_LENGTH = 80;
	public static final int MAX_DESCRIPTION_LENGTH = 300;
	public static final int MAX_TAGS = 20;
	public static final int MAX_TAG_LENGTH = 40;

	/**
	 * Var templates sparas; i pluginet config utan profil, i tester ett fält.
	 */
	public interface Storage
	{
		@Nullable
		String get();

		void set(String value);

		// Det som inte gick att läsa sparas undan innan något kan skriva över det.
		default void setBackup(String value)
		{
		}
	}

	/**
	 * Ett fel som visas för användaren, t.ex. att gränsen för antal templates är nådd.
	 */
	public static final class TemplateException extends RuntimeException
	{
		public TemplateException(String message)
		{
			super(message);
		}
	}

	// Fälten fylls av Gson.
	private static final class StoredFile
	{
		Integer formatVersion;
		List<JsonTemplate.FileSpec> templates;
	}

	private final Storage storage;
	private final Gson gson;
	private final List<JsonTemplate.FileSpec> specs = new ArrayList<>();
	private final List<JsonTemplate> templates = new ArrayList<>();
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

	@Inject
	CustomTemplates(ConfigManager configManager, Gson gson)
	{
		this(new Storage()
		{
			@Override
			public String get()
			{
				return configManager.getConfiguration(GielinorGoalsConfig.GROUP, CONFIG_KEY);
			}

			@Override
			public void set(String value)
			{
				configManager.setConfiguration(GielinorGoalsConfig.GROUP, CONFIG_KEY, value);
			}

			@Override
			public void setBackup(String value)
			{
				configManager.setConfiguration(GielinorGoalsConfig.GROUP, CORRUPT_BACKUP_KEY, value);
			}
		}, gson);
	}

	public CustomTemplates(Storage storage, Gson gson)
	{
		this.storage = storage;
		this.gson = gson;
		load();
	}

	/**
	 * Utan lagring utanför minnet; för tester och kataloger utan klient.
	 */
	public static CustomTemplates inMemory(Gson gson)
	{
		String[] value = {null};
		return new CustomTemplates(new Storage()
		{
			@Override
			public String get()
			{
				return value[0];
			}

			@Override
			public void set(String stored)
			{
				value[0] = stored;
			}
		}, gson);
	}

	public void addListener(Runnable listener)
	{
		listeners.add(listener);
	}

	public void removeListener(Runnable listener)
	{
		listeners.remove(listener);
	}

	public List<JsonTemplate> getTemplates()
	{
		return Collections.unmodifiableList(new ArrayList<>(templates));
	}

	public static boolean isCustomId(@Nullable String id)
	{
		return id != null && id.startsWith(ID_PREFIX);
	}

	@Nullable
	public JsonTemplate find(@Nullable String id)
	{
		int index = indexOf(id);
		return index < 0 ? null : templates.get(index);
	}

	/**
	 * Sparar en ny template.
	 *
	 * @return den nya templatens id, {@code custom:<uuid>}
	 * @throws TemplateException om en gräns överskrids eller templaten är ogiltig
	 */
	public String add(TemplateExport.Result export)
	{
		return add(export.getSpec());
	}

	/**
	 * Skriver över en befintlig template med en ny export av målen.
	 */
	public void replace(String id, TemplateExport.Result export)
	{
		replace(id, export.getSpec());
	}

	String add(JsonTemplate.FileSpec spec)
	{
		if (specs.size() >= MAX_TEMPLATES)
		{
			throw new TemplateException("You already have " + MAX_TEMPLATES
				+ " templates, the most there can be. Delete one first.");
		}
		JsonTemplate.FileSpec copy = copy(spec);
		copy.id = ID_PREFIX + UUID.randomUUID();
		List<JsonTemplate.FileSpec> next = new ArrayList<>(specs);
		next.add(0, copy);
		commit(next);
		return copy.id;
	}

	/**
	 * Skriver över en befintlig template; id:t behålls, så att insatta mål kan uppdateras.
	 */
	void replace(String id, JsonTemplate.FileSpec spec)
	{
		int index = requireIndex(id);
		JsonTemplate.FileSpec copy = copy(spec);
		copy.id = id;
		List<JsonTemplate.FileSpec> next = new ArrayList<>(specs);
		next.set(index, copy);
		commit(next);
	}

	/**
	 * Byter namn, beskrivning och taggar; noderna är oförändrade.
	 */
	public void rename(String id, String name, String description, List<String> tags)
	{
		JsonTemplate.FileSpec copy = copy(specs.get(requireIndex(id)));
		copy.name = name;
		copy.description = description;
		copy.tags = new ArrayList<>(tags);
		replace(id, copy);
	}

	/**
	 * @return kopians id
	 */
	public String duplicate(String id)
	{
		JsonTemplate.FileSpec copy = copy(specs.get(requireIndex(id)));
		copy.name = truncate(copy.name + " (copy)", MAX_NAME_LENGTH);
		return add(copy);
	}

	/**
	 * Tar bort templaten. Mål som redan satts in från den ligger kvar; de kan bara inte
	 * uppdateras från den längre.
	 */
	public void delete(String id)
	{
		List<JsonTemplate.FileSpec> next = new ArrayList<>(specs);
		next.remove(requireIndex(id));
		commit(next);
	}

	/**
	 * En kopia av templatens data, t.ex. för att exportera den.
	 */
	@Nullable
	JsonTemplate.FileSpec spec(String id)
	{
		int index = indexOf(id);
		return index < 0 ? null : copy(specs.get(index));
	}

	/**
	 * Kontrollerar en template mot gränserna och formatet utan att spara den.
	 *
	 * @throws TemplateException med vad som var fel
	 */
	static void validate(JsonTemplate.FileSpec spec)
	{
		String name = spec.name == null ? "" : spec.name.trim();
		if (name.isEmpty())
		{
			throw new TemplateException("Give the template a name.");
		}
		if (name.length() > MAX_NAME_LENGTH)
		{
			throw new TemplateException("The name can be at most " + MAX_NAME_LENGTH + " characters.");
		}
		if (spec.description != null && spec.description.length() > MAX_DESCRIPTION_LENGTH)
		{
			throw new TemplateException("The description can be at most " + MAX_DESCRIPTION_LENGTH + " characters.");
		}
		if (spec.tags != null)
		{
			if (spec.tags.size() > MAX_TAGS)
			{
				throw new TemplateException("A template can have at most " + MAX_TAGS + " tags.");
			}
			for (String tag : spec.tags)
			{
				if (tag == null || tag.length() > MAX_TAG_LENGTH)
				{
					throw new TemplateException("A tag can be at most " + MAX_TAG_LENGTH + " characters.");
				}
			}
		}
		int nodes = countNodes(spec.root);
		if (nodes > MAX_NODES)
		{
			throw new TemplateException("The template has " + nodes + " goals; the most is " + MAX_NODES + ".");
		}
		try
		{
			JsonTemplate.from(spec);
		}
		catch (IllegalArgumentException e)
		{
			throw new TemplateException("The template is not valid: " + e.getMessage());
		}
	}

	static int countNodes(@Nullable JsonTemplate.NodeSpec node)
	{
		if (node == null)
		{
			return 0;
		}
		int count = 1;
		if (node.children != null)
		{
			for (JsonTemplate.NodeSpec child : node.children)
			{
				count += countNodes(child);
			}
		}
		return count;
	}

	// Kontrollerar, sparar och läser sedan in listan igen som den ser ut sparad.
	private void commit(List<JsonTemplate.FileSpec> next)
	{
		List<JsonTemplate> built = new ArrayList<>();
		for (JsonTemplate.FileSpec spec : next)
		{
			validate(spec);
			built.add(build(spec));
		}
		StoredFile file = new StoredFile();
		file.formatVersion = JsonTemplate.FORMAT_VERSION;
		file.templates = next;
		String json = gson.toJson(file);
		if (json.length() > MAX_TOTAL_CHARS)
		{
			throw new TemplateException("Your templates would take up too much space together."
				+ " Delete or shrink a template first.");
		}
		storage.set(json);
		specs.clear();
		specs.addAll(next);
		templates.clear();
		templates.addAll(built);
		listeners.forEach(Runnable::run);
	}

	private void load()
	{
		String json = storage.get();
		if (json == null || json.isEmpty())
		{
			return;
		}
		try
		{
			StoredFile file = gson.fromJson(json, StoredFile.class);
			if (file == null || file.templates == null)
			{
				return;
			}
			for (JsonTemplate.FileSpec spec : file.templates)
			{
				try
				{
					if (spec == null || !isCustomId(spec.id))
					{
						throw new IllegalArgumentException("Missing id");
					}
					JsonTemplate template = build(spec);
					specs.add(spec);
					templates.add(template);
				}
				catch (IllegalArgumentException e)
				{
					// En trasig template (t.ex. från en nyare version) hoppas över men finns kvar i backupen.
					log.warn("Skipping a custom template that could not be read: {}", e.getMessage());
					storage.setBackup(json);
				}
			}
		}
		catch (JsonParseException e)
		{
			log.warn("Could not read custom templates, keeping a backup", e);
			storage.setBackup(json);
		}
	}

	private JsonTemplate build(JsonTemplate.FileSpec spec)
	{
		JsonTemplate.FileSpec copy = copy(spec);
		copy.category = TemplateCategories.MINE;
		return JsonTemplate.from(copy);
	}

	private JsonTemplate.FileSpec copy(JsonTemplate.FileSpec spec)
	{
		return gson.fromJson(gson.toJson(spec), JsonTemplate.FileSpec.class);
	}

	private int indexOf(@Nullable String id)
	{
		for (int i = 0; i < specs.size(); i++)
		{
			if (specs.get(i).id.equals(id))
			{
				return i;
			}
		}
		return -1;
	}

	private int requireIndex(String id)
	{
		int index = indexOf(id);
		if (index < 0)
		{
			throw new TemplateException("The template no longer exists.");
		}
		return index;
	}

	private static String truncate(String text, int max)
	{
		return text.length() <= max ? text : text.substring(0, max);
	}
}
