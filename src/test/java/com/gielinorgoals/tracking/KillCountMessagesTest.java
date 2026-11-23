package com.gielinorgoals.tracking;

import net.runelite.client.hiscore.HiscoreSkill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class KillCountMessagesTest
{
	@Test
	public void bossKillCountWithColourTagsAndThousandsSeparator()
	{
		assertCount(HiscoreSkill.ZULRAH, 1234, "Your Zulrah kill count is: <col=ff0000>1,234</col>.");
		assertCount(HiscoreSkill.VORKATH, 1_002_003, "Your Vorkath kill count is: <col=ff0000>1,002,003</col>.");
	}

	@Test
	public void chestCountsMapToHiscoreNames()
	{
		assertCount(HiscoreSkill.BARROWS_CHESTS, 87, "Your Barrows chest count is: <col=ff0000>87</col>.");
		assertCount(HiscoreSkill.LUNAR_CHESTS, 1000, "Your Lunar Chest count is: <col=ff0000>1,000</col>.");
	}

	@Test
	public void raidAndMinigameVariants()
	{
		assertCount(HiscoreSkill.CHAMBERS_OF_XERIC, 40,
			"Your completed Chambers of Xeric count is: <col=ff0000>40</col>.");
		assertCount(HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE, 3,
			"Your completed Chambers of Xeric Challenge Mode count is: <col=ff0000>3</col>.");
		assertCount(HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE, 2,
			"Your completed Theatre of Blood: Hard Mode count is: <col=ff0000>2</col>.");
		assertCount(HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT, 12,
			"Your completion count for Tombs of Amascut: Expert Mode is: <col=ff0000>12</col>.");
		assertCount(HiscoreSkill.WINTERTODT, 500, "Your subdued Wintertodt count is: <col=ff0000>500</col>.");
		assertCount(HiscoreSkill.THE_GAUNTLET, 25, "Your Gauntlet completion count is: <col=ff0000>25</col>.");
		assertCount(HiscoreSkill.THE_CORRUPTED_GAUNTLET, 9,
			"Your Corrupted Gauntlet completion count is: <col=ff0000>9</col>.");
	}

	@Test
	public void namesWithPunctuationAndTheArticle()
	{
		assertCount(HiscoreSkill.KREEARRA, 10, "Your Kree'arra kill count is: <col=ff0000>10</col>.");
		assertCount(HiscoreSkill.TZTOK_JAD, 3, "Your TzTok-Jad kill count is: <col=ff0000>3</col>.");
		assertCount(HiscoreSkill.THE_HUEYCOATL, 7, "Your Hueycoatl kill count is: <col=ff0000>7</col>.");
		assertCount(HiscoreSkill.PHOSANIS_NIGHTMARE, 4, "Your Phosani's Nightmare kill count is: <col=ff0000>4</col>.");
	}

	@Test
	public void clueCompletions()
	{
		assertCount(HiscoreSkill.CLUE_SCROLL_HARD, 1312,
			"You have completed <col=ff0000>1,312</col> hard Treasure Trails.");
		assertCount(HiscoreSkill.CLUE_SCROLL_BEGINNER, 1, "You have completed 1 beginner Treasure Trail.");
		assertCount(HiscoreSkill.CLUE_SCROLL_MASTER, 50,
			"<col=ef1020>You have completed <col=ff0000>50</col> master Treasure Trails.</col>");
	}

	@Test
	public void gauntletAndHueycoatlNamesMatchTheirHiscores()
	{
		// Namnen i spelet saknar "The"; RuneLites egen lista över KC-namn har dem så här.
		assertCount(HiscoreSkill.THE_GAUNTLET, 12, "Your Gauntlet completion count is: <col=ff0000>12</col>.");
		assertCount(HiscoreSkill.THE_CORRUPTED_GAUNTLET, 3,
			"Your Corrupted Gauntlet completion count is: <col=ff0000>3</col>.");
		assertCount(HiscoreSkill.THE_HUEYCOATL, 7, "Your Hueycoatl kill count is: <col=ff0000>7</col>.");
	}

	@Test
	public void unknownCountsAreIgnored()
	{
		assertNull(KillCountMessages.parse("Your Gnome Stronghold Agility Course lap count is: <col=ff0000>50</col>."));
		assertNull(KillCountMessages.parse("Your Lizardman shaman kill count is: 3."));
		assertNull(KillCountMessages.parse("Oh dear, you are dead!"));
		assertNull(KillCountMessages.parse("Fight duration: <col=ff0000>1:23</col>. Personal best: 1:10"));
	}

	private static void assertCount(HiscoreSkill skill, long count, String message)
	{
		KillCountMessages.Count parsed = KillCountMessages.parse(message);
		assertEquals(message, new KillCountMessages.Count(skill, count), parsed);
	}
}
