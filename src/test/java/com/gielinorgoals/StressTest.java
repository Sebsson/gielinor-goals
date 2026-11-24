package com.gielinorgoals;

import com.gielinorgoals.map.LayeredLayout;
import com.gielinorgoals.map.MapGraph;
import com.gielinorgoals.map.MapLayout;
import com.gielinorgoals.map.MapView;
import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.NextGoals;
import com.gielinorgoals.template.GoalTemplate;
import com.gielinorgoals.template.QuestChains;
import com.gielinorgoals.template.QuestRequirementsDb;
import com.gielinorgoals.template.TemplateCatalog;
import com.gielinorgoals.template.TemplateNode;
import com.gielinorgoals.tracking.WatchedAchievements;
import com.google.gson.Gson;
import java.util.Map;
import java.util.function.LongSupplier;
import net.runelite.api.Skill;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Alla templates i en profil: Quest cape och Achievement diary cape med hela kedjor, Base 99
 * och alla handplockade. Mäter det som körs vid varje ändring i spelläget (utvärdering och
 * pluginets arbete i onGoalsChanged utanför UI) och en kartlayout för "Alla mål".
 * Tiderna skrivs ut; gränserna är generösa så att testet inte blir skakigt på långsamma datorer.
 */
public class StressTest
{
	private static final int ROUNDS = 50;

	@Test
	public void everyTemplateInOneProfile()
	{
		Gson gson = new Gson();
		GoalManager manager = TestGoalManagers.inMemory("stress");
		QuestRequirementsDb db = QuestRequirementsDb.load(gson);
		for (GoalTemplate template : com.gielinorgoals.template.TestCombatAchievements.catalog(gson).getTemplates())
		{
			TemplateNode node = template.build(Map.of("level", 99));
			QuestChains.expand(node, db, manager.getTree());
			manager.insertTemplate(node);
		}
		int goals = manager.getTree().size();

		// Varierande XP, så att utvärderingen har något att göra varje gång.
		long[] tick = {0};
		LongSupplier evaluation = () -> time(() ->
		{
			long xp = 1_000_000 + tick[0]++;
			manager.updateGameData("stress", new GameData()
			{
				@Override
				public long getXp(Skill skill)
				{
					return xp;
				}
			});
		});
		LongSupplier pluginWork = () -> time(() ->
		{
			WatchedAchievements.from(manager.getTree());
			manager.watchedCounterKeys();
			NextGoals.select(manager.getTree(), manager::getProgress, 5);
		});
		// Panelen slår upp föräldrarna för varje rad den bygger (GoalRow); raderna själva går
		// inte att skapa utan RuneLites ikontjänster, men uppslaget är den kvadratiska delen.
		LongSupplier rowParents = () -> time(() -> manager.getTree().forEach(goal -> manager.getTree().getParents(goal.getId())));
		MapLayout[] layout = new MapLayout[1];
		LongSupplier mapLayout = () -> time(() ->
			layout[0] = LayeredLayout.compute(MapGraph.build(manager.getTree(), MapView.allGoals())));

		double evaluate = median(evaluation);
		double plugin = median(pluginWork);
		double map = median(mapLayout);
		double parents = median(rowParents);
		System.out.printf("Stress: %d goals, %d on the map%n", goals, layout[0].getNodes().size());
		System.out.printf("  evaluation per game-data change: %.2f ms (median of %d)%n", evaluate, ROUNDS);
		System.out.printf("  plugin work per change (watched achievements, counters, next goals): %.2f ms%n", plugin);
		System.out.printf("  map layout, all goals: %.2f ms%n", map);
		System.out.printf("  parent lookups for every panel row: %.2f ms%n", parents);

		assertTrue("goals " + goals, goals > 400);
		assertTrue("evaluation " + evaluate + " ms", evaluate < 50);
		assertTrue("plugin work " + plugin + " ms", plugin < 50);
		assertTrue("map layout " + map + " ms", map < 500);
	}

	private static long time(Runnable work)
	{
		long start = System.nanoTime();
		work.run();
		return System.nanoTime() - start;
	}

	// Median efter uppvärmning, i millisekunder.
	private static double median(LongSupplier sample)
	{
		for (int i = 0; i < 10; i++)
		{
			sample.getAsLong();
		}
		long[] values = new long[ROUNDS];
		for (int i = 0; i < ROUNDS; i++)
		{
			values[i] = sample.getAsLong();
		}
		java.util.Arrays.sort(values);
		return values[ROUNDS / 2] / 1_000_000.0;
	}
}
