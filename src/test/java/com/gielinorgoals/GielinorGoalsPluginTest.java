package com.gielinorgoals;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

// Startpunkt för ./gradlew run: laddar pluginet som inbyggt och startar dev-klienten.
public class GielinorGoalsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GielinorGoalsPlugin.class);
		RuneLite.main(args);
	}
}
