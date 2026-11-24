package com.gielinorgoals.template;

import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Utrustnings-templates i våg 3 (milstolpe 15, del 1).
 */
public class EquipmentTemplatesTest
{
	private final Gson gson = new Gson();
	private final TemplateCatalog catalog = new TemplateCatalog(gson);
	private final QuestRequirementsDb db = QuestRequirementsDb.load(gson);

	@Test
	public void itemIdsMatchGameval()
	{
		Map<String, Set<Integer>> expected = new HashMap<>();
		expected.put("blowpipe-serp", Set.of(ItemID.BLOWPIPE_FANG, ItemID.TOXIC_BLOWPIPE, ItemID.SERPENTINE_VISAGE,
			ItemID.SERPENTINE_HELM, ItemID.SNAKEBOSS_SCALE));
		expected.put("trident-seas", Set.of(ItemID.TOTS_UNCHARGED));
		expected.put("trident-swamp", Set.of(ItemID.TOTS_UNCHARGED, ItemID.MAGIC_FANG, ItemID.TOXIC_TOTS_UNCHARGED));
		expected.put("dhcb", Set.of(ItemID.DRAGONHUNTER_XBOW));
		expected.put("moons-of-peril", Set.of(ItemID.BLOOD_MOON_HELM, ItemID.BLOOD_MOON_CHESTPLATE,
			ItemID.BLOOD_MOON_TASSETS, ItemID.DUAL_MACUAHUITL, ItemID.FROST_MOON_HELM, ItemID.FROST_MOON_CHESTPLATE,
			ItemID.FROST_MOON_TASSETS, ItemID.FROSTMOON_SPEAR, ItemID.ECLIPSE_MOON_HELM, ItemID.ECLIPSE_MOON_CHESTPLATE,
			ItemID.ECLIPSE_MOON_TASSETS, ItemID.ECLIPSE_ATLATL));
		expected.put("sunfire-fanatic", Set.of(ItemID.SUNFIRE_HELM, ItemID.SUNFIRE_BODY, ItemID.SUNFIRE_LEGS));
		expected.put("dragonfire-shield", Set.of(ItemID.ANTIDRAGONBREATHSHIELD, ItemID.DRAGONFIRE_VISAGE,
			ItemID.DRAGONFIRE_SHIELD_UNCHARGED));
		expected.put("dt2-rings", Set.of(ItemID.CHROMIUM_INGOT, ItemID.ULTOR_VESTIGE, ItemID.ULTOR_RING,
			ItemID.BELLATOR_VESTIGE, ItemID.BELLATOR_RING, ItemID.MAGUS_VESTIGE, ItemID.MAGUS_RING, ItemID.VENATOR_VESTIGE,
			ItemID.VENATOR_RING));
		for (Map.Entry<String, Set<Integer>> entry : expected.entrySet())
		{
			GoalTemplate template = catalog.find(entry.getKey());
			assertNotNull(entry.getKey(), template);
			Set<Integer> items = new TreeSet<>();
			collect(template.build(Map.of()), node ->
			{
				if (node.getRequirement() instanceof ItemRequirement)
				{
					items.add(((ItemRequirement) node.getRequirement()).getItemId());
				}
			});
			assertEquals(entry.getKey(), new TreeSet<>(entry.getValue()), items);
		}
		// Torture-templaten har zenyte-templatens föremål via referensen.
		Set<Integer> torture = new TreeSet<>();
		collect(catalog.find("torture-tormented").build(Map.of()), node ->
		{
			if (node.getRequirement() instanceof ItemRequirement)
			{
				torture.add(((ItemRequirement) node.getRequirement()).getItemId());
			}
		});
		assertTrue(torture.containsAll(Set.of(ItemID.ZENYTE_SHARD, ItemID.ZENYTE_BRACELET_ENCHANTED,
			ItemID.ZENYTE_AMULET_ENCHANTED)));
	}

	@Test
	public void barrowsHasFourPiecesPerBrother()
	{
		GoalTemplate barrows = catalog.find("barrows-set");
		Map<String, Integer> heads = Map.of("AHRIM", ItemID.BARROWS_AHRIM_HEAD, "DHAROK", ItemID.BARROWS_DHAROK_HEAD,
			"GUTHAN", ItemID.BARROWS_GUTHAN_HEAD, "KARIL", ItemID.BARROWS_KARIL_HEAD, "TORAG", ItemID.BARROWS_TORAG_HEAD,
			"VERAC", ItemID.BARROWS_VERAC_HEAD);
		assertEquals(6, barrows.getParameters().get(0).getOptions().size());
		for (Map.Entry<String, Integer> brother : heads.entrySet())
		{
			TemplateNode root = barrows.build(Map.of(), Map.of("brother", brother.getKey()));
			TemplateNode obtain = root.getChildren().get(1);
			assertEquals(4, obtain.getChildren().size());
			TemplateNode head = obtain.getChildren().get(0);
			assertEquals((int) brother.getValue(), ((ItemRequirement) head.getRequirement()).getItemId());
			obtain.getChildren().forEach(piece ->
			{
				assertEquals(BarrowsTemplate.RATE_PER_PIECE, piece.getDropSource().getRate());
				assertEquals("hiscore:BARROWS_CHESTS", piece.getDropSource().counterKey());
			});
		}
		assertEquals("Dharok's equipment", barrows.build(Map.of(), Map.of("brother", "DHAROK")).getTitle());
	}

