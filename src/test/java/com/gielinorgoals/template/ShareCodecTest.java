package com.gielinorgoals.template;

import com.gielinorgoals.model.CombatAchievementRequirement;
import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.DropSource;
import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalEvaluator;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.SkillRequirement;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.client.hiscore.HiscoreSkill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * Delningssträngar: roundtrip, trasiga och fientliga strängar och ihopslagning av en hel profil.
 */
public class ShareCodecTest
{
	private final Gson gson = new Gson();
	private final ShareCodec codec = new ShareCodec(gson);

	@Test
	public void exportThenImportGivesTheSameStructure() throws Exception
	{
		GoalTree tree = sampleTree();
		Goal root = tree.getRoots().get(0);
		String share = codec.encode(TemplateExport.export(tree, root, "Raid prep", "Before the raid", List.of("raid")),
			ShareCodec.Kind.TEMPLATE);
		assertTrue(share.startsWith(ShareCodec.PREFIX));

		ShareCodec.Payload payload = codec.decode(share);
		assertEquals(ShareCodec.Kind.TEMPLATE, payload.getKind());
		assertEquals("Raid prep", payload.getName());
		assertEquals("Before the raid", payload.getTemplate().getDescription());
		assertEquals(List.of("raid"), payload.getTemplate().getTags());

		GoalTree fresh = new GoalTree();
		TemplateInserter.insert(fresh, payload.getTemplate().build(Map.of()));
		assertEquals(structure(tree), structure(fresh));
	}

	@Test
	public void importedDropSourceAndDoneWhenWork() throws Exception
	{
		GoalTree tree = sampleTree();
		ShareCodec.Payload payload = codec.decode(codec.encode(TemplateExport.export(tree, tree.getRoots().get(0),
			"Raid prep", "", List.of()), ShareCodec.Kind.TEMPLATE));
		GoalTree fresh = new GoalTree();
		TemplateInserter.insert(fresh, payload.getTemplate().build(Map.of()));

		Goal pet = find(fresh, "item:12921");
		assertEquals(4000, pet.getDropSource().getRate());
		assertEquals("hiscore:ZULRAH", pet.getDropSource().counterKey());
		Goal lower = find(fresh, "item:12954");
		assertEquals(List.of("item:13124"), lower.getDoneWhen());

		// Med cloak 4 i banken bockas den lägre av via doneWhen.
		GameData data = new GameData()
		{
			@Override
			public long getXp(Skill skill)
			{
				return -1;
			}

			@Override
			public long getItemCount(int itemId)
			{
				return itemId == 13124 ? 1 : 0;
			}
		};
		GoalEvaluator.evaluate(fresh, data, 1L, TemplateKeys::of);
		assertTrue(find(fresh, "item:13124").isCompleted());
		assertTrue(lower.isCompleted());
	}

