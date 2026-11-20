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
package com.gielinorgoals.model;

import java.util.Locale;
import javax.annotation.Nullable;
import lombok.Getter;
import net.runelite.api.gameval.VarbitID;

/**
 * Achievement diary-områden med varbits per nivå. Allt är verifierat mot spelets clientscripts:
 * - klarmarkering: [proc,area_task_complete]. Karamjas easy/medium/hard är ett äldre system
 *   där värdet 2 betyder klar; övriga nivåer använder 1.
 * - antal klara uppgifter och totalt antal per nivå: [proc,diary_completion_info].
 */
public enum DiaryArea
{
	ARDOUGNE("Ardougne", 1,
		new int[]{VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE,
			VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE},
		new int[]{VarbitID.ARDOUGNE_EASY_COUNT, VarbitID.ARDOUGNE_MED_COUNT,
			VarbitID.ARDOUGNE_HARD_COUNT, VarbitID.ARDOUGNE_ELITE_COUNT},
		new int[]{10, 12, 12, 8}),
	DESERT("Desert", 1,
		new int[]{VarbitID.DESERT_DIARY_EASY_COMPLETE, VarbitID.DESERT_DIARY_MEDIUM_COMPLETE,
			VarbitID.DESERT_DIARY_HARD_COMPLETE, VarbitID.DESERT_DIARY_ELITE_COMPLETE},
		new int[]{VarbitID.DESERT_EASY_COUNT, VarbitID.DESERT_MED_COUNT,
			VarbitID.DESERT_HARD_COUNT, VarbitID.DESERT_ELITE_COUNT},
		new int[]{11, 12, 10, 6}),
	FALADOR("Falador", 1,
		new int[]{VarbitID.FALADOR_DIARY_EASY_COMPLETE, VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE,
			VarbitID.FALADOR_DIARY_HARD_COMPLETE, VarbitID.FALADOR_DIARY_ELITE_COMPLETE},
		new int[]{VarbitID.FALADOR_EASY_COUNT, VarbitID.FALADOR_MED_COUNT,
			VarbitID.FALADOR_HARD_COUNT, VarbitID.FALADOR_ELITE_COUNT},
		new int[]{11, 14, 11, 6}),
	FREMENNIK("Fremennik", 1,
		new int[]{VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE,
			VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE},
		new int[]{VarbitID.FREMENNIK_EASY_COUNT, VarbitID.FREMENNIK_MED_COUNT,
			VarbitID.FREMENNIK_HARD_COUNT, VarbitID.FREMENNIK_ELITE_COUNT},
		new int[]{10, 9, 9, 6}),
	KANDARIN("Kandarin", 1,
		new int[]{VarbitID.KANDARIN_DIARY_EASY_COMPLETE, VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE,
			VarbitID.KANDARIN_DIARY_HARD_COMPLETE, VarbitID.KANDARIN_DIARY_ELITE_COMPLETE},
		new int[]{VarbitID.KANDARIN_EASY_COUNT, VarbitID.KANDARIN_MED_COUNT,
			VarbitID.KANDARIN_HARD_COUNT, VarbitID.KANDARIN_ELITE_COUNT},
		new int[]{11, 14, 11, 7}),
	KARAMJA("Karamja", 2,
		new int[]{VarbitID.ATJUN_EASY_DONE, VarbitID.ATJUN_MED_DONE,
			VarbitID.ATJUN_HARD_DONE, VarbitID.KARAMJA_DIARY_ELITE_COMPLETE},
		new int[]{VarbitID.KARAMJA_EASY_COUNT, VarbitID.KARAMJA_MED_COUNT,
			VarbitID.KARAMJA_HARD_COUNT, VarbitID.KARAMJA_ELITE_COUNT},
		new int[]{10, 19, 10, 5}),
	KOUREND("Kourend & Kebos", 1,
		new int[]{VarbitID.KOUREND_DIARY_EASY_COMPLETE, VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE,
			VarbitID.KOUREND_DIARY_HARD_COMPLETE, VarbitID.KOUREND_DIARY_ELITE_COMPLETE},
		new int[]{VarbitID.KOUREND_EASY_COUNT, VarbitID.KOUREND_MED_COUNT,
			VarbitID.KOUREND_HARD_COUNT, VarbitID.KOUREND_ELITE_COUNT},
		new int[]{12, 13, 10, 8}),
	LUMBRIDGE("Lumbridge & Draynor", 1,
		new int[]{VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE,
			VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE},
		new int[]{VarbitID.LUMBRIDGE_EASY_COUNT, VarbitID.LUMBRIDGE_MED_COUNT,
			VarbitID.LUMBRIDGE_HARD_COUNT, VarbitID.LUMBRIDGE_ELITE_COUNT},
		new int[]{12, 12, 11, 6}),
	MORYTANIA("Morytania", 1,
		new int[]{VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE,
			VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE},
		new int[]{VarbitID.MORYTANIA_EASY_COUNT, VarbitID.MORYTANIA_MED_COUNT,
			VarbitID.MORYTANIA_HARD_COUNT, VarbitID.MORYTANIA_ELITE_COUNT},
		new int[]{11, 11, 10, 6}),
	VARROCK("Varrock", 1,
		new int[]{VarbitID.VARROCK_DIARY_EASY_COMPLETE, VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE,
			VarbitID.VARROCK_DIARY_HARD_COMPLETE, VarbitID.VARROCK_DIARY_ELITE_COMPLETE},
		new int[]{VarbitID.VARROCK_EASY_COUNT, VarbitID.VARROCK_MED_COUNT,
			VarbitID.VARROCK_HARD_COUNT, VarbitID.VARROCK_ELITE_COUNT},
		new int[]{14, 13, 10, 5}),
	WESTERN("Western Provinces", 1,
		new int[]{VarbitID.WESTERN_DIARY_EASY_COMPLETE, VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE,
			VarbitID.WESTERN_DIARY_HARD_COMPLETE, VarbitID.WESTERN_DIARY_ELITE_COMPLETE},
		new int[]{VarbitID.WESTERN_EASY_COUNT, VarbitID.WESTERN_MED_COUNT,
			VarbitID.WESTERN_HARD_COUNT, VarbitID.WESTERN_ELITE_COUNT},
		new int[]{11, 13, 13, 7}),
	WILDERNESS("Wilderness", 1,
		new int[]{VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE,
			VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE},
		new int[]{VarbitID.WILDERNESS_EASY_COUNT, VarbitID.WILDERNESS_MED_COUNT,
			VarbitID.WILDERNESS_HARD_COUNT, VarbitID.WILDERNESS_ELITE_COUNT},
		new int[]{12, 11, 10, 7});

