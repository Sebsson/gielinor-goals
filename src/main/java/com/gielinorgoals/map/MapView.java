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
package com.gielinorgoals.map;

import com.gielinorgoals.model.Goal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Vad kartan visar: omfånget (en rot eller alla mål), vilka noder som är utfällda och om
 * klara mål döljs. Värdeobjekt, så att en ändrad vy ger en ny layout och en oförändrad vy
 * återanvänder den gamla.
 *
 * <p>Utfällningen är kartans egen och påverkar inte panelens {@code expanded}. Som standard
 * är aggregat ihopfällda och allt annat utfällt; omfångets rot är alltid utfälld från
 * början, annars skulle t.ex. en template-rot visas som en ensam nod.
 */
@Getter
@EqualsAndHashCode
public final class MapView
{
	// Null betyder "Alla mål".
	@Nullable
	private final String rootId;
	private final boolean hideCompleted;
	// Användarens egna val per mål; saknas ett mål gäller standarden ovan.
	private final Map<String, Boolean> expanded;

	private MapView(@Nullable String rootId, boolean hideCompleted, Map<String, Boolean> expanded)
	{
		this.rootId = rootId;
		this.hideCompleted = hideCompleted;
		this.expanded = Collections.unmodifiableMap(expanded);
	}

	public static MapView allGoals()
	{
		return new MapView(null, false, Map.of());
	}

	public static MapView of(@Nullable String rootId)
	{
		return new MapView(rootId, false, Map.of());
	}

	/**
	 * Annat omfång; utfällningen och "Dölj klara" behålls.
	 */
	public MapView withRoot(@Nullable String rootId)
	{
		return new MapView(rootId, hideCompleted, new HashMap<>(expanded));
	}

	public MapView withHideCompleted(boolean hide)
	{
		return new MapView(rootId, hide, new HashMap<>(expanded));
	}

	/**
	 * Fäller ut eller ihop ett mål på kartan.
	 */
	public MapView withExpanded(String goalId, boolean value)
	{
		Map<String, Boolean> copy = new HashMap<>(expanded);
		copy.put(goalId, value);
		return new MapView(rootId, hideCompleted, copy);
	}

	/**
	 * @param scopeRoot om målet är en av omfångets rötter
	 */
	public boolean isExpanded(Goal goal, boolean scopeRoot)
	{
		Boolean own = expanded.get(goal.getId());
		if (own != null)
		{
			return own;
		}
		return scopeRoot || !goal.isAggregate();
	}
}
