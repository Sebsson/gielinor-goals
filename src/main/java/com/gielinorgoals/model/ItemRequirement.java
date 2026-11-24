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
package com.gielinorgoals.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.util.QuantityFormatter;

/**
 * Ha ett visst antal av ett föremål. Räknas över inventory, equipment, bank och seed vault.
 * Med {@link #includeVariants} räknas även varianter enligt RuneLites variationstabell,
 * t.ex. laddade och oladdade versioner av samma föremål.
 * <p>
 * {@link #countsAlso} är föremål som redan har förbrukat målets föremål, t.ex. crystal legs som
 * gjordes av två crystal armour seeds: varje ägt exemplar räknas som {@code each} st.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class ItemRequirement extends Requirement
{
	public static final long MAX_QUANTITY = Integer.MAX_VALUE;

	private int itemId;
	// Namnet sparas så att titeln kan visas utan att fråga klienten.
	private String itemName;
	private long quantity;
	private boolean includeVariants;
	// Valfritt; null i äldre sparade mål och för mål utan motsvarigheter.
	@Nullable
	private List<CountsAlso> countsAlso;

	/**
	 * Ett föremål som räknas som {@code each} st av målets föremål, t.ex. crystal legs = 2 seeds.
	 */
	@Getter
	@EqualsAndHashCode
	public static final class CountsAlso
	{
		private int itemId;
		private long each;
		private boolean includeVariants;

		// För Gson.
		CountsAlso()
		{
		}

		public CountsAlso(int itemId, long each, boolean includeVariants)
		{
			if (itemId < 0)
			{
				throw new IllegalArgumentException("Invalid item id");
			}
			if (each < 1 || each > MAX_QUANTITY)
			{
				throw new IllegalArgumentException("Each must be between 1 and " + MAX_QUANTITY);
			}
			this.itemId = itemId;
			this.each = each;
			this.includeVariants = includeVariants;
		}

		boolean isValid()
		{
			return itemId >= 0 && each >= 1 && each <= MAX_QUANTITY;
		}

		Set<Integer> countedItemIds()
		{
			return ids(itemId, includeVariants);
		}
	}

	// För Gson.
	ItemRequirement()
	{
	}

	public ItemRequirement(int itemId, String itemName, long quantity, boolean includeVariants)
	{
		if (itemId < 0)
		{
			throw new IllegalArgumentException("Invalid item id");
		}
		if (quantity < 1 || quantity > MAX_QUANTITY)
		{
			throw new IllegalArgumentException("Quantity must be between 1 and " + MAX_QUANTITY);
		}
		this.itemId = itemId;
		this.itemName = itemName;
		this.quantity = quantity;
		this.includeVariants = includeVariants;
	}

	@Override
	public RequirementType getType()
	{
		return RequirementType.ITEM;
	}

	@Override
	public String describe()
	{
		String name = itemName == null || itemName.isEmpty() ? "Item " + itemId : itemName;
		return quantity == 1 ? name : QuantityFormatter.formatNumber(quantity) + " x " + name;
	}

	/**
	 * Samma mål med motsvarigheter som också räknas.
	 */
	public ItemRequirement withCountsAlso(@Nullable List<CountsAlso> countsAlso)
	{
		ItemRequirement copy = new ItemRequirement(itemId, itemName, quantity, includeVariants);
		copy.countsAlso = countsAlso == null || countsAlso.isEmpty() ? null : List.copyOf(countsAlso);
		return copy;
	}

	/**
	 * Motsvarigheterna; tom lista om det inte finns några.
	 */
	public List<CountsAlso> getCountsAlso()
	{
		return countsAlso == null ? Collections.emptyList() : countsAlso;
	}

	@Override
	public boolean isValid()
	{
		if (countsAlso != null)
		{
			for (CountsAlso also : countsAlso)
			{
				if (also == null || !also.isValid())
				{
					return false;
				}
			}
		}
		return itemId >= 0 && quantity >= 1 && quantity <= MAX_QUANTITY;
	}

	/**
	 * Föremåls-id som räknas mot målet.
	 */
	public Set<Integer> countedItemIds()
	{
		return ids(itemId, includeVariants);
	}

	private static Set<Integer> ids(int itemId, boolean includeVariants)
	{
		Set<Integer> ids = new LinkedHashSet<>();
		ids.add(itemId);
		if (includeVariants)
		{
			Collection<Integer> variations = ItemVariationMapping.getVariations(ItemVariationMapping.map(itemId));
			ids.addAll(variations);
		}
		return ids;
	}

	@Nullable
	@Override
	public GoalProgress progress(GameData data)
	{
		long count = count(countedItemIds(), data);
		if (count < 0)
		{
			return null;
		}
		// Varje ägd motsvarighet räknas som "each" st, t.ex. crystal legs som två seeds.
		for (CountsAlso also : getCountsAlso())
		{
			long owned = count(also.countedItemIds(), data);
			if (owned < 0)
			{
				return null;
			}
			count += owned * also.getEach();
		}

		String label = QuantityFormatter.quantityToStackSize(count) + "/" + QuantityFormatter.quantityToStackSize(quantity);
		return new GoalProgress(count, quantity, count >= quantity, label);
	}

	// Summan för id:na, eller -1 om något av dem inte är känt.
	private static long count(Set<Integer> ids, GameData data)
	{
		long count = 0;
		for (int id : ids)
		{
			long idCount = data.getItemCount(id);
			if (idCount < 0)
			{
				return -1;
			}
			count += idCount;
		}
		return count;
	}

	@Override
	public String describeProgress(GoalProgress progress)
	{
		long left = Math.max(0, progress.getTarget() - progress.getCurrent());
		return "You have " + QuantityFormatter.formatNumber(progress.getCurrent()) + " / "
			+ QuantityFormatter.formatNumber(progress.getTarget())
			+ (progress.isComplete() ? "" : "\n" + QuantityFormatter.formatNumber(left) + " left")
			+ (getCountsAlso().isEmpty() ? "" : "\nItems already made from them count too");
	}

	// Föremålets namn är bara för visning, och motsvarigheterna ändrar inte vad målet är.
	@Override
	public boolean isSameGoal(@Nullable Requirement other)
	{
		if (!(other instanceof ItemRequirement))
		{
			return false;
		}
		ItemRequirement item = (ItemRequirement) other;
		return item.itemId == itemId && item.quantity == quantity && item.includeVariants == includeVariants;
	}
}
