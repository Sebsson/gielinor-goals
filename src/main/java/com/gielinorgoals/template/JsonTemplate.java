/*
 * Copyright (c) 2026, Sebas
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gielinorgoals.template;

import com.gielinorgoals.model.CombatLevelRequirement;
import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.DropSource;
import com.gielinorgoals.model.GoalIcon;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.PointsActivity;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.SkillRequirement;
import com.gielinorgoals.model.TotalLevelRequirement;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.client.hiscore.HiscoreSkill;

/**
 * En handplockad template från en JSON-fil i resources. Formatet är gjort för att
 * skrivas för hand: varje nod har valfri titel, valfri räknarflagga, högst ett krav och
 * valfria barn. Krav anges med spelets enum-namn, t.ex.
 * <pre>
 * {"quest": "RECIPE_FOR_DISASTER"}
 * {"skill": "COOKING", "level": 70}
 * {"questPoints": 175}   {"totalLevel": 1500}   {"combatLevel": 100}
 * {"diary": {"area": "VARROCK", "tier": "HARD"}}
 * {"item": {"id": 7462, "name": "Barrows gloves", "quantity": 1, "includeVariants": true}}
 * {"killCount": {"hiscore": "ZULRAH", "count": 100}}
 * {"killCount": {"npc": "Lizardman shaman", "count": 500}}
 * {"killCount": {"manual": "Slayer points", "count": 750}}
 * </pre>
 * En nod utan krav är ett manuellt mål (eller en grupp med "aggregate"). Varje nod kan ha
 * "note" (visas i hover-texten) och "wikiPage". Quest-noder kan ha "expandChain": false
 * för att inte fällas ut med sin kedja. Item-noder kan ha en drop-källa för torrhetsmätaren:
 * <pre>
 * "dropSource": {"npc": "Lizardman shaman", "rate": 5000}
 * </pre>
 * Noder utan krav kan ha en egen ikon: {@code "icon": {"item": 25865}},
 * {@code {"activity": "THE_HUEYCOATL"}} eller {@code {"sideIcon": "QUEST"}}.
 * {@code "doneWhen": ["item:12954"]} gör noden klar när ett mål med någon av nycklarna
 * (se {@link TemplateKeys}) är klart, t.ex. en lägre defender när dragon defendern finns.
 * Filen valideras när den läses; en trasig fil ger ingen template alls, aldrig en halv.
 * <p>
 * Egna templates (sparade från ett mål) använder samma format, med två tillägg: en nod med
 * {@code "id": "n1"} kan återanvändas under en till förälder med en nod som bara har
 * {@code "ref": "n1"}, så att delade delmål förblir delade, och skill-krav kan ha
 * {@code "xp"} i stället för {@code "level"}. Combat achievements skrivs
 * {@code {"combatAchievement": {"id": 12, "name": "Noxious Foe", "tier": "Easy"}}}.
 * {@code "formatVersion"} är formatets egen version, skild från profilens schemaversion.
 * <p>
 * Poäng som spelet håller i en varbit eller varp skrivs
 * {@code {"points": {"activity": "SLAYER", "count": 750}}} (se {@link PointsActivity}).
 * {@code "version"} är templatens eget innehåll: den höjs när en handplockad template ändras
 * så att uppdateringsdialogen kan säga det. {@code "replaces": ["kc:manual:slayer points:750"]}
 * på en nod betyder att den ersätter ett mål med den nyckeln från en äldre version; dialogen
 * visar då bytet som en ändring i stället för ett nytt och ett borttaget mål.
 */
public class JsonTemplate implements GoalTemplate
{
	// Fälten fylls av Gson.
	static final class FileSpec
	{
		// Formatets version; saknas i de handplockade filerna och betyder då 1.
		Integer formatVersion;
		// Om "Show full quest chains" är ibockad från början; saknas = ja.
		Boolean expandQuestChains;
		// Templatens innehållsversion; saknas = 1. Höjs när en handplockad template ändras.
		Integer version;
		// Stabilt id; om det saknas används filnamnet utan .json.
		String id;
		// Vem som har kontrollerat siffrorna mot wikin och när (t.ex. "2026-11-24"); tomt = inte än.
		String verifiedBy;
		String verifiedAt;
		String name;
		String description;
		// En av kategorierna i TemplateCategories, t.ex. "Ironman · Mid".
		String category;
		List<String> tags;
		NodeSpec root;
	}

