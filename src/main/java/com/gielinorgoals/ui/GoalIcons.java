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
package com.gielinorgoals.ui;

import com.gielinorgoals.model.CombatAchievementRequirement;
import com.gielinorgoals.model.CombatLevelRequirement;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalIcon;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.PointsRequirement;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.SkillRequirement;
import com.gielinorgoals.model.TotalLevelRequirement;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

/**
 * Små ikoner per måltyp: skill-ikon, föremålsbild eller spelets egna sidoflikar för
 * quests, diaries och combat. Sprites laddas asynkront och cachas.
 */
@Singleton
public class GoalIcons
{
	static final int SIZE = 16;

	private final ItemManager itemManager;
	private final SkillIconManager skillIconManager;
	private final SpriteManager spriteManager;
	private final Map<Integer, BufferedImage> sprites = new ConcurrentHashMap<>();

	@Inject
	GoalIcons(ItemManager itemManager, SkillIconManager skillIconManager, SpriteManager spriteManager)
	{
		this.itemManager = itemManager;
		this.skillIconManager = skillIconManager;
		this.spriteManager = spriteManager;
	}

	/**
	 * Kravets ikon, eller målets egen ikon om det saknar krav (t.ex. grupper från templates).
	 */
	public void apply(Goal goal, JLabel label)
	{
		if (goal.getRequirement() != null)
		{
			apply(goal.getRequirement(), label);
		}
		else
		{
			apply(goal.getIcon(), label);
		}
	}

	public static boolean hasIcon(Goal goal)
	{
		return goal.getRequirement() != null || (goal.getIcon() != null && goal.getIcon().isValid());
	}

	public void apply(@Nullable GoalIcon icon, JLabel label)
	{
		if (icon == null || !icon.isValid())
		{
			label.setIcon(null);
		}
		else if (icon.getItemId() != null)
		{
			applyItem(icon.getItemId(), label);
		}
		else
		{
			applySprite(spriteFor(icon), label);
		}
	}

	/**
	 * Som {@link #loadImage(Requirement, Consumer)} men för en egen ikon.
	 */
	public void loadImage(@Nullable GoalIcon icon, Consumer<BufferedImage> consumer)
	{
		if (icon == null || !icon.isValid())
		{
			return;
		}
		if (icon.getItemId() != null)
		{
			AsyncBufferedImage image = itemManager.getImage(icon.getItemId());
			image.onLoaded(() -> consumer.accept(image));
		}
		else
		{
			loadSprite(spriteFor(icon), consumer);
		}
	}

	private static int spriteFor(GoalIcon icon)
	{
		if (icon.hiscoreSkill() != null)
		{
			return KillCountIcons.spriteFor(icon.hiscoreSkill());
		}
		switch (icon.getSideIcon())
		{
			case QUEST:
				return SpriteID.SideIcons.QUEST;
			case STATS:
				return SpriteID.SideIcons.STATS;
			case INVENTORY:
				return SpriteID.SideIcons.INVENTORY;
			case EQUIPMENT:
				return SpriteID.SideIcons.EQUIPMENT;
			case ACHIEVEMENT_DIARIES:
				return SpriteID.SideIcons.ACHIEVEMENT_DIARIES;
			case MINIGAMES:
				return SpriteID.SideIcons.MINIGAMES;
			default:
				return SpriteID.SideIcons.COMBAT;
		}
	}

	/**
	 * Sätter ikonen för kravet på etiketten. Manuella mål får ingen ikon.
	 */
	public void apply(@Nullable Requirement requirement, JLabel label)
	{
		if (requirement instanceof SkillRequirement)
		{
			label.setIcon(new ImageIcon(skillIconManager.getSkillImage(((SkillRequirement) requirement).getSkill(), true)));
		}
		else if (requirement instanceof ItemRequirement)
		{
			applyItem(((ItemRequirement) requirement).getItemId(), label);
		}
		else if (requirement instanceof QuestRequirement)
		{
			applySprite(SpriteID.SideIcons.QUEST, label);
		}
		else if (requirement instanceof DiaryRequirement)
		{
			applySprite(SpriteID.SideIcons.ACHIEVEMENT_DIARIES, label);
		}
		else if (requirement instanceof CombatAchievementRequirement || requirement instanceof CombatLevelRequirement)
		{
			applySprite(SpriteID.SideIcons.COMBAT, label);
		}
		else if (requirement instanceof TotalLevelRequirement)
		{
			applySprite(SpriteID.SideIcons.STATS, label);
		}
		else if (requirement instanceof QuestPointsRequirement)
		{
			applySprite(SpriteID.SideIcons.QUEST, label);
		}
		else if (requirement instanceof KillCountRequirement)
		{
			applySprite(KillCountIcons.spriteFor((KillCountRequirement) requirement), label);
		}
		else if (requirement instanceof PointsRequirement)
		{
			net.runelite.api.Skill skill = ((PointsRequirement) requirement).getActivity().getIconSkill();
			if (skill != null)
			{
				label.setIcon(new ImageIcon(skillIconManager.getSkillImage(skill, true)));
			}
			else
			{
				applySprite(SpriteID.SideIcons.MINIGAMES, label);
			}
		}
		else
		{
			label.setIcon(null);
		}
	}

