package com.gielinorgoals.model;

import com.gielinorgoals.persistence.GoalSerializer;
import com.gielinorgoals.persistence.LoadResult;
import com.gielinorgoals.persistence.SchemaMigrator;
import com.gielinorgoals.template.TemplateCatalog;
import com.gielinorgoals.template.TemplateInserter;
import com.gielinorgoals.template.TemplateKeys;
import com.gielinorgoals.template.TemplateNode;
import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * D2: föremål som redan förbrukat målets föremål (countsAlso), t.ex. crystal legs = 2 armour seeds.
 */
public class CountsAlsoTest
{
	private static final ItemRequirement SEEDS = new ItemRequirement(ItemID.PRIF_ARMOUR_SEED, "Crystal armour seed", 6,
		false).withCountsAlso(List.of(
			new ItemRequirement.CountsAlso(ItemID.CRYSTAL_HELMET_INACTIVE, 1, true),
			new ItemRequirement.CountsAlso(ItemID.CRYSTAL_PLATELEGS_INACTIVE, 2, true),
			new ItemRequirement.CountsAlso(ItemID.CRYSTAL_CHESTPLATE_INACTIVE, 3, true)));

	@Test
	public void ownedLegsCountAsTwoSeeds()
	{
		GoalProgress progress = SEEDS.progress(items(Map.of(ItemID.CRYSTAL_PLATELEGS_INACTIVE, 1)));
		assertEquals(2, progress.getCurrent());
		assertEquals(6, progress.getTarget());
		assertFalse(progress.isComplete());
		assertEquals("2/6", progress.getLabel());

		// Legs och ett seed i inventoryt: 3/6.
		assertEquals(3, SEEDS.progress(items(Map.of(ItemID.CRYSTAL_PLATELEGS_INACTIVE, 1, ItemID.PRIF_ARMOUR_SEED, 1)))
			.getCurrent());
		assertTrue(SEEDS.describeProgress(progress).contains("already made"));
	}

	@Test
	public void allThreePiecesCompleteTheGoal()
	{
		GoalProgress progress = SEEDS.progress(items(Map.of(ItemID.CRYSTAL_HELMET_INACTIVE, 1,
			ItemID.CRYSTAL_PLATELEGS_INACTIVE, 1, ItemID.CRYSTAL_CHESTPLATE_INACTIVE, 1)));
		assertEquals(6, progress.getCurrent());
		assertTrue(progress.isComplete());
	}

	@Test
	public void goalsWithoutCountsAlsoAreUnchanged()
	{
		ItemRequirement plain = new ItemRequirement(ItemID.PRIF_ARMOUR_SEED, "Crystal armour seed", 6, false);
		assertTrue(plain.getCountsAlso().isEmpty());
		assertEquals(0, plain.progress(items(Map.of(ItemID.CRYSTAL_PLATELEGS_INACTIVE, 1))).getCurrent());
		assertEquals(1, plain.progress(items(Map.of(ItemID.PRIF_ARMOUR_SEED, 1))).getCurrent());
		// Samma mål för dubblettregeln och samma nyckel, med eller utan motsvarigheter.
		assertTrue(plain.isSameGoal(SEEDS));
		assertEquals(TemplateKeys.of(plain), TemplateKeys.of(SEEDS));
		assertTrue(SEEDS.isValid());
		// Okänt antal för en motsvarighet ger okänd progress.
		assertNull(SEEDS.progress(GameData.EMPTY));
	}

	@Test
	public void countsAlsoIsSavedAndLoaded()
	{
		GoalSerializer serializer = new GoalSerializer(new Gson(), new SchemaMigrator());
		GoalTree tree = new GoalTree();
		Goal goal = new Goal("");
		goal.setRequirement(SEEDS);
		tree.add(null, goal);

		LoadResult loaded = serializer.deserialize(serializer.serialize(tree));
		assertFalse(loaded.isMigrated());
		ItemRequirement back = (ItemRequirement) loaded.getTree().find(goal.getId()).getRequirement();
		assertEquals(SEEDS, back);
		assertEquals(3, back.getCountsAlso().size());
		assertEquals(3, back.getCountsAlso().get(2).getEach());

		// Ett äldre sparat item-mål utan fältet fungerar som förut.
		LoadResult old = serializer.deserialize("{\"schemaVersion\":13,\"rootIds\":[\"a\"],\"goals\":[{\"id\":\"a\","
			+ "\"title\":\"\",\"childIds\":[],\"requirement\":{\"type\":\"ITEM\",\"itemId\":23956,"
			+ "\"itemName\":\"Crystal armour seed\",\"quantity\":6,\"includeVariants\":false}}]}");
		assertTrue(old.isMigrated());
		ItemRequirement oldItem = (ItemRequirement) old.getTree().find("a").getRequirement();
		assertTrue(oldItem.getCountsAlso().isEmpty());
		assertEquals(6, oldItem.getQuantity());
	}

	@Test
	public void bowfaTemplateCountsOwnedLegsAtOnce()
	{
		TemplateNode node = new TemplateCatalog(new Gson()).find("bowfa-crystal").build(Map.of());
		GoalTree tree = new GoalTree();
		TemplateInserter.insert(tree, node);
		GameData data = items(Map.of(ItemID.CRYSTAL_PLATELEGS_INACTIVE, 1));
		Map<String, GoalProgress> progress = GoalEvaluator.evaluate(tree, data, 1L, TemplateKeys::of).getProgress();

		Goal seeds = find(tree, "item:" + ItemID.PRIF_ARMOUR_SEED + ":6");
		Goal shards = find(tree, "item:" + ItemID.PRIF_CRYSTAL_SHARD + ":400");
		assertEquals(2, progress.get(seeds.getId()).getCurrent());
		assertEquals(100, progress.get(shards.getId()).getCurrent());
		// Bågen räknas som 100 shards men inte som armour seeds.
		GameData bow = items(Map.of(ItemID.BOW_OF_FAERDHINEN_INACTIVE, 1));
		Map<String, GoalProgress> withBow = GoalEvaluator.evaluate(tree, bow, 1L, TemplateKeys::of).getProgress();
		assertEquals(0, withBow.get(seeds.getId()).getCurrent());
		assertEquals(100, withBow.get(shards.getId()).getCurrent());

		// Torrhetsmätaren räknar fortfarande på alla sex seeds.
		assertEquals(6, DryMeter.dropsNeeded(seeds, DryMeter.sourceOf(seeds)));
		assertTrue(DryMeter.chanceText(seeds, 300).startsWith("Chance to have all 6 by now"));
		assertEquals("300 kc · 1.0× rate", DryMeter.rowText(seeds, 300));
	}

	private static Goal find(GoalTree tree, String key)
	{
		for (Goal goal : tree.flatten())
		{
			if (key.equals(TemplateKeys.of(goal)))
			{
				return goal;
			}
		}
		throw new AssertionError("No goal " + key);
	}

	private static GameData items(Map<Integer, Integer> counts)
	{
		return new GameData()
		{
			@Override
			public long getXp(Skill skill)
			{
				return -1;
			}

			@Override
			public long getItemCount(int itemId)
			{
				return counts.getOrDefault(itemId, 0);
			}
		};
	}
}
