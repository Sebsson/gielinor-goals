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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Value;

/**
 * Färdig layout för kartan: en ruta per mål och en kant per förälder-barn-par, i
 * kartkoordinater (pixlar vid zoom 1). Räknas fram av {@link LayeredLayout} och ändras
 * aldrig; målningen läser bara.
 */
@Getter
public final class MapLayout
{
	@Value
	public static class Node
	{
		String goalId;
		// Övre vänstra hörnet.
		double x;
		double y;
		double width;
		double height;
		int layer;
		// Plats i lagret från vänster, 0-baserad.
		int order;

		public double centerX()
		{
			return x + width / 2;
		}
	}

	/**
	 * Från förälderns nederkant till barnets överkant. Kanter som går över flera lager
	 * ({@code span} > 1) ritas som mjuka kurvor.
	 */
	@Value
	public static class Edge
	{
		String fromId;
		String toId;
		double x1;
		double y1;
		double x2;
		double y2;
		int span;
	}

	private final List<Node> nodes;
	private final List<Edge> edges;
	private final Map<String, Node> byId;
	private final double width;
	private final double height;

	MapLayout(List<Node> nodes, List<Edge> edges, Map<String, Node> byId, double width, double height)
	{
		this.nodes = Collections.unmodifiableList(nodes);
		this.edges = Collections.unmodifiableList(edges);
		this.byId = Collections.unmodifiableMap(byId);
		this.width = width;
		this.height = height;
	}

	@Nullable
	public Node node(String goalId)
	{
		return byId.get(goalId);
	}
}