	/**
	 * Hämtar ikonen som bild, t.ex. för overlays. {@code consumer} kan anropas direkt
	 * eller senare från en annan tråd när bilden har laddats. Manuella mål ger ingen bild.
	 */
	public void loadImage(@Nullable Requirement requirement, Consumer<BufferedImage> consumer)
	{
		if (requirement instanceof SkillRequirement)
		{
			consumer.accept(skillIconManager.getSkillImage(((SkillRequirement) requirement).getSkill(), true));
		}
		else if (requirement instanceof ItemRequirement)
		{
			AsyncBufferedImage image = itemManager.getImage(((ItemRequirement) requirement).getItemId());
			image.onLoaded(() -> consumer.accept(image));
		}
		else if (requirement instanceof QuestRequirement)
		{
			loadSprite(SpriteID.SideIcons.QUEST, consumer);
		}
		else if (requirement instanceof DiaryRequirement)
		{
			loadSprite(SpriteID.SideIcons.ACHIEVEMENT_DIARIES, consumer);
		}
		else if (requirement instanceof CombatAchievementRequirement || requirement instanceof CombatLevelRequirement)
		{
			loadSprite(SpriteID.SideIcons.COMBAT, consumer);
		}
		else if (requirement instanceof TotalLevelRequirement)
		{
			loadSprite(SpriteID.SideIcons.STATS, consumer);
		}
		else if (requirement instanceof QuestPointsRequirement)
		{
			loadSprite(SpriteID.SideIcons.QUEST, consumer);
		}
		else if (requirement instanceof KillCountRequirement)
		{
			loadSprite(KillCountIcons.spriteFor((KillCountRequirement) requirement), consumer);
		}
		else if (requirement instanceof PointsRequirement)
		{
			net.runelite.api.Skill skill = ((PointsRequirement) requirement).getActivity().getIconSkill();
			if (skill != null)
			{
				consumer.accept(skillIconManager.getSkillImage(skill, true));
			}
			else
			{
				loadSprite(SpriteID.SideIcons.MINIGAMES, consumer);
			}
		}
	}

	/**
	 * Ikonen för en hiscore-aktivitet, t.ex. i sökningen i redigeringsdialogen. Kan
	 * anropas senare från en annan tråd när bilden har laddats.
	 */
	public void loadActivityImage(net.runelite.client.hiscore.HiscoreSkill skill, Consumer<BufferedImage> consumer)
	{
		loadSprite(KillCountIcons.spriteFor(skill), consumer);
	}

	public void applyItem(int itemId, JLabel label)
	{
		itemManager.getImage(itemId).addTo(label);
	}

	private void applySprite(int spriteId, JLabel label)
	{
		BufferedImage cached = sprites.get(spriteId);
		if (cached != null)
		{
			label.setIcon(new ImageIcon(cached));
			return;
		}

		loadSprite(spriteId, resized -> SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(resized))));
	}

	private void loadSprite(int spriteId, Consumer<BufferedImage> consumer)
	{
		BufferedImage cached = sprites.get(spriteId);
		if (cached != null)
		{
			consumer.accept(cached);
			return;
		}

		spriteManager.getSpriteAsync(spriteId, 0, image ->
		{
			if (image == null)
			{
				return;
			}
			BufferedImage resized = ImageUtil.resizeImage(image, SIZE, SIZE);
			sprites.put(spriteId, resized);
			consumer.accept(resized);
		});
	}
}
