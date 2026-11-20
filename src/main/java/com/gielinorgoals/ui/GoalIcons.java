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
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.SkillRequirement;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
		else if (requirement instanceof CombatAchievementRequirement)
		{
			applySprite(SpriteID.SideIcons.COMBAT, label);
		}
		else
		{
			label.setIcon(null);
		}
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

		spriteManager.getSpriteAsync(spriteId, 0, image ->
		{
			if (image == null)
			{
				return;
			}
			BufferedImage resized = ImageUtil.resizeImage(image, SIZE, SIZE);
			sprites.put(spriteId, resized);
			SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(resized)));
		});
	}
}
