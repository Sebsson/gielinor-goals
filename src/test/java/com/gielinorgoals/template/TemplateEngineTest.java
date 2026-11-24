package com.gielinorgoals.template;

import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.SkillRequirement;
import com.gielinorgoals.persistence.GoalSerializer;
import com.gielinorgoals.persistence.LoadResult;
import com.gielinorgoals.persistence.SchemaMigrator;
import com.google.gson.Gson;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * Motorutökningarna i milstolpe 11: valparametrar, asynkront bygge och quest-serier.
 */
public class TemplateEngineTest
{
	private static final List<TemplateParameter.Option> SKILLS = List.of(
		new TemplateParameter.Option("ATTACK", "Attack"),
		new TemplateParameter.Option("MINING", "Mining"));

	@Test
	public void choiceParameterFallsBackToItsDefault()
	{
		TemplateParameter skill = TemplateParameter.choice("skill", "Skill", SKILLS, "ATTACK");

		assertEquals(TemplateParameter.Kind.CHOICE, skill.getKind());
		assertEquals("MINING", skill.choiceIn(Map.of("skill", "MINING")));
		assertEquals("ATTACK", skill.choiceIn(Map.of()));
		// Ett värde som inte finns längre (t.ex. i gammal sparad data) ger standardvalet.
		assertEquals("ATTACK", skill.choiceIn(Map.of("skill", "SAILING")));

		TemplateParameter level = TemplateParameter.number("level", "Level", 1, 99, 70);
		assertEquals(TemplateParameter.Kind.NUMBER, level.getKind());
		assertEquals(99, level.valueIn(Map.of("level", 120)));
		assertEquals(70, level.valueIn(Map.of()));

		try
		{
			TemplateParameter.choice("skill", "Skill", SKILLS, "FISHING");
			fail("The default must be one of the options");
		}
		catch (IllegalArgumentException expected)
		{
			// Väntat.
		}
	}

	@Test
	public void choicesAreSavedAndUsedByUpdateFromTemplate()
	{
		SkillTemplate template = new SkillTemplate();
		TemplateNode node = template.build(Map.of(), Map.of("skill", "MINING"));
		node.setTemplateId(template.getId());
		node.setTemplateParams(Map.of());
		node.setTemplateChoices(Map.of("skill", "MINING"));
		GoalTree tree = new GoalTree();
		Goal root = TemplateInserter.insert(tree, node, null).getRoot();
		assertEquals(Map.of("skill", "MINING"), root.getTemplateChoices());

		TemplateUpdates updates = new TemplateUpdates(new TemplateCatalog(List.of(template)),
			QuestRequirementsDb.load(new Gson()));
		// Ombyggd med samma val: inget nytt att föreslå.
		TemplateNode rebuilt = updates.rebuild(root);
		assertEquals(SkillRequirement.level(Skill.MINING, 70), rebuilt.getChildren().get(0).getRequirement());
		assertTrue(updates.plan(tree, root).isEmpty());

		// Äldre rötter utan sparade val får standardvalet.
		root.setTemplateChoices(null);
		assertEquals(SkillRequirement.level(Skill.ATTACK, 70), updates.rebuild(root).getChildren().get(0).getRequirement());
	}

	@Test
	public void choicesRoundTripAndVersion11Upgrades()
	{
		GoalSerializer serializer = new GoalSerializer(new Gson(), new SchemaMigrator());
		String v11 = "{\"schemaVersion\":11,\"rootIds\":[\"a\"],\"goals\":[{\"id\":\"a\",\"title\":\"Skill ladder\","
			+ "\"templateId\":\"skill\",\"templateParams\":{\"questChains\":0},\"childIds\":[]}]}";

		LoadResult result = serializer.deserialize(v11);
		assertTrue(result.isMigrated());
		Goal root = result.getTree().find("a");
		assertNull(root.getTemplateChoices());
		assertEquals(Integer.valueOf(0), root.getTemplateParams().get("questChains"));

		root.setTemplateChoices(Map.of("skill", "MINING"));
		LoadResult again = serializer.deserialize(serializer.serialize(result.getTree()));
		assertFalse(again.isMigrated());
		assertEquals(Map.of("skill", "MINING"), again.getTree().find("a").getTemplateChoices());
		assertTrue(SchemaMigrator.CURRENT_VERSION >= 12);
	}

	@Test
	public void staticTemplatesAreBuiltAtOnce()
	{
		List<TemplateBuilder.Result> results = new ArrayList<>();
		TemplateBuilder builder = new TemplateBuilder(r -> fail("no client thread needed"), r -> fail("no EDT hop"));

		builder.build(new SkillTemplate(), Map.of(), Map.of(), results::add);

		assertEquals(1, results.size());
		assertTrue(results.get(0).isReady());
	}

