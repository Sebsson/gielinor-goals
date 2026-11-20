package com.gielinorgoals.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.google.gson.Gson;
import java.util.Map;
import org.junit.Test;

public class ContainerCacheTest
{
	private final Gson gson = new Gson();

	@Test
	public void roundTripKeepsItemsAndTimestamp()
	{
		ContainerCache cache = new ContainerCache(1234L, Map.of(995, 1_000_000L, 385, 250L));

		ContainerCache loaded = ContainerCache.fromJson(gson, cache.toJson(gson));

		assertEquals(1234L, loaded.getSavedAt());
		assertEquals(Long.valueOf(1_000_000L), loaded.getItems().get(995));
		assertEquals(Long.valueOf(250L), loaded.getItems().get(385));
	}

	@Test
	public void missingOrBrokenDataGivesNull()
	{
		assertNull(ContainerCache.fromJson(gson, null));
		assertNull(ContainerCache.fromJson(gson, ""));
		assertNull(ContainerCache.fromJson(gson, "{broken"));
		assertNull(ContainerCache.fromJson(gson, "{\"savedAt\":1}"));
		assertNull(ContainerCache.fromJson(gson, "{\"savedAt\":1,\"items\":{\"x\":1}}"));
	}
}
