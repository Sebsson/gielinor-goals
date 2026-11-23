package com.gielinorgoals.template;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class QuestRequirementsDbTest
{
	private final QuestRequirementsDb db = QuestRequirementsDb.load(new Gson());
	private final Map<Integer, String> enumNames = new HashMap<>();

	{
		for (Quest quest : Quest.values())
		{
			enumNames.put(quest.getId(), quest.getName());
		}
	}

	@Test
	public void priorityChainsAreLoaded()
	{
		for (Quest quest : new Quest[]{Quest.SONG_OF_THE_ELVES, Quest.RECIPE_FOR_DISASTER, Quest.DRAGON_SLAYER_II,
			Quest.DESERT_TREASURE_I, Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE, Quest.MONKEY_MADNESS_II})
		{
			assertNotNull(quest.getName(), db.get(quest.getId()));
		}
		assertEquals(Integer.valueOf(175), db.get(Quest.RECIPE_FOR_DISASTER.getId()).getQuestPoints());
	}

	@Test
	public void everyReferencePointsAtAnExistingQuestWithTheSameName()
	{
		for (QuestRequirementsDb.Entry entry : db.all().values())
		{
			assertEquals("entry " + entry.getId(), enumNames.get(entry.getId()), entry.getName());
			for (QuestRequirementsDb.Ref ref : entry.getQuests())
			{
				assertEquals(entry.getName() + " -> " + ref.getName(), enumNames.get(ref.getId()), ref.getName());
			}
			for (QuestRequirementsDb.Partial partial : entry.getPartial())
			{
				assertEquals(entry.getName() + " partial", enumNames.get(partial.getId()), partial.getName());
				assertTrue(partial.getText() != null && !partial.getText().isEmpty());
			}
		}
	}

	@Test
	public void everyPrerequisiteHasItsOwnEntry()
	{
		// Kedjorna ska kunna fällas ut hela vägen för de quests som finns i filen.
		List<String> missing = new ArrayList<>();
		for (QuestRequirementsDb.Entry entry : db.all().values())
		{
			for (QuestRequirementsDb.Ref ref : entry.getQuests())
			{
				if (db.get(ref.getId()) == null)
				{
					missing.add(entry.getName() + " -> " + ref.getName());
				}
			}
		}
		assertTrue("Prerequisites without an entry: " + missing, missing.isEmpty());
	}

	@Test
	public void skillNamesAndLevelsAreValid()
	{
		for (QuestRequirementsDb.Entry entry : db.all().values())
		{
			for (QuestRequirementsDb.SkillReq req : entry.getSkills())
			{
				try
				{
					Skill.valueOf(req.getSkill());
				}
				catch (IllegalArgumentException e)
				{
					fail(entry.getName() + " has unknown skill " + req.getSkill());
				}
				assertTrue(entry.getName() + " " + req.getSkill(), req.getLevel() >= 2 && req.getLevel() <= 99);
			}
		}
	}

	@Test
	public void thereAreNoCycles()
	{
		Set<Integer> done = new HashSet<>();
		for (int id : db.all().keySet())
		{
			visit(id, new HashSet<>(), done);
		}
	}

	private void visit(int id, Set<Integer> path, Set<Integer> done)
	{
		if (done.contains(id))
		{
			return;
		}
		if (!path.add(id))
		{
			fail("Cycle through " + enumNames.get(id));
		}
		QuestRequirementsDb.Entry entry = db.get(id);
		if (entry != null)
		{
			for (QuestRequirementsDb.Ref ref : entry.getQuests())
			{
				visit(ref.getId(), path, done);
			}
		}
		path.remove(id);
		done.add(id);
	}

	@Test
	public void questsMissingFromTheFileAreListedButDoNotFail()
	{
		List<String> missing = db.missingQuests();
		System.out.println("Quests without requirement data (" + missing.size() + " of " + Quest.values().length + "): "
			+ missing);
		assertNull(db.get(-12345));
	}
}
