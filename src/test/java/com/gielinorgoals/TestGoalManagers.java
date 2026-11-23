package com.gielinorgoals;

import com.gielinorgoals.tracking.InitialSync;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

/**
 * En GoalManager som sparar i minnet, för tester i andra paket (Storage är paketprivat).
 */
public final class TestGoalManagers
{
	private TestGoalManagers()
	{
	}

	public static GoalManager inMemory(String profile)
	{
		Map<String, String> stored = new HashMap<>();
		GoalManager manager = new GoalManager(new GoalManager.Storage()
		{
			@Override
			public String get(String profileKey, String key)
			{
				return stored.get(profileKey + "." + key);
			}

			@Override
			public void set(String profileKey, String key, String value)
			{
				stored.put(profileKey + "." + key, value);
			}
		}, new Gson(), new InitialSync(() -> 1L));
		manager.loadProfile(profile);
		return manager;
	}
}
