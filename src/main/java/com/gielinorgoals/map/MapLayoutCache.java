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

import com.gielinorgoals.model.GoalTree;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Håller senaste layouten och räknar om den bara när målen eller vyn har ändrats.
 * Målen jämförs via GoalManagers revision, som räknas upp vid varje sparning (även när
 * ett mål klaras), så målning och hovring återanvänder samma layout. Används bara på EDT.
 */
public final class MapLayoutCache
{
	@Nullable
	private GoalTree tree;
	private long revision = Long.MIN_VALUE;
	@Nullable
	private MapView view;
	@Nullable
	private MapGraph graph;
	@Nullable
	private MapLayout layout;
	private int computations;

	public MapLayout get(GoalTree tree, long revision, MapView view)
	{
		if (layout == null || this.tree != tree || this.revision != revision || !Objects.equals(this.view, view))
		{
			graph = MapGraph.build(tree, view);
			layout = LayeredLayout.compute(graph);
			this.tree = tree;
			this.revision = revision;
			this.view = view;
			computations++;
		}
		return layout;
	}

	/**
	 * Grafen som hör till senaste layouten, t.ex. för ihopfällda noder och grannar vid hovring.
	 */
	@Nullable
	public MapGraph getGraph()
	{
		return graph;
	}

	// För tester: hur många gånger layouten har räknats fram.
	int getComputations()
	{
		return computations;
	}
}
