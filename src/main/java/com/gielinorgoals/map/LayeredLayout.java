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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lagerlayout för en DAG, en förenklad Sugiyama:
 * <ol>
 * <li>Lager = längsta vägen från en rot, så att varje delmål hamnar under alla sina föräldrar.</li>
 * <li>Ordning inom lagret med barycenter-metoden: några svep ner och upp där varje nod
 * sorteras efter medelpositionen hos föräldrarna respektive barnen. Den ordning som ger
 * minst korsningar mellan intilliggande lager behålls.</li>
 * <li>Koordinater med jämnt avstånd, där föräldrar flyttas mot mitten av sina barn och
 * barn mot sina föräldrar, utan att noder i samma lager överlappar.</li>
 * </ol>
 * Kanter över flera lager får inga dummy-noder; kartan ritar dem som kurvor. Allt är
 * deterministiskt: samma graf ger samma koordinater.
 */
public final class LayeredLayout
{
	public static final double NODE_WIDTH = 190;
	public static final double NODE_HEIGHT = 54;
	public static final double H_GAP = 26;
	public static final double V_GAP = 64;

	private static final int ORDER_SWEEPS = 6;
	private static final int POSITION_PASSES = 4;

	private final MapGraph graph;
	private final Map<String, Integer> index = new HashMap<>();
	private final Map<String, Integer> layerOf = new HashMap<>();
	private final List<List<String>> layers = new ArrayList<>();
	private final Map<String, Double> x = new HashMap<>();

	private LayeredLayout(MapGraph graph)
	{
		this.graph = graph;
		List<Goal> nodes = graph.getNodes();
		for (int i = 0; i < nodes.size(); i++)
		{
			index.put(nodes.get(i).getId(), i);
		}
	}

	public static MapLayout compute(MapGraph graph)
	{
		LayeredLayout layout = new LayeredLayout(graph);
		layout.assignLayers();
		layout.orderLayers();
		layout.assignX();
		return layout.result();
	}

	// Kahn i upptäcktsordning: varje nod får lagret efter sin djupaste förälder.
	private void assignLayers()
	{
		Map<String, Integer> remaining = new HashMap<>();
		Deque<String> ready = new ArrayDeque<>();
		for (Goal goal : graph.getNodes())
		{
			int parents = graph.parentsOf(goal.getId()).size();
			remaining.put(goal.getId(), parents);
			layerOf.put(goal.getId(), 0);
			if (parents == 0)
			{
				ready.add(goal.getId());
			}
		}
		while (!ready.isEmpty())
		{
			String id = ready.poll();
			int layer = layerOf.get(id);
			for (String child : graph.childrenOf(id))
			{
				layerOf.put(child, Math.max(layerOf.get(child), layer + 1));
				if (remaining.merge(child, -1, Integer::sum) == 0)
				{
					ready.add(child);
				}
			}
		}

		// Startordning inom lagren: upptäcktsordningen.
		for (Goal goal : graph.getNodes())
		{
			int layer = layerOf.get(goal.getId());
			while (layers.size() <= layer)
			{
				layers.add(new ArrayList<>());
			}
			layers.get(layer).add(goal.getId());
		}
	}

	private void orderLayers()
	{
		List<List<String>> best = copy(layers);
		long bestCrossings = crossings();
		for (int sweep = 0; sweep < ORDER_SWEEPS && bestCrossings > 0; sweep++)
		{
			boolean down = sweep % 2 == 0;
			if (down)
			{
				for (int l = 1; l < layers.size(); l++)
				{
					sortByBarycenter(l, true);
				}
			}
			else
			{
				for (int l = layers.size() - 2; l >= 0; l--)
				{
					sortByBarycenter(l, false);
				}
			}
			long now = crossings();
			if (now < bestCrossings)
			{
				bestCrossings = now;
				best = copy(layers);
			}
		}
		layers.clear();
		layers.addAll(best);
	}

	// Medelpositionen hos grannarna i alla andra lager, centrerad per lager så att lager
	// med olika bredd går att jämföra. Utan grannar behåller noden sin plats.
	private void sortByBarycenter(int layer, boolean useParents)
	{
		List<String> nodes = layers.get(layer);
		Map<String, Double> centered = centeredPositions();
		Map<String, Double> key = new HashMap<>();
		for (String id : nodes)
		{
			List<String> neighbours = useParents ? graph.parentsOf(id) : graph.childrenOf(id);
			if (neighbours.isEmpty())
			{
				key.put(id, centered.get(id));
				continue;
			}
			double sum = 0;
			for (String neighbour : neighbours)
			{
				sum += centered.get(neighbour);
			}
			key.put(id, sum / neighbours.size());
		}
		Map<String, Integer> before = positions(nodes);
		// Stabil sortering: lika värden behåller sin tidigare ordning.
		nodes.sort(Comparator.<String>comparingDouble(key::get).thenComparingInt(before::get));
	}

	private Map<String, Double> centeredPositions()
	{
		Map<String, Double> centered = new HashMap<>();
		for (List<String> layer : layers)
		{
			double middle = (layer.size() - 1) / 2.0;
			for (int i = 0; i < layer.size(); i++)
			{
				centered.put(layer.get(i), i - middle);
			}
		}
		return centered;
	}

