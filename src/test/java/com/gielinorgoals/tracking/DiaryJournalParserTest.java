package com.gielinorgoals.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryJournal;
import com.gielinorgoals.model.DiaryTier;
import java.util.List;
import org.junit.Test;

public class DiaryJournalParserTest
{
	// Riktiga rader från Morytania-journalen i spelet (2026-11), för en spelare som klarat easy-hard.
	private static final List<String> MORYTANIA = List.of(
		" ",
		"<col=00ff00>Easy",
		"<col=000000>",
		" ",
		"<str>Craft any Snelm from scratch in Morytania.",
		"<str>Cook a thin Snail on the Port Phasmatys range.",
		"<str>Get a slayer task from the Slayer Master in Canifis.",
		"<str>Kill a Banshee in the Slayer Tower.",
		"<str>Have Sbott in Canifis tan something for you.",
		"<str>Enter Mort Myre Swamp.",
		"<str>Kill a Ghoul.",
		"<str>Place a Scarecrow in the Morytania flower patch.",
		"<str>Offer some bonemeal at the Ectofuntus.",
		"<str>Kill a werewolf in its human form using the Wolfbane",
		"<str>Dagger.",
		"<str>Restore your prayer points at the nature altar.",
		"<col=000000>If I ever lose my Morytania legs I can speak to Le-sabre in",
		"<col=000000>Canifis.",
		" ",
		"<col=00ff00>Medium",
		"<col=000000>",
		" ",
		"<str>Catch a swamp lizard.",
		"<str>Complete a lap of the Canifis agility course.",
		"<str>Obtain some Bark from a Hollow tree.",
		"<str>Travel to Dragontooth Isle.",
		"<str>Kill a Terror Dog.",
		"<str>Complete a game of trouble brewing.",
		"<str>Board the Swampy boat at the Hollows.",
		"<str>Make a batch of steel cannonballs at the Port Phasmatys",
		"<str>furnace.",
		"<str>Kill a Fever Spider on Braindeath Island.",
		"<str>Use an ectophial to return to Port Phasmatys.",
		"<str>Mix a Guthix Balance potion while in Morytania.",
		"<col=000000>If I ever lose my Morytania legs I can speak to Le-sabre in",
		"<col=000000>Canifis.",
		" ",
		"<col=00ff00>Hard",
		"<col=000000>",
		" ",
		"<str>Enter the Kharyrll portal in your POH.",
		"<str>Climb the advanced spike chain within Slayer Tower.",
		"<str>Harvest some Watermelon from the Allotment patch on",
		"<str>Harmony Island.",
		"<str>Chop and burn some mahogany logs on Mos Le'Harmless.",
		"<str>Complete a temple trek with a hard companion.",
		"<str>Kill a Cave Horror.",
		"<str>Harvest some Bittercap Mushrooms from the patch in",
		"<str>Canifis.",
		"<str>Pray at the Altar of Nature with Piety activated.",
		"<str>Use the shortcut to get to the bridge over the Salve.",
		"<str>Mine some Mithril ore in the Abandoned Mine.",
		"<col=000000>If I ever lose my Morytania legs I can speak to Le-sabre in",
		"<col=000000>Canifis.",
		" ",
		"<col=ffff00>Elite",
		"<str>",
		" ",
		"<col=000000>Catch a shark in Burgh de Rott with your bare hands.",
		"<str>Cremate any Shade remains on a Magic or Redwood pyre.",
		"<col=000000>Fertilize the Morytania herb patch using Lunar Magic.",
		"<col=000000>Craft a Black dragonhide body in Canifis bank.",
		"<col=000000>Kill an Abyssal demon in the Slayer Tower.",
		"<col=000000>Loot the Barrows chest while wearing any complete",
		"<col=000000>barrows set.",
		"",
		"");