	@Test
	public void clientDataTemplatesAreBuiltOnTheClientThreadThenHandedToTheEdt()
	{
		Deque<Runnable> clientThread = new ArrayDeque<>();
		Deque<Runnable> edt = new ArrayDeque<>();
		TemplateBuilder builder = new TemplateBuilder(clientThread::add, edt::add);
		ClientTemplate template = new ClientTemplate();
		List<TemplateBuilder.Result> results = new ArrayList<>();

		builder.build(template, Map.of(), Map.of(), results::add);
		// Inget byggs förrän klienttråden kör.
		assertTrue(results.isEmpty());
		assertEquals(0, template.builds);
		clientThread.poll().run();
		assertEquals(1, template.builds);
		assertTrue(results.isEmpty());
		edt.poll().run();
		assertEquals(1, results.size());
		assertTrue(results.get(0).isReady());

		// Utloggad: ett meddelande i stället för ett förslag.
		template.loggedIn = false;
		builder.build(template, Map.of(), Map.of(), results::add);
		clientThread.poll().run();
		edt.poll().run();
		assertFalse(results.get(1).isReady());
		assertEquals(TemplateUnavailableException.LOG_IN, results.get(1).getMessage());

		// Inloggad men utan data: samma meddelande, ingen krasch.
		template.loggedIn = true;
		template.hasData = false;
		builder.build(template, Map.of(), Map.of(), results::add);
		clientThread.poll().run();
		edt.poll().run();
		assertFalse(results.get(2).isReady());
	}

	@Test
	public void clientDataTemplatesAreNotRebuiltOnTheEdt()
	{
		ClientTemplate template = new ClientTemplate();
		TemplateNode node = template.build(Map.of(), Map.of());
		node.setTemplateId(template.getId());
		GoalTree tree = new GoalTree();
		Goal root = TemplateInserter.insert(tree, node, null).getRoot();
		TemplateUpdates updates = new TemplateUpdates(new TemplateCatalog(List.of(template)),
			QuestRequirementsDb.load(new Gson()));
		int before = template.builds;

		// Radens uppdateringsmärke räknas på EDT och får inte läsa spelets data.
		assertNull(updates.rebuild(root));
		assertFalse(updates.hasUpdate(tree, root, 1));
		assertEquals(before, template.builds);
	}

	@Test
	public void questSeriesAreInTheDatabaseInWikiOrder()
	{
		QuestRequirementsDb db = QuestRequirementsDb.load(new Gson());
		assertEquals(24, db.getSeries().size());
		for (QuestRequirementsDb.Series series : db.getSeries())
		{
			assertFalse(series.getName(), series.getQuests().isEmpty());
			for (QuestRequirementsDb.Ref quest : series.getQuests())
			{
				assertTrue(series.getName() + ": " + quest.getName(), db.get(quest.getId()) != null);
				assertEquals(quest.getName(), db.get(quest.getId()).getName());
			}
		}
		List<String> elf = names(db.series("Elf"));
		assertEquals("Plague City", elf.get(0));
		assertEquals("Song of the Elves", elf.get(elf.size() - 1));
		assertEquals(List.of("Children of the Sun", "Twilight's Promise", "The Heart of Darkness", "The Final Dawn"),
			names(db.series("Twilight Emissaries")));
		assertNull(db.series("No such series"));
	}

	@Test
	public void unverifiedTemplatesAreListed()
	{
		// Listar bara; ett test som inte misslyckas, så att det syns vilka som återstår.
		List<String> unverified = new ArrayList<>();
		for (GoalTemplate template : JsonTemplates.load(new Gson()))
		{
			if (!((JsonTemplate) template).isVerified())
			{
				unverified.add(template.getId());
			}
		}
		System.out.println("Templates not yet verified against the wiki (" + unverified.size() + "): " + unverified);
	}

	private static List<String> names(QuestRequirementsDb.Series series)
	{
		List<String> names = new ArrayList<>();
		series.getQuests().forEach(quest -> names.add(quest.getName()));
		return names;
	}

	// Liten template med en valparameter: en skill till 70.
	private static final class SkillTemplate implements GoalTemplate
	{
		private static final TemplateParameter SKILL = TemplateParameter.choice("skill", "Skill", SKILLS, "ATTACK");

		@Override
		public String getId()
		{
			return "skill";
		}

		@Override
		public String getName()
		{
			return "Skill";
		}

		@Override
		public String getDescription()
		{
			return "";
		}

		@Override
		public String getCategory()
		{
			return TemplateCategories.SKILLS;
		}

		@Override
		public List<TemplateParameter> getParameters()
		{
			return List.of(SKILL);
		}

		@Override
		public TemplateNode build(Map<String, Integer> parameters)
		{
			return build(parameters, Map.of());
		}

		@Override
		public TemplateNode build(Map<String, Integer> parameters, Map<String, String> choices)
		{
			Skill skill = Skill.valueOf(SKILL.choiceIn(choices));
			return TemplateNode.aggregate("Skill").add(TemplateNode.requirement(SkillRequirement.level(skill, 70)));
		}
	}

	// En template som behöver spelets data; läget styrs av testet.
	private static final class ClientTemplate implements GoalTemplate
	{
		boolean loggedIn = true;
		boolean hasData = true;
		int builds;

		@Override
		public String getId()
		{
			return "client";
		}

		@Override
		public String getName()
		{
			return "Client";
		}

		@Override
		public String getDescription()
		{
			return "";
		}

		@Override
		public String getCategory()
		{
			return TemplateCategories.OTHER;
		}

		@Override
		public boolean needsClientData()
		{
			return true;
		}

		@Override
		public TemplateNode build(Map<String, Integer> parameters)
		{
			builds++;
			if (!loggedIn || !hasData)
			{
				throw new TemplateUnavailableException(TemplateUnavailableException.LOG_IN);
			}
			return TemplateNode.aggregate("Client").add(new TemplateNode("Task", null, false));
		}
	}
}
