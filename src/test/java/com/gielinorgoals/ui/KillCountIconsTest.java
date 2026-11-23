package com.gielinorgoals.ui;

import com.gielinorgoals.model.CompletedLog;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.KillCountRequirement;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class KillCountIconsTest
{
	@Test
	public void everyBossHasItsOwnIcon()
	{
		// En ny boss i RuneLite utan rad i listan märks här i stället för att tyst få combat-ikonen.
		List<HiscoreSkill> missing = new ArrayList<>();
		for (HiscoreSkill skill : HiscoreSkill.values())
		{
			if (skill.getType() == HiscoreSkillType.BOSS && KillCountIcons.spriteFor(skill) == SpriteID.SideIcons.COMBAT)
			{
				missing.add(skill);
			}
		}
		assertTrue("Bosses without icon: " + missing, missing.isEmpty());
	}

	@Test
	public void npcAndManualCountersUseSideIcons()
	{
		assertEquals(SpriteID.SideIcons.COMBAT, KillCountIcons.spriteFor(KillCountRequirement.npc("Cave kraken", 10)));
		assertEquals(SpriteID.SideIcons.STATS, KillCountIcons.spriteFor(KillCountRequirement.manual("Runs", 10)));
		assertEquals(SpriteID.IconActivities25x25.CLUE_SCROLL_ALL,
			KillCountIcons.spriteFor(KillCountRequirement.hiscore(HiscoreSkill.CLUE_SCROLL_HARD, 10)));
	}

	@Test
	public void completedFilterFindsKillCountGoals()
	{
		GoalTree tree = new GoalTree();
		Goal kc = new Goal("");
		kc.setRequirement(KillCountRequirement.hiscore(HiscoreSkill.ZULRAH, 100));
		Goal other = new Goal("Other");
		tree.add(null, kc);
		tree.add(null, other);
		kc.markCompleted(true, 1L);
		other.markCompleted(true, 1L);

		List<CompletedLog.Section> sections = CompletedLog.build(tree, CompletedLog.TypeFilter.KILL_COUNT, false, true,
			2L, ZoneId.of("UTC"));

		assertEquals(1, sections.get(0).getEntries().size());
		assertEquals("100 Zulrah kills", sections.get(0).getEntries().get(0).getGoal().getDisplayTitle());
	}
}