	private static Map<String, Integer> positions(List<String> layer)
	{
		Map<String, Integer> positions = new HashMap<>();
		for (int i = 0; i < layer.size(); i++)
		{
			positions.put(layer.get(i), i);
		}
		return positions;
	}

	// Korsningar mellan kanter som börjar i samma lager; kanter över flera lager räknas
	// med sin slutposition, vilket räcker som mått för att jämföra ordningar.
	private long crossings()
	{
		Map<String, Double> centered = centeredPositions();
		long total = 0;
		for (List<String> layer : layers)
		{
			List<double[]> edges = new ArrayList<>();
			for (String from : layer)
			{
				for (String to : graph.childrenOf(from))
				{
					edges.add(new double[]{centered.get(from), centered.get(to), layerOf.get(to)});
				}
			}
			for (int i = 0; i < edges.size(); i++)
			{
				for (int j = i + 1; j < edges.size(); j++)
				{
					double[] a = edges.get(i);
					double[] b = edges.get(j);
					if (a[2] == b[2] && (a[0] - b[0]) * (a[1] - b[1]) < 0)
					{
						total++;
					}
				}
			}
		}
		return total;
	}

	private void assignX()
	{
		double pitch = NODE_WIDTH + H_GAP;
		for (List<String> layer : layers)
		{
			double middle = (layer.size() - 1) / 2.0;
			for (int i = 0; i < layer.size(); i++)
			{
				x.put(layer.get(i), (i - middle) * pitch);
			}
		}
		for (int pass = 0; pass < POSITION_PASSES; pass++)
		{
			for (int l = 1; l < layers.size(); l++)
			{
				place(layers.get(l), true, pitch);
			}
			// Sista svepet går uppåt, så att föräldrar hamnar centrerade över sina barn.
			for (int l = layers.size() - 2; l >= 0; l--)
			{
				place(layers.get(l), false, pitch);
			}
		}
	}

	// Flyttar noderna mot sina grannars mitt men behåller ordningen och minsta avståndet;
	// hela lagret förskjuts sedan så att avvikelsen från önskat läge blir noll i snitt.
	private void place(List<String> layer, boolean towardsParents, double pitch)
	{
		double[] desired = new double[layer.size()];
		for (int i = 0; i < layer.size(); i++)
		{
			String id = layer.get(i);
			List<String> neighbours = towardsParents ? graph.parentsOf(id) : graph.childrenOf(id);
			if (neighbours.isEmpty())
			{
				desired[i] = x.get(id);
				continue;
			}
			double sum = 0;
			for (String neighbour : neighbours)
			{
				sum += x.get(neighbour);
			}
			desired[i] = sum / neighbours.size();
		}

		double[] placed = new double[layer.size()];
		for (int i = 0; i < layer.size(); i++)
		{
			placed[i] = i == 0 ? desired[i] : Math.max(desired[i], placed[i - 1] + pitch);
		}
		double shift = 0;
		for (int i = 0; i < layer.size(); i++)
		{
			shift += desired[i] - placed[i];
		}
		shift /= Math.max(1, layer.size());
		for (int i = 0; i < layer.size(); i++)
		{
			x.put(layer.get(i), placed[i] + shift);
		}
	}

	private MapLayout result()
	{
		double minX = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		for (double value : x.values())
		{
			minX = Math.min(minX, value);
			maxX = Math.max(maxX, value);
		}
		if (x.isEmpty())
		{
			minX = 0;
			maxX = 0;
		}

		List<MapLayout.Node> nodes = new ArrayList<>();
		Map<String, MapLayout.Node> byId = new HashMap<>();
		for (int l = 0; l < layers.size(); l++)
		{
			List<String> layer = layers.get(l);
			for (int i = 0; i < layer.size(); i++)
			{
				String id = layer.get(i);
				MapLayout.Node node = new MapLayout.Node(id, round(x.get(id) - minX), l * (NODE_HEIGHT + V_GAP),
					NODE_WIDTH, NODE_HEIGHT, l, i);
				nodes.add(node);
				byId.put(id, node);
			}
		}

		List<MapLayout.Edge> edges = new ArrayList<>();
		for (Goal goal : graph.getNodes())
		{
			MapLayout.Node from = byId.get(goal.getId());
			for (String childId : graph.childrenOf(goal.getId()))
			{
				MapLayout.Node to = byId.get(childId);
				edges.add(new MapLayout.Edge(from.getGoalId(), to.getGoalId(), from.centerX(), from.getY() + from.getHeight(),
					to.centerX(), to.getY(), to.getLayer() - from.getLayer()));
			}
		}

		double width = x.isEmpty() ? 0 : maxX - minX + NODE_WIDTH;
		double height = layers.isEmpty() ? 0 : layers.size() * (NODE_HEIGHT + V_GAP) - V_GAP;
		return new MapLayout(nodes, edges, byId, width, height);
	}

	// Hela pixlar ger skarpa kanter och gör jämförelser i tester exakta.
	private static double round(double value)
	{
		return Math.round(value);
	}

	private static List<List<String>> copy(List<List<String>> layers)
	{
		List<List<String>> copy = new ArrayList<>();
		layers.forEach(layer -> copy.add(new ArrayList<>(layer)));
		return copy;
	}
}
