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
 */
public class JsonTemplate implements GoalTemplate
{
	// Fälten fylls av Gson.
	static final class FileSpec
	{
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
		String quest;
		String skill;
		Integer level;
		Integer questPoints;
		Integer totalLevel;
		Integer combatLevel;
		DiarySpec diary;
		ItemSpec item;
		List<NodeSpec> children;
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

	// Handplockade templates visar hela quest-kedjorna som standard.
	@Override
	public boolean defaultExpandQuestChains()
	{
		return true;
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		return toNode(root);
	}

	private static TemplateNode toNode(NodeSpec spec)
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
				node.add(toNode(child));
			}
		}
		return node;
	}

	@Nullable
	private static Requirement toRequirement(NodeSpec spec)
	{
		if (spec.quest != null)
		{
			Quest quest = Quest.valueOf(spec.quest);
			return new QuestRequirement(quest.getId(), quest.getName());
		}
		if (spec.skill != null)
		{
			if (spec.level == null)
			{
				throw new IllegalArgumentException("Skill " + spec.skill + " needs a level");
			}
			return SkillRequirement.level(Skill.valueOf(spec.skill), spec.level);
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
			return new DiaryRequirement(DiaryArea.valueOf(spec.diary.area), DiaryTier.valueOf(spec.diary.tier));
		}
		if (spec.item != null)
		{
			if (spec.item.id == null || spec.item.name == null)
			{
				throw new IllegalArgumentException("Item needs an id and a name");
			}
			return new ItemRequirement(spec.item.id, spec.item.name,
				spec.item.quantity == null ? 1 : spec.item.quantity,
				Boolean.TRUE.equals(spec.item.includeVariants) || Boolean.TRUE.equals(spec.item.variants));
		}
		if (spec.killCount != null)
		{
			return toKillCount(spec.killCount);
		}
		return null;
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
				return KillCountRequirement.hiscore(HiscoreSkill.valueOf(spec.hiscore), spec.count);
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
				source = DropSource.hiscore(HiscoreSkill.valueOf(spec.hiscore), spec.rate);
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
			return GoalIcon.activity(HiscoreSkill.valueOf(spec.activity));
		}
		return GoalIcon.side(GoalIcon.SideIcon.valueOf(spec.sideIcon));
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