	static final class NodeSpec
	{
		// Lokalt id så att noden kan delas med "ref" från en annan förälder.
		String id;
		// Bara "ref": samma nod som den med det id:t, som delmål även här.
		String ref;
		String title;
		Boolean aggregate;
		// Egen sida på OSRS Wiki för "Open on wiki"; går före den som härleds ur kravet.
		String wikiPage;
		// Visas i hover-texten, t.ex. "Bought for 750 slayer points".
		String note;
		// Bara på quest-noder; false = fäll inte ut kedjan för just den här questen.
		Boolean expandChain;
		// Bara på item-noder.
		ActivitySpec dropSource;
		ActivitySpec killCount;
		IconSpec icon;
		// Nycklar till andra mål, t.ex. "item:12954"; noden är klar när något av dem är klart.
		List<String> doneWhen;
		// Nycklar för mål i en äldre version som den här noden ersätter.
		List<String> replaces;
		PointsSpec points;
		String quest;
		String skill;
		Integer level;
		Long xp;
		CombatAchievementSpec combatAchievement;
		Integer questPoints;
		Integer totalLevel;
		Integer combatLevel;
		DiarySpec diary;
		ItemSpec item;
		List<NodeSpec> children;
	}

	static final class PointsSpec
	{
		String activity;
		Integer count;
	}

	static final class CombatAchievementSpec
	{
		Integer id;
		String name;
		String tier;
	}

	static final class DiarySpec
	{
		String area;
		String tier;
	}

	static final class ItemSpec
	{
		Integer id;
		String name;
		Long quantity;
		// "includeVariants" som i målens sparformat; "variants" finns kvar för äldre filer.
		Boolean includeVariants;
		Boolean variants;
		// Föremål som redan har förbrukat det här, t.ex. crystal legs = 2 armour seeds.
		List<CountsAlsoSpec> countsAlso;
	}

	static final class CountsAlsoSpec
	{
		Integer item;
		Long each;
		Boolean includeVariants;
	}

	// Exakt en av item (item-id), activity (HiscoreSkill-namn) eller sideIcon (GoalIcon.SideIcon).
	static final class IconSpec
	{
		Integer item;
		String activity;
		String sideIcon;
	}

	// En aktivitet: exakt en av hiscore (HiscoreSkill-namn), npc eller manual (räknarnamn).
	// "count" används av killCount, "rate" (1/rate) av dropSource.
	static final class ActivitySpec
	{
		String hiscore;
		String npc;
		String manual;
		Integer count;
		Integer rate;
		// Bara dropSource: föremål per drop, t.ex. 3 Hueycoatl hides.
		Integer perDrop;
	}

	private final String id;
	private final String name;
	private final String description;
	private String category = TemplateCategories.OTHER;
	private List<String> tags = java.util.Collections.emptyList();
	private final NodeSpec root;
	private boolean verified;
	private boolean expandQuestChains = true;
	private int version = 1;

	/**
	 * Formatets nuvarande version. En fil med högre version kommer från en nyare version av
	 * pluginet och läses inte, hellre än att tolkas fel.
	 */
	public static final int FORMAT_VERSION = 1;

	private JsonTemplate(String id, String name, String description, NodeSpec root)
	{
		this.id = id;
		this.name = name;
		this.description = description;
		this.root = root;
	}

	/**
	 * @throws IllegalArgumentException om filen saknar fält eller har okända namn
	 */
	static JsonTemplate from(FileSpec spec)
	{
		return from(spec, null);
	}

