package com.gielinorgoals.template;

import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.PointsActivity;
import com.gielinorgoals.model.PointsRequirement;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Skilling-outfits och kappor i våg 2 (milstolpe 13, del 3).
 */
public class OutfitTemplatesTest
{
	private final TemplateCatalog catalog = new TemplateCatalog(new Gson());

	@Test
	public void itemIdsMatchGameval()
	{
		Map<String, List<Integer>> expected = new HashMap<>();
		expected.put("angler", List.of(ItemID.AERIAL_FISHING_PEARL, ItemID.TRAWLER_REWARD_HAT, ItemID.TRAWLER_REWARD_TOP,
			ItemID.TRAWLER_REWARD_LEGS, ItemID.TRAWLER_REWARD_BOOTS));
		expected.put("prospector", List.of(ItemID.MOTHERLODE_NUGGET, ItemID.MOTHERLODE_REWARD_HAT,
			ItemID.MOTHERLODE_REWARD_TOP, ItemID.MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_BOOTS));
		expected.put("lumberjack", List.of(ItemID.FORESTRY_CURRENCY, ItemID.MAPLE_LOGS, ItemID.YEW_LOGS, ItemID.MAGIC_LOGS,
			ItemID.REDWOOD_LOGS, ItemID.RAMBLE_LUMBERJACK_HAT, ItemID.RAMBLE_LUMBERJACK_TOP, ItemID.RAMBLE_LUMBERJACK_LEGS,
			ItemID.RAMBLE_LUMBERJACK_BOOTS));
		expected.put("pyromancer", List.of(ItemID.PYROMANCER_HOOD, ItemID.PYROMANCER_TOP, ItemID.PYROMANCER_BOTTOM,
			ItemID.PYROMANCER_BOOTS));
		expected.put("rogue", List.of(ItemID.ROGUESDEN_HELM, ItemID.ROGUESDEN_BODY, ItemID.ROGUESDEN_GLOVES,
			ItemID.ROGUESDEN_LEGS, ItemID.ROGUESDEN_BOOTS));
		expected.put("farmers-outfit", List.of(ItemID.TITHE_REWARD_HAT_MALE, ItemID.TITHE_REWARD_TORSO_MALE,
			ItemID.TITHE_REWARD_LEGS_MALE, ItemID.TITHE_REWARD_FEET_MALE));
		expected.put("carpenters-outfit", List.of(ItemID.CONSTRUCTION_HAT, ItemID.CONSTRUCTION_SHIRT,
			ItemID.CONSTRUCTION_TROUSERS, ItemID.CONSTRUCTION_BOOTS));
		expected.put("smiths-uniform", List.of(ItemID.SMITHING_UNIFORM_TORSO, ItemID.SMITHING_UNIFORM_LEGS,
			ItemID.SMITHING_UNIFORM_GLOVES, ItemID.SMITHING_UNIFORM_BOOTS));
		expected.put("spirit-angler", List.of(ItemID.SPIRIT_FLAKES, ItemID.SPIRIT_ANGLER_HAT, ItemID.TRAWLER_REWARD_HAT,
			ItemID.SPIRIT_ANGLER_TOP, ItemID.TRAWLER_REWARD_TOP, ItemID.SPIRIT_ANGLER_LEGS, ItemID.TRAWLER_REWARD_LEGS,
			ItemID.SPIRIT_ANGLER_BOOTS, ItemID.TRAWLER_REWARD_BOOTS));
		expected.put("dizanas-quiver", List.of(ItemID.DIZANAS_QUIVER_UNCHARGED, ItemID.SUNFIRESPLINTER,
			ItemID.DIZANAS_QUIVER_INFINITE));
		expected.put("mythical-cape", List.of(ItemID.COINS, ItemID.MYTHICAL_CAPE));
		expected.put("champions-cape", List.of(ItemID.CHAMPIONS_CHALLENGE_IMP, ItemID.CHAMPIONS_CHALLENGE_GOBLIN,
			ItemID.CHAMPIONS_CHALLENGE_SKELETON, ItemID.CHAMPIONS_CHALLENGE_ZOMBIE, ItemID.CHAMPIONS_CHALLENGE_GIANT,
			ItemID.CHAMPIONS_CHALLENGE_HOBGOBLIN, ItemID.CHAMPIONS_CHALLENGE_GHOUL, ItemID.CHAMPIONS_CHALLENGE_EARTHWARRIOR,
			ItemID.CHAMPIONS_CHALLENGE_JOGRE, ItemID.CHAMPIONS_CHALLENGE_LESSERDEMON, ItemID.CHAMPION_CAPE));

		for (Map.Entry<String, List<Integer>> entry : expected.entrySet())
		{
			GoalTemplate template = catalog.find(entry.getKey());
			assertTrue(entry.getKey(), template != null);
			List<Integer> items = new ArrayList<>();
			collect(template.build(Map.of()), node ->
			{
				if (node.getRequirement() instanceof ItemRequirement)
				{
					items.add(((ItemRequirement) node.getRequirement()).getItemId());
				}
			});
			assertEquals(entry.getKey(), entry.getValue(), items);
		}
	}

