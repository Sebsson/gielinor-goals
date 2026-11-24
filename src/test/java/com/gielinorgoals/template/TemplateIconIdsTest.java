package com.gielinorgoals.template;

import com.gielinorgoals.model.GoalIcon;
import com.google.gson.Gson;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Item-ikonerna i templaten ska vara riktiga föremål. Ett felskrivet id syns annars bara
 * som en tom ikon. Listan nedan är gameval-konstanterna för de ikoner som används; en ny
 * ikon läggs till här med sin konstant.
 */
public class TemplateIconIdsTest
{
	private static final Set<Integer> KNOWN_ICONS = Set.of(
		ItemID.BOW_OF_FAERDHINEN,
		ItemID.DRAGON_WARHAMMER,
		ItemID.XERIC_TALISMAN,
		ItemID.TZHAAR_CAPE_FIRE,
		ItemID.INFERNAL_CAPE,
		ItemID.DRAGON_PARRYINGDAGGER,
		ItemID.BARBASSAULT_PENANCE_FIGHTER_TORSO,
		ItemID.AVAS_ASSEMBLER,
		ItemID.GRACEFUL_TOP,
		ItemID.GRACE,
		ItemID.BH_RUNE_POUCH,
		ItemID.MA2_SARADOMIN_CAPE,
		ItemID.ZENYTE_AMULET_ENCHANTED,
		ItemID.ZENYTE,
		ItemID.ARDY_CAPE_ELITE,
		ItemID.DRAMEN_STAFF,
		ItemID.SKILLCAPE_QP,
		ItemID.SKILLCAPE_AD,
		ItemID.SKILLCAPE_MAX,
		ItemID.VARROCK_ARMOUR_ELITE,
		ItemID.ECTOPHIAL,
		ItemID.DRAGON_SCIMITAR,
		ItemID.PEST_VOID_KNIGHT_TOP,
		ItemID.LUNAR_SEAL_OF_PASSAGE,
		ItemID.SLAYER_HELM,
		ItemID.ELITE_VOID_KNIGHT_TOP,
		ItemID.BONECRUSHER,
		ItemID.SLAYER_HERB_SACK,
		ItemID.SEED_BOX,
		ItemID.ASH_SANCTIFIER);

	@Test
	public void everyItemIconIsAKnownGamevalItem()
	{
		Set<Integer> used = new TreeSet<>();
		for (GoalTemplate template : TestCombatAchievements.catalog(new Gson()).getTemplates())
		{
			if (template.getIcon() != null && template.getIcon().getItemId() != null)
			{
				used.add(template.getIcon().getItemId());
			}
			collect(template.build(Map.of("level", 70)), used);
		}

		Set<Integer> unknown = new TreeSet<>(used);
		unknown.removeAll(KNOWN_ICONS);
		assertTrue("item icons without a known gameval constant: " + unknown, unknown.isEmpty());
		assertTrue("icons used: " + used, used.size() >= 15);
	}

	private static void collect(TemplateNode node, Set<Integer> out)
	{
		GoalIcon icon = node.getIcon();
		if (icon != null && icon.getItemId() != null)
		{
			out.add(icon.getItemId());
		}
		node.getChildren().forEach(child -> collect(child, out));
	}
}
