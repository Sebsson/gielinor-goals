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

	@Override
	public boolean isValid()
	{
		return itemId >= 0 && quantity >= 1 && quantity <= MAX_QUANTITY;
	}

	/**
	 * Föremåls-id som räknas mot målet.
	 */
	public Set<Integer> countedItemIds()
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
		long count = 0;
		for (int id : countedItemIds())
		{
			long idCount = data.getItemCount(id);
			if (idCount < 0)
			{
				return null;
			}
			count += idCount;
		}

		String label = QuantityFormatter.quantityToStackSize(count) + "/" + QuantityFormatter.quantityToStackSize(quantity);
		return new GoalProgress(count, quantity, count >= quantity, label);
	}

	@Override
	public String describeProgress(GoalProgress progress)
	{
		long left = Math.max(0, progress.getTarget() - progress.getCurrent());
		return "You have " + QuantityFormatter.formatNumber(progress.getCurrent()) + " / "
			+ QuantityFormatter.formatNumber(progress.getTarget())
			+ (progress.isComplete() ? "" : "\n" + QuantityFormatter.formatNumber(left) + " left");
	}
}
