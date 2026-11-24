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

import com.gielinorgoals.model.DropSource;
import com.gielinorgoals.model.GoalIcon;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.QuestRequirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.hiscore.HiscoreSkill;

/**
 * En Barrows-brors fyra delar, med brodern som valparameter. JSON-formatet har inga
 * valparametrar, så templaten är genererad i stället för sex nästan likadana filer.
 * <p>
 * Källa: https://oldschool.runescape.wiki/w/Chest_(Barrows) (ändrad 8 nov 2026, hämtad 2026-11-24): med alla sex bröder
 * dödade är chansen för en viss del ungefär 1/350 per kista (7 × 1/2,448); färre bröder ger lägre chans (1/(450 − 58N)
 * för någon del per drag). https://oldschool.runescape.wiki/w/Barrows - Priest in Peril för Morytania, påbörjad
 * Nature Spirit för Mort Myre och påbörjad His Faithful Servants för att gräva i kryptorna. Delarnas namn är wikins;
 * item-id är gameval-konstanterna (wikins sidor visar inga id).
 */
public class BarrowsTemplate implements GoalTemplate
{
	// Chans för en viss del per kista med alla sex bröder, enligt wikin.
	static final int RATE_PER_PIECE = 350;

	private static final class Brother
	{
		final String key;
		final String name;
		final int[] items;
		final String[] names;

		Brother(String key, String name, int[] items, String[] names)
		{
			this.key = key;
			this.name = name;
			this.items = items;
			this.names = names;
		}
	}

	private static final List<Brother> BROTHERS = List.of(
		new Brother("AHRIM", "Ahrim",
			new int[]{ItemID.BARROWS_AHRIM_HEAD, ItemID.BARROWS_AHRIM_BODY, ItemID.BARROWS_AHRIM_LEGS, ItemID.BARROWS_AHRIM_WEAPON},
			new String[]{"Ahrim's hood", "Ahrim's robetop", "Ahrim's robeskirt", "Ahrim's staff"}),
		new Brother("DHAROK", "Dharok",
			new int[]{ItemID.BARROWS_DHAROK_HEAD, ItemID.BARROWS_DHAROK_BODY, ItemID.BARROWS_DHAROK_LEGS, ItemID.BARROWS_DHAROK_WEAPON},
			new String[]{"Dharok's helm", "Dharok's platebody", "Dharok's platelegs", "Dharok's greataxe"}),
		new Brother("GUTHAN", "Guthan",
			new int[]{ItemID.BARROWS_GUTHAN_HEAD, ItemID.BARROWS_GUTHAN_BODY, ItemID.BARROWS_GUTHAN_LEGS, ItemID.BARROWS_GUTHAN_WEAPON},
			new String[]{"Guthan's helm", "Guthan's platebody", "Guthan's chainskirt", "Guthan's warspear"}),
		new Brother("KARIL", "Karil",
			new int[]{ItemID.BARROWS_KARIL_HEAD, ItemID.BARROWS_KARIL_BODY, ItemID.BARROWS_KARIL_LEGS, ItemID.BARROWS_KARIL_WEAPON},
			new String[]{"Karil's coif", "Karil's leathertop", "Karil's leatherskirt", "Karil's crossbow"}),
		new Brother("TORAG", "Torag",
			new int[]{ItemID.BARROWS_TORAG_HEAD, ItemID.BARROWS_TORAG_BODY, ItemID.BARROWS_TORAG_LEGS, ItemID.BARROWS_TORAG_WEAPON},
			new String[]{"Torag's helm", "Torag's platebody", "Torag's platelegs", "Torag's hammers"}),
		new Brother("VERAC", "Verac",
			new int[]{ItemID.BARROWS_VERAC_HEAD, ItemID.BARROWS_VERAC_BODY, ItemID.BARROWS_VERAC_LEGS, ItemID.BARROWS_VERAC_WEAPON},
			new String[]{"Verac's helm", "Verac's brassard", "Verac's plateskirt", "Verac's flail"}));

	static final TemplateParameter BROTHER = TemplateParameter.choice("brother", "Brother", options(), "AHRIM");

	private static List<TemplateParameter.Option> options()
	{
		List<TemplateParameter.Option> options = new ArrayList<>();
		for (Brother brother : BROTHERS)
		{
			options.add(new TemplateParameter.Option(brother.key, brother.name));
		}
		return options;
	}

	@Override
	public String getId()
	{
		return "barrows-set";
	}

	@Override
	public String getName()
	{
		return "Barrows set";
	}

	@Override
	public String getDescription()
	{
		return "All four pieces of one Barrows brother's equipment";
	}

	@Override
	public String getCategory()
	{
		return TemplateCategories.IRONMAN_MID;
	}

	@Override
	public List<String> getTags()
	{
		return List.of("barrows", "barrows set", "barrows gear", "ahrim", "ahrims", "dharok", "dharoks", "guthan",
			"guthans", "karil", "karils", "torag", "torags", "verac", "veracs", "ironman");
	}

	@Override
	public GoalIcon getIcon()
	{
		return GoalIcon.item(ItemID.BARROWS_DHAROK_HEAD);
	}

	@Override
	public List<TemplateParameter> getParameters()
	{
		return List.of(BROTHER);
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters)
	{
		return build(parameters, Map.of());
	}

	@Override
	public TemplateNode build(Map<String, Integer> parameters, Map<String, String> choices)
	{
		String key = BROTHER.choiceIn(choices);
		Brother brother = BROTHERS.stream().filter(b -> b.key.equals(key)).findFirst().orElse(BROTHERS.get(0));

		TemplateNode root = TemplateNode.aggregate(brother.name + "'s equipment");
		root.setIcon(GoalIcon.item(brother.items[0]));
		root.setWikiPage("Barrows equipment");

		TemplateNode unlock = TemplateNode.aggregate("Unlock");
		unlock.setIcon(GoalIcon.side(GoalIcon.SideIcon.QUEST));
		unlock.add(TemplateNode.requirement(quest(Quest.PRIEST_IN_PERIL)));
		unlock.add(partial("Start Nature Spirit to cross Mort Myre Swamp", Quest.NATURE_SPIRIT));
		unlock.add(partial("Start His Faithful Servants to dig into the crypts", Quest.HIS_FAITHFUL_SERVANTS));
		root.add(unlock);

		TemplateNode obtain = TemplateNode.aggregate("Obtain");
		obtain.setIcon(GoalIcon.side(GoalIcon.SideIcon.INVENTORY));
		for (int i = 0; i < brother.items.length; i++)
		{
			TemplateNode piece = TemplateNode.requirement(new ItemRequirement(brother.items[i], brother.names[i], 1, true));
			piece.setDropSource(DropSource.hiscore(HiscoreSkill.BARROWS_CHESTS, RATE_PER_PIECE));
			if (i == 0)
			{
				piece.setNote("About 1/350 per chest for each piece with all six brothers killed; fewer brothers give a "
					+ "lower chance, and only brothers you killed can drop their items.");
			}
			obtain.add(piece);
		}
		root.add(obtain);
		return root;
	}

	private static QuestRequirement quest(Quest quest)
	{
		return new QuestRequirement(quest.getId(), quest.getName());
	}

	// Ett quest som bara behöver vara påbörjat: ett manuellt mål som blir klart med hela questen.
	private static TemplateNode partial(String title, Quest quest)
	{
		TemplateNode node = new TemplateNode(title, null, false);
		node.setIcon(GoalIcon.side(GoalIcon.SideIcon.QUEST));
		node.setWikiPage(quest.getName());
		node.setDoneWhen(List.of("quest:" + quest.getId()));
		return node;
	}
}