	@Test
	public void brokenStringsGiveClearErrors() throws Exception
	{
		expectError("", "Paste a share string");
		expectError("hello world", "should start with GG1:");
		expectError("GG2:abcd", "newer version");
		expectError("GG1:not base64!!", "characters that don't belong");
		expectError("GG1:" + Base64.getEncoder().encodeToString("plain text".getBytes(StandardCharsets.UTF_8)),
			"can't be unpacked");

		GoalTree sample = sampleTree();
		String good = codec.encode(TemplateExport.export(sample, sample.getRoots().get(0), "Raid", "", List.of()),
			ShareCodec.Kind.TEMPLATE);
		byte[] packed = Base64.getDecoder().decode(good.substring(ShareCodec.PREFIX.length()));
		String truncated = ShareCodec.PREFIX + Base64.getEncoder().encodeToString(Arrays.copyOf(packed,
			packed.length / 2));
		expectError(truncated, "cut off");
		// Mellanslag och radbrytningar från kopieringen gör inget.
		assertEquals("Raid", codec.decode(good.substring(0, 30) + "\n  " + good.substring(30)).getName());

		// En gzip-bomb: liten sträng, stor uppackad.
		byte[] zeros = new byte[ShareCodec.MAX_UNPACKED_BYTES + 1024];
		expectError(ShareCodec.PREFIX + Base64.getEncoder().encodeToString(gzip(zeros)), "too large when unpacked");

		expectError(pack("{not json"), "can't be read");
		expectError(pack("[1,2,3]"), "not in the right format");
		expectError(pack("{\"formatVersion\":1,\"kind\":\"template\"}"), "no goals");
		expectError(pack("{\"formatVersion\":2,\"kind\":\"template\",\"template\":" + node("{\"title\":\"A\"}") + "}"),
			"newer version");
		expectError(pack(envelope("{\"title\":\"A\",\"monster\":\"Zulrah\"}")), "Unknown field \"monster\"");
		expectError(pack("{\"formatVersion\":1,\"kind\":\"template\",\"secret\":1,\"template\":"
			+ node("{\"title\":\"A\"}") + "}"), "Unknown field \"secret\"");
		expectError(pack(envelope("{\"title\":\"A\",\"children\":[{\"quest\":\"NOT_A_QUEST\"}]}")),
			"Unknown quest \"NOT_A_QUEST\"");
		expectError(pack(envelope("{\"title\":\"A\",\"children\":[{\"skill\":\"OVERALL\",\"level\":50}]}")),
			"Unknown skill");
		expectError(pack(envelope("{\"title\":\"A\",\"children\":[{\"skill\":\"COOKING\",\"level\":150}]}")),
			"Level must be between");
		expectError(pack(envelope("{\"title\":\"A\",\"children\":[{\"item\":{\"id\":-5,\"name\":\"X\"}}]}")),
			"not a valid item");
		expectError(pack(envelope("{\"title\":\"A\",\"children\":[{\"item\":{\"id\":9999999,\"name\":\"X\"}}]}")),
			"not a valid item");
		expectError(pack(envelope("{\"title\":\"A\",\"quest\":\"COOKS_ASSISTANT\",\"skill\":\"COOKING\",\"level\":5}")),
			"only have one requirement");
		expectError(pack(envelope("{\"title\":\"" + "x".repeat(ImportValidator.MAX_TITLE_LENGTH + 1) + "\"}")),
			"longer than");
		expectError(pack(envelope("{\"title\":\"A\",\"aggregate\":\"yes\"}")), "true or false");
		expectError(pack(envelope("{\"title\":\"A\",\"level\":\"ten\"}")), "should be a number");
		expectError(pack(envelope("{\"title\":\"A\",\"children\":{\"title\":\"B\"}}")), "should be a list");
		// Cykler: en nod som är sitt eget delmål.
		expectError(pack(envelope("{\"id\":\"a\",\"title\":\"A\",\"children\":[{\"title\":\"B\",\"children\":"
			+ "[{\"ref\":\"a\"}]}]}")), "cycle");
		expectError(pack(envelope(deep(ImportValidator.MAX_DEPTH + 1))), "levels deep");
		expectError(pack(envelope(wide(ImportValidator.MAX_NODES + 1))), "more than");
		expectError(pack("{\"formatVersion\":1,\"kind\":\"virus\",\"template\":" + node("{\"title\":\"A\"}") + "}"),
			"unknown kind");
	}