	@Getter
	private final String displayName;
	// Värde som betyder klar för easy/medium/hard; elite är alltid 1.
	private final int lowTierCompleteValue;
	private final int[] completeVarbits;
	private final int[] countVarbits;
	private final int[] taskTotals;

	DiaryArea(String displayName, int lowTierCompleteValue, int[] completeVarbits, int[] countVarbits,
		int[] taskTotals)
	{
		this.displayName = displayName;
		this.lowTierCompleteValue = lowTierCompleteValue;
		this.completeVarbits = completeVarbits;
		this.countVarbits = countVarbits;
		this.taskTotals = taskTotals;
	}

	public int varbitFor(DiaryTier tier)
	{
		return completeVarbits[tier.ordinal()];
	}

	public int completeValueFor(DiaryTier tier)
	{
		return tier == DiaryTier.ELITE ? 1 : lowTierCompleteValue;
	}

	/**
	 * Varbit med antal klara uppgifter i nivån.
	 */
	public int countVarbitFor(DiaryTier tier)
	{
		return countVarbits[tier.ordinal()];
	}

	/**
	 * Antal uppgifter i nivån enligt spelets clientscript.
	 */
	public int taskTotalFor(DiaryTier tier)
	{
		return taskTotals[tier.ordinal()];
	}

	/**
	 * Hittar området utifrån journalens rubrik, t.ex. "Varrock Area Tasks" eller
	 * "Kourend &amp; Kebos Tasks".
	 */
	@Nullable
	public static DiaryArea fromJournalTitle(String title)
	{
		String upper = title.toUpperCase(Locale.ROOT);
		for (DiaryArea area : values())
		{
			// Första ordet räcker och är unikt: ARDOUGNE, DESERT, ..., KOUREND, LUMBRIDGE, WESTERN.
			String keyword = area.displayName.split(" ")[0].toUpperCase(Locale.ROOT);
			if (upper.contains(keyword))
			{
				return area;
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
