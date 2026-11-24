package com.gielinorgoals.template;

import com.gielinorgoals.model.CombatAchievementRequirement;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

/**
 * Påhittade combat achievements för tester utan klient: tre uppgifter per nivå.
 */
public final class TestCombatAchievements
{
	public static final int PER_TIER = 3;

	private TestCombatAchievements()
	{
	}

	public static CombatAchievementSource sample()
	{
		List<CombatAchievementRequirement> tasks = new ArrayList<>();
		int id = 0;
		for (String tier : CombatAchievementTemplate.TIERS)
		{
			for (int i = 0; i < PER_TIER; i++)
			{
				tasks.add(new CombatAchievementRequirement(id, tier + " task " + (i + 1), tier));
				id++;
			}
		}
		return () -> tasks;
	}

	/**
	 * Hela katalogen, med de påhittade uppgifterna, så att alla templates går att bygga.
	 */
	public static TemplateCatalog catalog(Gson gson)
	{
		return new TemplateCatalog(gson, QuestRequirementsDb.load(gson), sample());
	}
}