	@Test
	public void wholeProfileMergesWithoutDuplicates() throws Exception
	{
		GoalTree tree = sampleTree();
		Goal extra = new Goal("Second plan");
		tree.add(null, extra);
		Goal shared = find(tree, "group:bank trip");
		tree.link(extra.getId(), shared.getId());
		int before = tree.flatten().size();

		String share = codec.encode(TemplateExport.exportAll(tree), ShareCodec.Kind.GOALS);
		ShareCodec.Payload payload = codec.decode(share);
		assertEquals(ShareCodec.Kind.GOALS, payload.getKind());

		// Samma profil igen: allt finns redan, inget nytt.
		TemplateNode container = payload.getTemplate().build(Map.of());
		TemplatePlanner.Plan plan = TemplatePlanner.planMerge(tree, container, GameData.EMPTY);
		assertEquals(0, plan.getNewGoals());
		TemplateInserter.Result again = TemplateInserter.merge(tree, container);
		assertTrue(again.getCreated().isEmpty());
		assertEquals(before, tree.flatten().size());

		// En profil med en del av målen: bara det som saknas läggs till, och delade mål förblir delade.
		GoalTree partial = new GoalTree();
		Goal mine = new Goal("Raid prep");
		partial.add(null, mine);
		Goal myGear = new Goal("Gear");
		partial.add(mine.getId(), myGear);
		Goal myBank = new Goal("Bank trip");
		partial.add(myGear.getId(), myBank);
		Goal myRope = new Goal("");
		myRope.setRequirement(new ItemRequirement(954, "Rope", 1, false));
		partial.add(myBank.getId(), myRope);
		TemplateInserter.Result merged = TemplateInserter.merge(partial, payload.getTemplate().build(Map.of()));
		assertEquals(before, partial.flatten().size());
		assertEquals(2, partial.getRoots().size());
		assertTrue(merged.getLinked().containsAll(List.of(mine, myGear, myBank, myRope)));
		// Gear, Supplies och Second plan.
		assertEquals(3, partial.getParents(find(partial, "group:bank trip").getId()).size());
		assertEquals(structure(tree), structure(partial));
	}

	@Test
	public void importedGoalsAreNeverCompletedFromTheExport() throws Exception
	{
		GoalTree tree = sampleTree();
		for (Goal goal : tree.flatten())
		{
			goal.markCompleted(true, 5L, false);
		}
		ShareCodec.Payload payload = codec.decode(codec.encode(TemplateExport.exportAll(tree), ShareCodec.Kind.GOALS));
		GoalTree fresh = new GoalTree();
		TemplateInserter.merge(fresh, payload.getTemplate().build(Map.of()));
		for (Goal goal : fresh.flatten())
		{
			assertTrue(!goal.isCompleted() && goal.getCompletedAt() == null);
		}
	}

	// Ett mål med delat delmål, drop-källa, doneWhen, KC, CA, XP-mål, diary och quest.
	private static GoalTree sampleTree()
	{
		GoalTree tree = new GoalTree();
		Goal root = new Goal("Raid prep");
		tree.add(null, root);
		Goal gear = add(tree, root, new Goal("Gear"));
		Goal supplies = add(tree, root, new Goal("Supplies"));
		Goal bank = add(tree, gear, new Goal("Bank trip"));
		tree.link(supplies.getId(), bank.getId());
		add(tree, bank, requirement(new ItemRequirement(954, "Rope", 1, false)));
		Goal pet = add(tree, gear, requirement(new ItemRequirement(12921, "Pet snakeling", 1, false)));
		pet.setDropSource(DropSource.hiscore(HiscoreSkill.ZULRAH, 4000));
		Goal lower = add(tree, gear, requirement(new ItemRequirement(12954, "Dragon defender", 1, true)));
		lower.setDoneWhen(new ArrayList<>(List.of("item:13124")));
		add(tree, gear, requirement(new ItemRequirement(13124, "Ardougne cloak 4", 1, true)));
		add(tree, supplies, requirement(KillCountRequirement.npc("Lizardman shaman", 500)));
		add(tree, supplies, requirement(new CombatAchievementRequirement(12, "Noxious Foe", "Easy")));
		add(tree, supplies, requirement(SkillRequirement.xp(Skill.MAGIC, 13_034_431)));
		add(tree, supplies, requirement(new DiaryRequirement(DiaryArea.VARROCK, DiaryTier.HARD)));
		Goal ds = add(tree, root, requirement(new QuestRequirement(Quest.DRAGON_SLAYER_I.getId(),
			Quest.DRAGON_SLAYER_I.getName())));
		add(tree, ds, requirement(new QuestRequirement(Quest.RUNE_MYSTERIES.getId(), Quest.RUNE_MYSTERIES.getName())));
		Goal note = add(tree, root, new Goal("Ask a friend"));
		note.setNote("Bring food");
		note.setWikiPage("Chambers of Xeric");
		return tree;
	}

