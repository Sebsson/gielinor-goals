package com.gielinorgoals.ui;

import com.gielinorgoals.model.CombatAchievementRequirement;
import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.SkillRequirement;
import net.runelite.api.Skill;
import net.runelite.client.hiscore.HiscoreSkill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class WikiLinksTest
{
	private static final String W = "https://oldschool.runescape.wiki/w/";

	@Test
	public void itemsUseSpecialLookupWithTheId()
	{
		assertEquals(W + "Special:Lookup?type=item&id=7462",
			WikiLinks.url(new ItemRequirement(7462, "Barrows gloves", 1, false)));
	}

	@Test
	public void questPagesAreEncoded()
	{
		assertEquals(W + "King%27s_Ransom", WikiLinks.url(new QuestRequirement(82, "King's Ransom")));
		assertEquals(W + "Romeo_%26_Juliet", WikiLinks.url(new QuestRequirement(121, "Romeo & Juliet")));
		assertEquals(W + "Wanted%21", WikiLinks.url(new QuestRequirement(156, "Wanted!")));
		assertEquals(W + "Desert_Treasure_II_-_The_Fallen_Empire",
			WikiLinks.url(new QuestRequirement(2343, "Desert Treasure II - The Fallen Empire")));
		// RFD:s delar är avsnitt på huvudsidan.
		assertEquals(W + "Recipe_for_Disaster",
			WikiLinks.url(new QuestRequirement(2310, "Recipe for Disaster - Pirate Pete")));
	}

	@Test
	public void skillDiaryAndOtherPages()
	{
		assertEquals(W + "Runecraft", WikiLinks.url(SkillRequirement.level(Skill.RUNECRAFT, 50)));
		assertEquals(W + "Lumbridge_%26_Draynor_Diary",
			WikiLinks.url(new DiaryRequirement(DiaryArea.LUMBRIDGE, DiaryTier.ELITE)));
		assertEquals(W + "Combat_Achievements", WikiLinks.url(new CombatAchievementRequirement(17, "Noxious Foe", "Easy")));
		assertEquals(W + "Quest_points", WikiLinks.url(new QuestPointsRequirement(175)));
	}

	@Test
	public void killCountLinksToTheBossOrNpc()
	{
		assertEquals(W + "Zulrah", WikiLinks.url(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 100)));
		assertEquals(W + "Kree%27Arra", WikiLinks.url(KillCountRequirement.hiscore(HiscoreSkill.KREEARRA, 100)));
		assertEquals(W + "Barrows", WikiLinks.url(KillCountRequirement.hiscore(HiscoreSkill.BARROWS_CHESTS, 100)));
		assertEquals(W + "Treasure_Trails", WikiLinks.url(KillCountRequirement.hiscore(HiscoreSkill.CLUE_SCROLL_HARD, 100)));
		assertEquals(W + "Lizardman_shaman", WikiLinks.url(KillCountRequirement.npc("Lizardman shaman", 100)));
		assertNull(WikiLinks.url(KillCountRequirement.manual("Birdhouse runs", 10)));
	}

	@Test
	public void ownWikiPageWinsAndManualGoalsHaveNone()
	{
		Goal gloves = new Goal("");
		gloves.setRequirement(new ItemRequirement(7462, "Barrows gloves", 1, false));
		gloves.setWikiPage("Culinaromancer's Chest");
		assertEquals(W + "Culinaromancer%27s_Chest", WikiLinks.url(gloves));

		assertNull(WikiLinks.url(new Goal("Buy a house")));
	}

	@Test
	public void subpagesKeepTheirSlash()
	{
		assertEquals(W + "Recipe_for_Disaster/Another_Cook%27s_Quest", WikiLinks.page("Recipe for Disaster/Another Cook's Quest"));
	}
}