	@Test
	public void categoriesAndCosts()
	{
		for (String id : List.of("angler", "prospector", "lumberjack", "pyromancer", "rogue", "farmers-outfit",
			"carpenters-outfit", "smiths-uniform", "spirit-angler"))
		{
			assertEquals(id, TemplateCategories.SKILLING_OUTFITS, catalog.find(id).getCategory());
		}
		for (String id : List.of("dizanas-quiver", "mythical-cape", "champions-cape"))
		{
			assertEquals(id, TemplateCategories.CAPES, catalog.find(id).getCategory());
		}
		assertTrue(requirements("farmers-outfit").contains(new PointsRequirement(PointsActivity.TITHE_FARM, 400)));
		assertTrue(requirements("smiths-uniform").contains(new PointsRequirement(PointsActivity.GIANTS_FOUNDRY, 15000)));
		assertTrue(requirements("carpenters-outfit").contains(KillCountRequirement.manual("Carpenter points", 2000)));
		ItemRequirement nuggets = new ItemRequirement(ItemID.MOTHERLODE_NUGGET, "Golden nugget", 180, false);
		assertTrue(requirements("prospector").stream().anyMatch(r -> nuggets.isSameGoal((com.gielinorgoals.model.Requirement) r)));
		ItemRequirement flakes = new ItemRequirement(ItemID.SPIRIT_FLAKES, "Spirit flakes", 4800, false);
		assertTrue(requirements("spirit-angler").stream().anyMatch(r -> flakes.isSameGoal((com.gielinorgoals.model.Requirement) r)));
		assertTrue(requirements("dizanas-quiver").contains(new QuestRequirement(Quest.CHILDREN_OF_THE_SUN.getId(),
			Quest.CHILDREN_OF_THE_SUN.getName())));
		assertTrue(requirements("mythical-cape").contains(new QuestRequirement(Quest.DRAGON_SLAYER_II.getId(),
			Quest.DRAGON_SLAYER_II.getName())));
		assertTrue(requirements("champions-cape").contains(new QuestPointsRequirement(32)));
	}

	@Test
	public void dropSourcesAreSet()
	{
		List<TemplateNode> drops = new ArrayList<>();
		collect(catalog.find("champions-cape").build(Map.of()), node ->
		{
			if (node.getDropSource() != null)
			{
				drops.add(node);
			}
		});
		assertEquals(10, drops.size());
		drops.forEach(node -> assertEquals(5000, node.getDropSource().getRate()));

		List<TemplateNode> trawler = new ArrayList<>();
		collect(catalog.find("angler").build(Map.of()), node ->
		{
			if (node.getDropSource() != null)
			{
				trawler.add(node);
			}
		});
		assertEquals(4, trawler.size());
		assertEquals("manual:fishing trawler games", trawler.get(0).getDropSource().counterKey());
	}

	@Test
	public void spiritAnglerSharesTheAnglerPieces()
	{
		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, catalog.find("angler").build(Map.of()));
		TemplateInserter.Result spirit = TemplateInserter.insert(tree, catalog.find("spirit-angler").build(Map.of()));
		assertEquals(4, spirit.getLinked().size());
		for (int id : new int[]{ItemID.TRAWLER_REWARD_HAT, ItemID.TRAWLER_REWARD_TOP, ItemID.TRAWLER_REWARD_LEGS,
			ItemID.TRAWLER_REWARD_BOOTS})
		{
			List<Goal> found = new ArrayList<>();
			for (Goal goal : tree.flatten())
			{
				if (("item:" + id).equals(TemplateKeys.of(goal)))
				{
					found.add(goal);
				}
			}
			assertEquals(1, found.size());
			assertEquals(2, tree.getParents(found.get(0).getId()).size());
		}
	}

	@Test
	public void nicknamesFindTheTemplates()
	{
		Map<String, String> nicknames = new HashMap<>();
		nicknames.put("mlm", "Prospector kit");
		nicknames.put("wt", "Pyromancer outfit");
		nicknames.put("gf", "Smiths' uniform");
		nicknames.put("mh", "Carpenter's outfit");
		nicknames.put("trawler", "Angler's outfit");
		nicknames.put("forestry", "Lumberjack outfit");
		nicknames.put("quiver", "Dizana's quiver");
		nicknames.put("ds2", "Mythical cape");
		nicknames.put("champion scrolls", "Champion's cape");
		nicknames.put("skilling outfit", "Rogue equipment");
		for (Map.Entry<String, String> entry : nicknames.entrySet())
		{
			Set<String> found = new TreeSet<>();
			TemplateCategories.group(catalog.getTemplates(), entry.getKey(), false).values()
				.forEach(list -> list.forEach(template -> found.add(template.getName())));
			assertTrue(entry.getKey() + " -> " + found, found.contains(entry.getValue()));
		}
	}

	private List<Object> requirements(String id)
	{
		List<Object> out = new ArrayList<>();
		collect(catalog.find(id).build(Map.of()), node ->
		{
			if (node.getRequirement() != null)
			{
				out.add(node.getRequirement());
			}
		});
		return out;
	}

	private static void collect(TemplateNode node, java.util.function.Consumer<TemplateNode> visitor)
	{
		visitor.accept(node);
		node.getChildren().forEach(child -> collect(child, visitor));
	}
}