	/**
	 * @param fallbackId id om filen inte anger något, normalt filnamnet utan .json
	 */
	static JsonTemplate from(FileSpec spec, @Nullable String fallbackId)
	{
		if (spec == null || spec.name == null || spec.root == null)
		{
			throw new IllegalArgumentException("Template needs a name and a root");
		}
		if (spec.formatVersion != null && (spec.formatVersion < 1 || spec.formatVersion > FORMAT_VERSION))
		{
			throw new IllegalArgumentException("Template format " + spec.formatVersion
				+ " is from a newer version of Gielinor Goals");
		}
		String id = spec.id != null ? spec.id
			: fallbackId != null ? fallbackId
			: spec.name.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
		JsonTemplate template = new JsonTemplate(id, spec.name, spec.description == null ? "" : spec.description,
			spec.root);
		if (spec.category != null)
		{
			template.category = spec.category;
		}
		if (spec.tags != null)
		{
			template.tags = java.util.List.copyOf(spec.tags);
		}
		template.verified = notBlank(spec.verifiedBy) && notBlank(spec.verifiedAt);
		template.expandQuestChains = !Boolean.FALSE.equals(spec.expandQuestChains);
		if (spec.version != null)
		{
			if (spec.version < 1)
			{
				throw new IllegalArgumentException("Template version must be 1 or higher");
			}
			template.version = spec.version;
		}
		// Bygg en gång direkt, så att fel i filen märks när den läses och inte i dialogen.
		template.build(Map.of());
		return template;
	}

	@Override
	public String getId()
	{
		return id;
	}

	/**
	 * Om någon har kontrollerat templatens siffror mot wikin (verifiedBy och verifiedAt satta).
	 */
	public boolean isVerified()
	{
		return verified;
	}

	private static boolean notBlank(@Nullable String value)
	{
		return value != null && !value.trim().isEmpty();
	}

	@Override
	public String getCategory()
	{
		return category;
	}

