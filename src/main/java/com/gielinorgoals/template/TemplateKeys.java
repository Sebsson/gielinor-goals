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

import com.gielinorgoals.model.CombatAchievementRequirement;
import com.gielinorgoals.model.CombatLevelRequirement;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.SkillRequirement;
import com.gielinorgoals.model.TotalLevelRequirement;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Stabila nycklar för template-noder, t.ex. "quest:117", "skill:ATTACK:70" och "item:7462".
 * Nyckeln härleds ur kravet, så den är densamma varje gång templaten byggs och för ett
 * befintligt mål med samma krav. Noder utan krav (grupper, manuella steg) får en nyckel
 * av sin titel. Nycklarna sparas på målen och får inte ändra format utan migrering.
 */
public final class TemplateKeys
{
	private TemplateKeys()
	{
	}

	public static String of(TemplateNode node)
	{
		String key = of(node.getRequirement());
		return key != null ? key : "group:" + node.getTitle().trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * Målets nyckel: den sparade om målet skapades av en template, annars härledd ur kravet.
	 *
	 * @return nyckeln, eller null för ett manuellt mål som inte kom från en template
	 */
	@Nullable
	public static String of(Goal goal)
	{
		if (goal.getTemplateNodeKey() != null)
		{
			return goal.getTemplateNodeKey();
		}
		return of(goal.getRequirement());
	}

	@Nullable
	public static String of(@Nullable Requirement requirement)
	{
		if (requirement instanceof QuestRequirement)
		{
			return "quest:" + ((QuestRequirement) requirement).getQuestId();
		}
		if (requirement instanceof SkillRequirement)
		{
			SkillRequirement skill = (SkillRequirement) requirement;
			switch (skill.getMode())
			{
				case LEVEL:
					return "skill:" + skill.getSkill().name() + ":" + skill.getTarget();
				case XP:
					return "skillxp:" + skill.getSkill().name() + ":" + skill.getTarget();
				default:
					return "skillgain:" + skill.getSkill().name() + ":" + skill.getTarget();
			}
		}
		if (requirement instanceof ItemRequirement)
		{
			ItemRequirement item = (ItemRequirement) requirement;
			return "item:" + item.getItemId() + (item.getQuantity() == 1 ? "" : ":" + item.getQuantity());
		}
		if (requirement instanceof DiaryRequirement)
		{
			DiaryRequirement diary = (DiaryRequirement) requirement;
			return "diary:" + diary.getArea().name() + ":" + diary.getTier().name();
		}
		if (requirement instanceof CombatAchievementRequirement)
		{
			return "ca:" + ((CombatAchievementRequirement) requirement).getTaskId();
		}
		if (requirement instanceof QuestPointsRequirement)
		{
			return "qp:" + ((QuestPointsRequirement) requirement).getTarget();
		}
		if (requirement instanceof TotalLevelRequirement)
		{
			return "total:" + ((TotalLevelRequirement) requirement).getTarget();
		}
		if (requirement instanceof CombatLevelRequirement)
		{
			return "combat:" + ((CombatLevelRequirement) requirement).getTarget();
		}
		if (requirement instanceof KillCountRequirement)
		{
			KillCountRequirement kc = (KillCountRequirement) requirement;
			return "kc:" + kc.counterKey() + ":" + kc.getTarget();
		}
		return null;
	}
}