	@Test
	public void dropSourcesPointAtTheRightBoss()
	{
		Map<String, String> counters = new HashMap<>();
		collect(catalog.find("dt2-rings").build(Map.of()), node ->
		{
			if (node.getDropSource() != null)
			{
				counters.put(node.getDisplayTitle(), node.getDropSource().counterKey() + "/" + node.getDropSource().getRate());
			}
		});
		assertEquals("hiscore:VARDORVIS/1088", counters.get("Ultor vestige"));
		assertEquals("hiscore:THE_WHISPERER/512", counters.get("Bellator vestige"));
		assertEquals("hiscore:DUKE_SUCELLUS/720", counters.get("Magus vestige"));
		assertEquals("hiscore:THE_LEVIATHAN/768", counters.get("Venator vestige"));
	}

	@Test
	public void sharedNodesAreLinkedNotCopied()
	{
		GoalTree tree = new GoalTree();
		insert(tree, "zenyte-jewellery");
		insert(tree, "blowpipe-serp");
		insert(tree, "torture-tormented");
		// Seas först: swamp ska länka in den befintliga seas-roten i stället för att skapa en till.
		insert(tree, "trident-seas");
		insert(tree, "trident-swamp");
		Map<String, Integer> counts = new HashMap<>();
		for (Goal goal : tree.flatten())
		{
			if (goal.getRequirement() != null)
			{
				counts.merge(TemplateKeys.of(goal.getRequirement()), 1, Integer::sum);
			}
		}
		counts.forEach((key, count) -> assertEquals(key, 1, (int) count));
		assertEquals(1, (int) counts.get("quest:" + Quest.REGICIDE.getId()));
		assertEquals(1, roots(tree, "zenyte-jewellery"));
		assertEquals(1, roots(tree, "trident-seas"));
	}

	@Test
	public void ownedRingsCountTowardChromium()
	{
		TemplateNode node = catalog.find("dt2-rings").build(Map.of());
		List<ItemRequirement> chromium = new ArrayList<>();
		collect(node, n ->
		{
			if (n.getRequirement() instanceof ItemRequirement
				&& ((ItemRequirement) n.getRequirement()).getItemId() == ItemID.CHROMIUM_INGOT)
			{
				chromium.add((ItemRequirement) n.getRequirement());
			}
		});
		assertEquals(12, chromium.get(0).getQuantity());
		assertEquals(4, chromium.get(0).getCountsAlso().size());
	}

	@Test
	public void nicknamesFindTheTemplates()
	{
		assertEquals(List.of("Toxic blowpipe + Serpentine helm"), search("bp"));
		assertEquals(List.of("Toxic blowpipe + Serpentine helm"), search("serp"));
		assertEquals(List.of("Dragon hunter crossbow"), search("dhcb"));
		assertEquals(List.of("Dragonfire shield"), search("dfs"));
		// Zenyte-templaten har också taggarna torture och tormented.
		assertTrue(search("torture").contains("Tormented bracelet + Amulet of torture"));
		assertTrue(search("tormented").contains("Tormented bracelet + Amulet of torture"));
		assertEquals(List.of("Moons of Peril"), search("moons"));
		assertEquals(List.of("Sunfire fanatic armour"), search("sunfire"));
		assertEquals(List.of("Desert Treasure II rings"), search("ultor"));
		assertEquals(List.of("Desert Treasure II rings"), search("venator"));
		assertTrue(search("barrows").contains("Barrows set"));
		assertTrue(search("dharok").contains("Barrows set"));
	}

	@Test
	public void everythingLoads()
	{
		assertEquals(List.of(), catalog.getLoadErrors());
	}

	private List<String> search(String query)
	{
		List<String> found = new ArrayList<>();
		TemplateCategories.group(catalog.getTemplates(), query, false).values()
			.forEach(list -> list.forEach(template -> found.add(template.getName())));
		return found;
	}

	private void insert(GoalTree tree, String id)
	{
		TemplateNode node = catalog.find(id).build(Map.of());
		QuestChains.expand(node, db, tree);
		node.setTemplateId(id);
		node.setTemplateParams(Map.of(TemplateUpdates.QUEST_CHAINS_PARAM, 1));
		TemplateInserter.insert(tree, node);
	}

	private static int roots(GoalTree tree, String id)
	{
		int count = 0;
		for (Goal goal : tree.flatten())
		{
			count += id.equals(goal.getTemplateId()) ? 1 : 0;
		}
		return count;
	}

	private static void collect(TemplateNode node, java.util.function.Consumer<TemplateNode> visitor)
	{
		visitor.accept(node);
		node.getChildren().forEach(child -> collect(child, visitor));
	}
}
