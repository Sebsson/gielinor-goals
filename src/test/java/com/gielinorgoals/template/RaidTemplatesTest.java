package com.gielinorgoals.template;

import com.gielinorgoals.model.DropSource;
import com.gielinorgoals.model.DryMeter;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.persistence.GoalSerializer;
import com.gielinorgoals.persistence.SchemaMigrator;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.hiscore.HiscoreSkill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Raid-templates i våg 3 (milstolpe 15, del 2) och ungefärliga drop rates.
 */
public class RaidTemplatesTest
{
	private final Gson gson = new Gson();
	private final TemplateCatalog catalog = new TemplateCatalog(gson);

	@Test
	public void uniqueIdsMatchGameval()
	{
		assertEquals(new TreeSet<>(Set.of(ItemID.RAIDS_PRAYERSCROLL, ItemID.RAIDS_PRAYERSCROLL_AUGURY,
			ItemID.TWISTED_BUCKLER, ItemID.DRAGONHUNTER_XBOW, ItemID.DINHS_BULWARK, ItemID.ANCESTRAL_HAT,
			ItemID.ANCESTRAL_ROBE_TOP, ItemID.ANCESTRAL_ROBE_BOTTOM, ItemID.DRAGON_CLAWS, ItemID.ELDER_MAUL,
			ItemID.KODAI_INSIGNIA, ItemID.TWISTED_BOW)), itemIds("chambers-of-xeric"));
		assertEquals(new TreeSet<>(Set.of(ItemID.INFERNAL_DEFENDER_HILT, ItemID.GHRAZI_RAPIER,
			ItemID.SANGUINESTI_STAFF_UNCHARGED, ItemID.JUSTICIAR_FACEGUARD, ItemID.JUSTICIAR_CHESTGUARD,
			ItemID.JUSTICIAR_LEG_GUARDS, ItemID.SCYTHE_OF_VITUR_UNCHARGED)), itemIds("theatre-of-blood"));
		assertEquals(new TreeSet<>(Set.of(ItemID.OSMUMTENS_FANG, ItemID.LIGHTBEARER, ItemID.ELIDINIS_WARD,
			ItemID.MASORI_MASK, ItemID.MASORI_BODY, ItemID.MASORI_CHAPS, ItemID.TUMEKENS_SHADOW_UNCHARGED)),
			itemIds("tombs-of-amascut"));
	}

	@Test
	public void completionCountsUseTheRaidHiscores()
	{
		assertEquals(List.of(HiscoreSkill.CHAMBERS_OF_XERIC, HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE),
			killCounts("chambers-of-xeric"));
		assertEquals(List.of(HiscoreSkill.THEATRE_OF_BLOOD, HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE),
			killCounts("theatre-of-blood"));
		assertEquals(List.of(HiscoreSkill.TOMBS_OF_AMASCUT, HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT),
			killCounts("tombs-of-amascut"));
	}

	@Test
	public void everyUniqueHasAnApproximateRateFromTheRaid()
	{
		Map<String, HiscoreSkill> raids = Map.of("chambers-of-xeric", HiscoreSkill.CHAMBERS_OF_XERIC,
			"theatre-of-blood", HiscoreSkill.THEATRE_OF_BLOOD, "tombs-of-amascut", HiscoreSkill.TOMBS_OF_AMASCUT);
		for (Map.Entry<String, HiscoreSkill> raid : raids.entrySet())
		{
			List<DropSource> sources = new ArrayList<>();
			collect(catalog.find(raid.getKey()).build(Map.of()), node ->
			{
				if (node.getRequirement() instanceof ItemRequirement)
				{
					assertNotNull(node.getDisplayTitle(), node.getDropSource());
					sources.add(node.getDropSource());
				}
			});
			for (DropSource source : sources)
			{
				assertTrue(source.isApproximate());
				assertEquals(raid.getValue(), source.hiscoreSkill());
				assertFalse(DropSource.DEFAULT_APPROXIMATE_NOTE.equals(source.getApproximateNote()));
			}
		}
		Map<String, Integer> cox = rates("chambers-of-xeric");
		assertEquals(500, (int) cox.get("Dragon hunter crossbow"));
		assertEquals(1000, (int) cox.get("Twisted bow"));
		assertEquals(143, (int) cox.get("Dexterous prayer scroll"));
		Map<String, Integer> tob = rates("theatre-of-blood");
		assertEquals(86, (int) tob.get("Avernic defender hilt"));
		assertEquals(692, (int) tob.get("Scythe of vitur (uncharged)"));
		Map<String, Integer> toa = rates("tombs-of-amascut");
		assertEquals(26, (int) toa.get("Osmumten's fang"));
		assertEquals(183, (int) toa.get("Tumeken's shadow (uncharged)"));
		// DHCB-templaten har samma ungefärliga rate som raiden.
		assertEquals(500, (int) rates("dhcb").get("Dragon hunter crossbow"));
	}

	@Test
	public void approximateRatesShowTheSign()
	{
		DropSource source = DropSource.hiscore(HiscoreSkill.CHAMBERS_OF_XERIC, 500).withApproximate("Depends on points");
		Goal goal = new Goal("");
		goal.setRequirement(new ItemRequirement(ItemID.DRAGONHUNTER_XBOW, "Dragon hunter crossbow", 1, true));
		goal.setDropSource(source);
		assertEquals("250 kc · ≈0.5× rate", DryMeter.rowText(goal, 250));
		assertEquals("Chance to have received it by now: ≈39%", DryMeter.chanceText(goal, 250));
		assertEquals("≈1/500 from Chambers of Xeric", source.describe());
		assertEquals("Depends on points", source.getApproximateNote());

		DropSource exact = DropSource.hiscore(HiscoreSkill.ZULRAH, 512);
		assertFalse(exact.isApproximate());
		assertEquals(null, exact.getApproximateNote());
		assertEquals(DropSource.DEFAULT_APPROXIMATE_NOTE, exact.withApproximate(" ").getApproximateNote());
		// Flaggan följer med när antalet per drop ändras.
		assertTrue(source.withPerDrop(3).isApproximate());
	}

