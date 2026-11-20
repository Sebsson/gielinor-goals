package com.gielinorgoals.ui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GielinorGoalsPanelTest
{
	private static final long MINUTE = 60_000L;

	@Test
	public void formatAgeUsesLargestWholeUnit()
	{
		assertEquals("just now", GielinorGoalsPanel.formatAge(10_000L));
		assertEquals("1 minute ago", GielinorGoalsPanel.formatAge(MINUTE));
		assertEquals("45 minutes ago", GielinorGoalsPanel.formatAge(45 * MINUTE));
		assertEquals("2 hours ago", GielinorGoalsPanel.formatAge(130 * MINUTE));
		assertEquals("3 days ago", GielinorGoalsPanel.formatAge(3 * 24 * 60 * MINUTE));
		assertEquals("just now", GielinorGoalsPanel.formatAge(-5L));
	}
}
