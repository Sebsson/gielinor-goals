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
package com.gielinorgoals.tracking;

import com.gielinorgoals.GielinorGoalsConfig;
import com.gielinorgoals.persistence.ContainerCache;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

/**
 * Räknar föremål i inventory och equipment (alltid läsbara) samt bank och seed vault,
 * som bara kan läsas när de är öppna och därför cachas per RuneScape-profil.
 * Alla metoder körs på klienttråden.
 */
@Singleton
public class ItemTracker
{
	static final String BANK_CACHE_KEY = "bankCache";
	static final String SEED_VAULT_CACHE_KEY = "seedVaultCache";

	private final Client client;
	private final ItemManager itemManager;
	private final ConfigManager configManager;
	private final Gson gson;

	private Map<Integer, Long> inventory = Collections.emptyMap();
	private Map<Integer, Long> worn = Collections.emptyMap();
	@Nullable
	private ContainerCache bank;
	@Nullable
	private ContainerCache seedVault;

	// Profilen som cacharna laddades från och sparas till.
	@Nullable
	private String cacheProfileKey;
	private boolean inventoryKnown;
	private boolean needFullRead;
	private boolean dirty;
	private boolean saveBank;
	private boolean saveSeedVault;

	@Inject
	ItemTracker(Client client, ItemManager itemManager, ConfigManager configManager, Gson gson)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.configManager = configManager;
		this.gson = gson;
	}

	public void reset(boolean loggedIn)
	{
		inventory = Collections.emptyMap();
		worn = Collections.emptyMap();
		bank = null;
		seedVault = null;
		cacheProfileKey = null;
		inventoryKnown = false;
		saveBank = false;
		saveSeedVault = false;
		needFullRead = loggedIn;
		dirty = true;
	}

	public void onGameStateChanged(GameState state)
	{
		if (state == GameState.LOGGED_IN)
		{
			needFullRead = true;
		}
		else if (state == GameState.LOGIN_SCREEN)
		{
			// Glöm förra kontots föremål så att de aldrig räknas för en ny profil.
			reset(false);
		}
	}

	public void onItemContainerChanged(int containerId, @Nullable ItemContainer container)
	{
		if (container == null)
		{
			return;
		}

		if (containerId == InventoryID.INV)
		{
			inventory = count(container);
			inventoryKnown = true;
		}
		else if (containerId == InventoryID.WORN)
		{
			worn = count(container);
		}
		else if (containerId == InventoryID.BANK)
		{
			bank = new ContainerCache(System.currentTimeMillis(), count(container));
			saveBank = true;
		}
		else if (containerId == InventoryID.SEED_VAULT)
		{
			seedVault = new ContainerCache(System.currentTimeMillis(), count(container));
			saveSeedVault = true;
		}
		else
		{
			return;
		}
		dirty = true;
	}

	/**
	 * Laddar cachar för nya profiler, sparar ändrade cachar och läser om allt efter inloggning.
	 *
	 * @return ny föremålsdata om något har ändrats sedan förra anropet, annars null
	 */
	@Nullable
	public GameSnapshot.Items onGameTick(@Nullable String profileKey)
	{
		if (profileKey != null && !profileKey.equals(cacheProfileKey))
		{
			loadCaches(profileKey);
		}

		if (needFullRead && client.getGameState() == GameState.LOGGED_IN)
		{
			onItemContainerChanged(InventoryID.INV, client.getItemContainer(InventoryID.INV));
			onItemContainerChanged(InventoryID.WORN, client.getItemContainer(InventoryID.WORN));
			// Inventory finns alltid när man är inloggad, även om containern inte skickats än.
			inventoryKnown = true;
			needFullRead = false;
			dirty = true;
		}

		saveCaches();

		if (!dirty)
		{
			return null;
		}
		dirty = false;

		if (!inventoryKnown)
		{
			return GameSnapshot.Items.UNKNOWN;
		}

		Map<Integer, Long> totals = sum(Arrays.asList(
			inventory,
			worn,
			bank == null ? Collections.emptyMap() : bank.getItems(),
			seedVault == null ? Collections.emptyMap() : seedVault.getItems()));
		return new GameSnapshot.Items(totals, bank == null ? null : bank.getSavedAt());
	}

	private void loadCaches(String profileKey)
	{
		cacheProfileKey = profileKey;
		// Har förrådet redan setts den här sessionen är det färskare än cachen.
		if (bank == null)
		{
			bank = ContainerCache.fromJson(gson,
				configManager.getConfiguration(GielinorGoalsConfig.GROUP, profileKey, BANK_CACHE_KEY));
		}
		if (seedVault == null)
		{
			seedVault = ContainerCache.fromJson(gson,
				configManager.getConfiguration(GielinorGoalsConfig.GROUP, profileKey, SEED_VAULT_CACHE_KEY));
		}
		dirty = true;
	}

	// Sparar högst en gång per tick, även om banken ändras många gånger.
	private void saveCaches()
	{
		if (cacheProfileKey == null)
		{
			return;
		}

		if (saveBank && bank != null)
		{
			configManager.setConfiguration(GielinorGoalsConfig.GROUP, cacheProfileKey, BANK_CACHE_KEY, bank.toJson(gson));
			saveBank = false;
		}
		if (saveSeedVault && seedVault != null)
		{
			configManager.setConfiguration(GielinorGoalsConfig.GROUP, cacheProfileKey, SEED_VAULT_CACHE_KEY,
				seedVault.toJson(gson));
			saveSeedVault = false;
		}
	}

	private Map<Integer, Long> count(ItemContainer container)
	{
		return count(container.getItems(), itemManager::canonicalize);
	}

	/**
	 * Summerar ett förråds föremål per grundföremål. Tomma platser och placeholders
	 * (antal 0) hoppas över; noterade föremål räknas om via {@code canonicalize}.
	 */
	static Map<Integer, Long> count(Item[] items, IntUnaryOperator canonicalize)
	{
		Map<Integer, Long> counts = new HashMap<>();
		for (Item item : items)
		{
			if (item == null || item.getId() < 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			counts.merge(canonicalize.applyAsInt(item.getId()), (long) item.getQuantity(), Long::sum);
		}
		return counts;
	}

	static Map<Integer, Long> sum(List<Map<Integer, Long>> sources)
	{
		Map<Integer, Long> totals = new HashMap<>();
		for (Map<Integer, Long> source : sources)
		{
			source.forEach((id, quantity) -> totals.merge(id, quantity, Long::sum));
		}
		return totals;
	}
}