	@Test
	public void approximateRatesAreSavedAndShared()
	{
		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, catalog.find("theatre-of-blood").build(Map.of()));
		Goal scythe = find(tree, ItemID.SCYTHE_OF_VITUR_UNCHARGED);

		GoalSerializer serializer = new GoalSerializer(gson, new SchemaMigrator());
		GoalTree loaded = serializer.deserialize(serializer.serialize(tree)).getTree();
		DropSource back = loaded.find(scythe.getId()).getDropSource();
		assertTrue(back.isApproximate());
		assertEquals(scythe.getDropSource().getApproximateNote(), back.getApproximateNote());
		assertEquals(16, SchemaMigrator.CURRENT_VERSION);

		// Export till en egen template och tillbaka behåller flaggan och förklaringen.
		Goal root = tree.getRoots().get(0);
		TemplateExport.Result export = TemplateExport.export(tree, root, "My ToB", "", List.of());
		TemplateNode rebuilt = JsonTemplate.from(export.getSpec()).build(Map.of());
		List<DropSource> sources = new ArrayList<>();
		collect(rebuilt, node ->
		{
			if (node.getDropSource() != null)
			{
				sources.add(node.getDropSource());
			}
		});
		assertEquals(7, sources.size());
		sources.forEach(source -> assertTrue(source.isApproximate()));
		assertEquals(scythe.getDropSource().getApproximateNote(), sources.get(sources.size() - 1).getApproximateNote());
	}

	@Test
	public void approximateNoteNeedsTheFlag()
	{
		JsonTemplate.ActivitySpec spec = new JsonTemplate.ActivitySpec();
		spec.hiscore = "ZULRAH";
		spec.rate = 512;
		spec.approximateNote = "Why";
		JsonTemplate.NodeSpec node = new JsonTemplate.NodeSpec();
		node.item = new JsonTemplate.ItemSpec();
		node.item.id = ItemID.BLOWPIPE_FANG;
		node.item.name = "Tanzanite fang";
		node.dropSource = spec;
		JsonTemplate.FileSpec file = new JsonTemplate.FileSpec();
		file.id = "bad";
		file.name = "Bad";
		file.root = node;
		try
		{
			JsonTemplate.from(file);
			throw new AssertionError("approximateNote without approximate should be refused");
		}
		catch (IllegalArgumentException expected)
		{
			assertTrue(expected.getMessage().contains("approximate"));
		}
	}

	@Test
	public void nicknamesFindTheRaids()
	{
		// DHCB-templaten har också taggen cox.
		assertEquals(List.of("Dragon hunter crossbow", "Chambers of Xeric"), search("cox"));
		assertEquals(List.of("Chambers of Xeric"), search("olm"));
		assertEquals(List.of("Chambers of Xeric"), search("raids 1"));
		assertEquals(List.of("Theatre of Blood"), search("tob"));
		assertEquals(List.of("Theatre of Blood"), search("raids 2"));
		assertEquals(List.of("Tombs of Amascut"), search("toa"));
		assertEquals(List.of("Tombs of Amascut"), search("raids 3"));
		assertTrue(search("raids").containsAll(List.of("Chambers of Xeric", "Theatre of Blood", "Tombs of Amascut")));
		assertEquals(List.of(), catalog.getLoadErrors());
	}

	private Set<Integer> itemIds(String id)
	{
		Set<Integer> items = new TreeSet<>();
		collect(catalog.find(id).build(Map.of()), node ->
		{
			if (node.getRequirement() instanceof ItemRequirement)
			{
				items.add(((ItemRequirement) node.getRequirement()).getItemId());
			}
		});
		return items;
	}

	private List<HiscoreSkill> killCounts(String id)
	{
		List<HiscoreSkill> skills = new ArrayList<>();
		collect(catalog.find(id).build(Map.of()), node ->
		{
			if (node.getRequirement() instanceof KillCountRequirement)
			{
				KillCountRequirement kc = (KillCountRequirement) node.getRequirement();
				assertEquals(KillCountRequirement.ActivityType.HISCORE, kc.getActivityType());
				skills.add(HiscoreSkill.valueOf(kc.getActivityId()));
			}
		});
		return skills;
	}

	private Map<String, Integer> rates(String id)
	{
		Map<String, Integer> rates = new HashMap<>();
		collect(catalog.find(id).build(Map.of()), node ->
		{
			if (node.getDropSource() != null)
			{
				rates.put(node.getDisplayTitle(), node.getDropSource().getRate());
			}
		});
		return rates;
	}

	private List<String> search(String query)
	{
		List<String> found = new ArrayList<>();
		TemplateCategories.group(catalog.getTemplates(), query, false).values()
			.forEach(list -> list.forEach(template -> found.add(template.getName())));
		return found;
	}

	private static Goal find(GoalTree tree, int itemId)
	{
		for (Goal goal : tree.flatten())
		{
			if (goal.getRequirement() instanceof ItemRequirement
				&& ((ItemRequirement) goal.getRequirement()).getItemId() == itemId)
			{
				return goal;
			}
		}
		throw new AssertionError("No item " + itemId);
	}

	private static void collect(TemplateNode node, java.util.function.Consumer<TemplateNode> visitor)
	{
		visitor.accept(node);
		node.getChildren().forEach(child -> collect(child, visitor));
	}
}