	@Override
	public List<String> getTags()
	{
		return tags;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Nullable
	@Override
	public Requirement getIconRequirement()
	{
		return toRequirement(root);
	}

	@Nullable
	@Override
	public GoalIcon getIcon()
	{
		return root.icon == null ? null : toIcon(root.icon);
	}

	@Override
	public int getVersion()
	{
		return version;
	}

	// Handplockade templates visar hela quest-kedjorna som standard; egna sparar sina kedjor.
	@Override
	public boolean defaultExpandQuestChains()
	{
		return expandQuestChains;
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		return toNode(root, new java.util.HashMap<>(), new java.util.HashSet<>());
	}

	/**
	 * Ett fel i en nod, med var i templaten noden finns; bara den innersta noden anges.
	 */
	static final class NodeException extends IllegalArgumentException
	{
		NodeException(String message)
		{
			super(message);
		}
	}

	// Kort beskrivning av en nod för felmeddelanden: titeln, kravet eller "a goal".
	private static String describe(NodeSpec spec)
	{
		if (spec.title != null && !spec.title.trim().isEmpty())
		{
			return "\"" + abbreviate(spec.title.trim()) + "\"";
		}
		if (spec.quest != null)
		{
			return "quest " + abbreviate(spec.quest);
		}
		if (spec.item != null && spec.item.name != null)
		{
			return "\"" + abbreviate(spec.item.name) + "\"";
		}
		if (spec.skill != null)
		{
			return "skill " + abbreviate(spec.skill);
		}
		return spec.ref != null ? "a shared goal" : "a goal";
	}

	private static String abbreviate(String text)
	{
		return text.length() <= 40 ? text : text.substring(0, 40) + "...";
	}

	// Enum-värde ur filen, med ett begripligt fel i stället för "No enum constant".
	private static <E extends Enum<E>> E enumOf(Class<E> type, @Nullable String name, String what)
	{
		if (name != null)
		{
			for (E value : type.getEnumConstants())
			{
				if (value.name().equals(name))
				{
					return value;
				}
			}
		}
		throw new IllegalArgumentException("Unknown " + what + " \"" + (name == null ? "" : abbreviate(name)) + "\"");
	}

	/**
	 * @param defined noder med id som redan är byggda
	 * @param open    id:n för noderna på vägen hit; en ref till någon av dem vore en cykel
	 */
	private static TemplateNode toNode(NodeSpec spec, Map<String, TemplateNode> defined, java.util.Set<String> open)
	{
		if (spec == null)
		{
			throw new IllegalArgumentException("A goal in the template is empty");
		}
		try
		{
			return toCheckedNode(spec, defined, open);
		}
		catch (NodeException e)
		{
			throw e;
		}
		catch (IllegalArgumentException e)
		{
			throw new NodeException("In " + describe(spec) + ": " + e.getMessage());
		}
	}

	private static TemplateNode toCheckedNode(NodeSpec spec, Map<String, TemplateNode> defined,
		java.util.Set<String> open)
	{
		if (spec.ref != null)
		{
			return toRef(spec, defined, open);
		}
		if (spec.id != null)
		{
			if (spec.id.trim().isEmpty() || defined.containsKey(spec.id) || !open.add(spec.id))
			{
				throw new IllegalArgumentException("Node id \"" + spec.id + "\" is empty or used twice");
			}
		}
		TemplateNode node = toPlainNode(spec, defined, open);
		if (spec.id != null)
		{
			node.setLocalId(spec.id);
			open.remove(spec.id);
			defined.put(spec.id, node);
		}
		return node;
	}

	// En nod som bara pekar på en tidigare nod: samma titel och krav, inga egna barn.
	private static TemplateNode toRef(NodeSpec spec, Map<String, TemplateNode> defined, java.util.Set<String> open)
	{
		if (spec.id != null || spec.title != null || spec.aggregate != null || spec.children != null
			|| spec.note != null || spec.wikiPage != null || spec.icon != null || spec.doneWhen != null
			|| spec.dropSource != null || spec.expandChain != null || spec.replaces != null
			|| toRequirement(spec) != null)
		{
			throw new IllegalArgumentException("A node with \"ref\" can't have anything else");
		}
		if (open.contains(spec.ref))
		{
			throw new IllegalArgumentException("Node \"" + spec.ref + "\" is its own sub-goal (a cycle)");
		}
		TemplateNode target = defined.get(spec.ref);
		if (target == null)
		{
			throw new IllegalArgumentException("ref \"" + spec.ref + "\" doesn't point to an earlier node");
		}
		TemplateNode node = new TemplateNode(target.getTitle(), target.getRequirement(), target.isAggregate());
		node.setRef(spec.ref);
		node.setIcon(target.getIcon());
		node.setWikiPage(target.getWikiPage());
		// Kedjan fälls ut (om alls) där noden definieras, inte en gång till här.
		node.setExpandChain(false);
		return node;
	}

	private static TemplateNode toPlainNode(NodeSpec spec, Map<String, TemplateNode> defined, java.util.Set<String> open)
	{
		Requirement requirement = toRequirement(spec);
		String title = spec.title == null ? "" : spec.title;
		if (title.isEmpty() && requirement == null)
		{
			throw new IllegalArgumentException("A node needs a title or a requirement");
		}
		TemplateNode node = new TemplateNode(title, requirement, Boolean.TRUE.equals(spec.aggregate));
		node.setWikiPage(spec.wikiPage);
		node.setNote(spec.note == null || spec.note.trim().isEmpty() ? null : spec.note.trim());
		if (spec.expandChain != null)
		{
			if (!(requirement instanceof QuestRequirement))
			{
				throw new IllegalArgumentException("expandChain is only for quest nodes");
			}
			node.setExpandChain(spec.expandChain);
		}
		if (spec.icon != null)
		{
			node.setIcon(toIcon(spec.icon));
		}
		if (spec.doneWhen != null)
		{
			for (String key : spec.doneWhen)
			{
				if (key == null || key.trim().isEmpty() || !key.equals(key.trim()) || !key.contains(":"))
				{
					throw new IllegalArgumentException("doneWhen needs keys like \"item:12954\"");
				}
			}
			node.setDoneWhen(List.copyOf(spec.doneWhen));
		}
		if (spec.replaces != null)
		{
			node.setReplaces(List.copyOf(keys(spec.replaces, "replaces")));
		}
		if (spec.dropSource != null)
		{
			if (!(requirement instanceof ItemRequirement))
			{
				throw new IllegalArgumentException("dropSource is only for item nodes");
			}
			node.setDropSource(toDropSource(spec.dropSource));
		}
		if (spec.children != null)
		{
			for (NodeSpec child : spec.children)
			{
				node.add(toNode(child, defined, open));
			}
		}
		return node;
	}

	// Högsta item-id som godtas; spelets id:n ligger långt under.
	static final int MAX_ITEM_ID = 100_000;

	@Nullable
	private static Requirement toRequirement(NodeSpec spec)
	{
		int kinds = count(spec.quest, spec.skill, spec.combatAchievement, spec.questPoints, spec.totalLevel,
			spec.combatLevel, spec.diary, spec.item, spec.killCount, spec.points);
		if (kinds > 1)
		{
			throw new IllegalArgumentException("A goal can only have one requirement");
		}
		if (spec.skill == null && (spec.level != null || spec.xp != null))
		{
			throw new IllegalArgumentException("level and xp need a skill");
		}
		if (spec.item != null && spec.item.id != null && (spec.item.id < 0 || spec.item.id > MAX_ITEM_ID))
		{
			throw new IllegalArgumentException("Item id " + spec.item.id + " is not a valid item");
		}
		if (spec.quest != null)
		{
			Quest quest = enumOf(Quest.class, spec.quest, "quest");
			return new QuestRequirement(quest.getId(), quest.getName());
		}
		if (spec.skill != null)
		{
			if ((spec.level == null) == (spec.xp == null))
			{
				throw new IllegalArgumentException("Skill " + spec.skill + " needs a level or an xp target");
			}
			Skill skill = enumOf(Skill.class, spec.skill, "skill");
			if ("OVERALL".equals(skill.name()))
			{
				throw new IllegalArgumentException("Unknown skill \"OVERALL\"");
			}
			return spec.level != null ? SkillRequirement.level(skill, spec.level) : SkillRequirement.xp(skill, spec.xp);
		}
		if (spec.points != null)
		{
			if (spec.points.count == null)
			{
				throw new IllegalArgumentException("points needs a count");
			}
			return new com.gielinorgoals.model.PointsRequirement(
				enumOf(PointsActivity.class, spec.points.activity, "points activity"), spec.points.count);
		}
		if (spec.combatAchievement != null)
		{
			CombatAchievementSpec ca = spec.combatAchievement;
			if (ca.id == null || ca.id < 0)
			{
				throw new IllegalArgumentException("combatAchievement needs a task id");
			}
			return new com.gielinorgoals.model.CombatAchievementRequirement(ca.id, ca.name, ca.tier);
		}
		if (spec.questPoints != null)
		{
			return new QuestPointsRequirement(spec.questPoints);
		}
		if (spec.totalLevel != null)
		{
			return new TotalLevelRequirement(spec.totalLevel);
		}
		if (spec.combatLevel != null)
		{
			return new CombatLevelRequirement(spec.combatLevel);
		}
		if (spec.diary != null)
		{
			return new DiaryRequirement(enumOf(DiaryArea.class, spec.diary.area, "diary area"),
				enumOf(DiaryTier.class, spec.diary.tier, "diary tier"));
		}
		if (spec.item != null)
		{
			if (spec.item.id == null || spec.item.name == null)
			{
				throw new IllegalArgumentException("Item needs an id and a name");
			}
			ItemRequirement item = new ItemRequirement(spec.item.id, spec.item.name,
				spec.item.quantity == null ? 1 : spec.item.quantity,
				Boolean.TRUE.equals(spec.item.includeVariants) || Boolean.TRUE.equals(spec.item.variants));
			return spec.item.countsAlso == null ? item : item.withCountsAlso(countsAlso(spec.item.countsAlso));
		}
		if (spec.killCount != null)
		{
			return toKillCount(spec.killCount);
		}
		return null;
	}

	private static List<ItemRequirement.CountsAlso> countsAlso(List<CountsAlsoSpec> specs)
	{
		List<ItemRequirement.CountsAlso> list = new java.util.ArrayList<>();
		for (CountsAlsoSpec spec : specs)
		{
			if (spec == null || spec.item == null || spec.each == null)
			{
				throw new IllegalArgumentException("countsAlso needs an item and an each");
			}
			if (spec.item < 0 || spec.item > MAX_ITEM_ID)
			{
				throw new IllegalArgumentException("Item id " + spec.item + " is not a valid item");
			}
			list.add(new ItemRequirement.CountsAlso(spec.item, spec.each, Boolean.TRUE.equals(spec.includeVariants)));
		}
		return list;
	}

	// Nycklar som "item:12954", kontrollerade på samma sätt som doneWhen.
	private static List<String> keys(List<String> keys, String field)
	{
		for (String key : keys)
		{
			if (key == null || key.trim().isEmpty() || !key.equals(key.trim()) || !key.contains(":"))
			{
				throw new IllegalArgumentException(field + " needs keys like \"item:12954\"");
			}
		}
		return keys;
	}

	private static int count(Object... values)
	{
		int count = 0;
		for (Object value : values)
		{
			count += value != null ? 1 : 0;
		}
		return count;
	}

	private static KillCountRequirement toKillCount(ActivitySpec spec)
	{
		if (spec.count == null)
		{
			throw new IllegalArgumentException("killCount needs a count");
		}
		switch (activityKind(spec))
		{
			case HISCORE:
				return KillCountRequirement.hiscore(enumOf(HiscoreSkill.class, spec.hiscore, "hiscore activity"), spec.count);
			case NPC:
				return KillCountRequirement.npc(spec.npc, spec.count);
			default:
				return KillCountRequirement.manual(spec.manual, spec.count);
		}
	}

	private static DropSource toDropSource(ActivitySpec spec)
	{
		if (spec.rate == null)
		{
			throw new IllegalArgumentException("dropSource needs a rate");
		}
		DropSource source;
		switch (activityKind(spec))
		{
			case HISCORE:
				source = DropSource.hiscore(enumOf(HiscoreSkill.class, spec.hiscore, "hiscore activity"), spec.rate);
				break;
			case NPC:
				source = DropSource.npc(spec.npc, spec.rate);
				break;
			default:
				source = DropSource.manual(spec.manual, spec.rate);
		}
		return spec.perDrop == null ? source : source.withPerDrop(spec.perDrop);
	}

	private static GoalIcon toIcon(IconSpec spec)
	{
		int set = (spec.item != null ? 1 : 0) + (spec.activity != null ? 1 : 0) + (spec.sideIcon != null ? 1 : 0);
		if (set != 1)
		{
			throw new IllegalArgumentException("An icon needs exactly one of item, activity or sideIcon");
		}
		if (spec.item != null)
		{
			return GoalIcon.item(spec.item);
		}
		if (spec.activity != null)
		{
			return GoalIcon.activity(enumOf(HiscoreSkill.class, spec.activity, "hiscore activity"));
		}
		return GoalIcon.side(enumOf(GoalIcon.SideIcon.class, spec.sideIcon, "icon"));
	}

	private static KillCountRequirement.ActivityType activityKind(ActivitySpec spec)
	{
		int set = (spec.hiscore != null ? 1 : 0) + (spec.npc != null ? 1 : 0) + (spec.manual != null ? 1 : 0);
		if (set != 1)
		{
			throw new IllegalArgumentException("An activity needs exactly one of hiscore, npc or manual");
		}
		return spec.hiscore != null ? KillCountRequirement.ActivityType.HISCORE
			: spec.npc != null ? KillCountRequirement.ActivityType.NPC
			: KillCountRequirement.ActivityType.MANUAL;
	}
}