	// Varje mål som nyckel → föräldrarnas nycklar, krav, anteckning och flaggor; oberoende av id:n.
	private static Map<String, String> structure(GoalTree tree)
	{
		Map<String, String> out = new TreeMap<>();
		for (Goal goal : tree.flatten())
		{
			List<String> parents = new ArrayList<>();
			tree.getParents(goal.getId()).forEach(parent -> parents.add(TemplateKeys.of(parent) == null
				? "group:" + parent.getTitle().toLowerCase() : TemplateKeys.of(parent)));
			parents.sort(String::compareTo);
			Requirement requirement = goal.getRequirement();
			String key = TemplateKeys.of(goal) == null ? "group:" + goal.getTitle().toLowerCase() : TemplateKeys.of(goal);
			out.put(key, parents + " " + (requirement == null ? "-" : requirement.describe()) + " " + goal.getNote()
				+ " " + goal.getWikiPage() + " " + goal.getDoneWhen()
				+ " " + (goal.getDropSource() == null ? "-" : goal.getDropSource().getRate()));
		}
		return out;
	}

	private static Goal add(GoalTree tree, Goal parent, Goal goal)
	{
		tree.add(parent.getId(), goal);
		return goal;
	}

	private static Goal requirement(Requirement requirement)
	{
		Goal goal = new Goal("");
		goal.setRequirement(requirement);
		return goal;
	}

	private static Goal find(GoalTree tree, String key)
	{
		for (Goal goal : tree.flatten())
		{
			String goalKey = TemplateKeys.of(goal) == null ? "group:" + goal.getTitle().toLowerCase() : TemplateKeys.of(goal);
			if (key.equals(goalKey))
			{
				return goal;
			}
		}
		throw new AssertionError("No goal " + key);
	}

	private void expectError(String share, String message)
	{
		try
		{
			codec.decode(share);
			fail("Expected an error with: " + message);
		}
		catch (ShareCodec.ShareException e)
		{
			assertNotNull(e.getMessage());
			assertTrue(e.getMessage(), e.getMessage().contains(message));
		}
	}

	private static String envelope(String root)
	{
		return "{\"formatVersion\":1,\"kind\":\"template\",\"template\":" + node(root) + "}";
	}

	private static String node(String root)
	{
		return "{\"name\":\"Shared\",\"root\":" + root + "}";
	}

	private static String deep(int depth)
	{
		StringBuilder json = new StringBuilder();
		for (int i = 0; i < depth; i++)
		{
			json.append("{\"title\":\"L").append(i).append("\",\"children\":[");
		}
		json.append("{\"title\":\"leaf\"}");
		for (int i = 0; i < depth; i++)
		{
			json.append("]}");
		}
		return json.toString();
	}

	private static String wide(int count)
	{
		StringBuilder json = new StringBuilder("{\"title\":\"Root\",\"children\":[");
		for (int i = 0; i < count; i++)
		{
			json.append(i == 0 ? "" : ",").append("{\"title\":\"G").append(i).append("\"}");
		}
		return json.append("]}").toString();
	}

	private static String pack(String json) throws IOException
	{
		return ShareCodec.PREFIX + Base64.getEncoder().encodeToString(gzip(json.getBytes(StandardCharsets.UTF_8)));
	}

	private static byte[] gzip(byte[] data) throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream out = new GZIPOutputStream(bytes))
		{
			out.write(data);
		}
		return bytes.toByteArray();
	}
}