	@Test
	public void parsesRealMorytaniaJournal()
	{
		DiaryJournal journal = DiaryJournalParser.parse("<col=ffff00>Morytania Tasks", MORYTANIA);

		assertEquals(DiaryArea.MORYTANIA, journal.getArea());
		assertTrue(journal.getMismatchedTiers().toString(), journal.getMismatchedTiers().isEmpty());
		assertEquals(11, journal.tasks(DiaryTier.EASY).size());
		assertEquals(11, journal.tasks(DiaryTier.MEDIUM).size());
		assertEquals(10, journal.tasks(DiaryTier.HARD).size());

		// Radbruten uppgift blir en uppgift, och anteckningen efter nivån är borttagen.
		assertEquals("Kill a werewolf in its human form using the Wolfbane Dagger.",
			journal.tasks(DiaryTier.EASY).get(9).getText());
		assertEquals("Restore your prayer points at the nature altar.",
			journal.tasks(DiaryTier.EASY).get(10).getText());

		List<DiaryJournal.Task> elite = journal.tasks(DiaryTier.ELITE);
		assertEquals(6, elite.size());
		assertFalse(elite.get(0).isDone());
		assertTrue(elite.get(1).isDone());
		assertEquals("Loot the Barrows chest while wearing any complete barrows set.", elite.get(5).getText());
		assertFalse(elite.get(5).isDone());
	}

	@Test
	public void inlineTagsAndWrappedUnfinishedTasksAreHandled()
	{
		List<String> lines = List.of(
			"<col=ffff00>Elite",
			"<col=000000>",
			"<col=000000>Fletch a Magic Longbow in Tirannwn.",
			"<col=000000>Kill the Thermonuclear Smoke devil <col=800000>(Does not require task)<col=000000>.",
			"<col=000000>Have Prissy Scilla protect your Magic tree.",
			"<col=000000>Use the Elven overpass advanced cliffside shortcut.",
			"<str>Equip any complete void set.",
			"<col=000000>Claim a Chompy bird hat from Rantz after registering at",
			"<col=000000>least 1000 kills.",
			"<str>Pickpocket an Elf.");

		DiaryJournal journal = DiaryJournalParser.parse("Western Area Tasks", lines);

		List<DiaryJournal.Task> elite = journal.tasks(DiaryTier.ELITE);
		assertEquals(7, elite.size());
		assertEquals("Kill the Thermonuclear Smoke devil (Does not require task).", elite.get(1).getText());
		assertEquals("Claim a Chompy bird hat from Rantz after registering at least 1000 kills.", elite.get(5).getText());
		assertTrue(elite.get(6).isDone());
	}

	@Test
	public void wrongTaskCountIsReportedInsteadOfImported()
	{
		DiaryJournal journal = DiaryJournalParser.parse("Ardougne Area Tasks", List.of("Medium", "Only one task."));

		assertTrue(journal.tasks(DiaryTier.MEDIUM).isEmpty());
		assertEquals(Integer.valueOf(1), journal.getMismatchedTiers().get(DiaryTier.MEDIUM));
	}

	@Test
	public void runeLiteRequirementTextIsIgnored()
	{
		List<String> lines = new java.util.ArrayList<>();
		lines.add("Elite");
		for (int i = 1; i <= 8; i++)
		{
			lines.add("Elite task " + i + ".<col=ffffff>(<col=800000>90 Agility<col=ffffff>)");
		}

		DiaryJournal journal = DiaryJournalParser.parse("Ardougne Area Tasks", lines);

		assertEquals("Elite task 1.", journal.tasks(DiaryTier.ELITE).get(0).getText());
		assertEquals(8, journal.tasks(DiaryTier.ELITE).size());
	}

	@Test
	public void nonDiaryJournalIsIgnored()
	{
		assertNull(DiaryJournalParser.parse("Cook's Assistant", List.of("Easy", "Talk to the cook.")));
	}

	@Test
	public void tierHeadersAreRecognised()
	{
		assertEquals(DiaryTier.EASY, DiaryJournalParser.tierHeader("Easy"));
		assertEquals(DiaryTier.HARD, DiaryJournalParser.tierHeader("Hard Tasks:"));
		assertNull(DiaryJournalParser.tierHeader("Easy money from the shop"));
	}
}
