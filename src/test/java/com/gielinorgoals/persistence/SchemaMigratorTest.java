package com.gielinorgoals.persistence;

import static org.junit.Assert.assertEquals;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.Test;

public class SchemaMigratorTest
{
	@Test
	public void currentVersionIsUntouched()
	{
		String json = "{\"schemaVersion\":" + SchemaMigrator.CURRENT_VERSION + ",\"goals\":[]}";
		JsonObject result = new SchemaMigrator().migrate(parse(json));

		assertEquals(SchemaMigrator.CURRENT_VERSION, SchemaMigrator.versionOf(result));
		assertEquals(0, result.getAsJsonArray("goals").size());
	}

	@Test
	public void version1IsMigratedToCurrentWithGoalsIntact()
	{
		JsonObject result = new SchemaMigrator().migrate(parse("{\"schemaVersion\":1,\"goals\":[{\"id\":\"a\"}]}"));

		assertEquals(SchemaMigrator.CURRENT_VERSION, SchemaMigrator.versionOf(result));
		assertEquals(1, result.getAsJsonArray("goals").size());
	}

	@Test
	public void stepsRunInOrderAndBumpVersion()
	{
		UnaryOperator<JsonObject> v1ToV2 = root ->
		{
			root.addProperty("trail", "a");
			return root;
		};
		UnaryOperator<JsonObject> v2ToV3 = root ->
		{
			root.addProperty("trail", root.get("trail").getAsString() + "b");
			return root;
		};
		SchemaMigrator migrator = new SchemaMigrator(Map.of(1, v1ToV2, 2, v2ToV3));

		JsonObject result = migrator.migrate(parse("{\"schemaVersion\":1}"), 3);

		assertEquals(3, SchemaMigrator.versionOf(result));
		assertEquals("ab", result.get("trail").getAsString());
	}

	@Test(expected = IllegalStateException.class)
	public void missingStepThrows()
	{
		new SchemaMigrator().migrate(parse("{}"), 1);
	}

	private static JsonObject parse(String json)
	{
		return new Gson().fromJson(json, JsonObject.class);
	}
}
