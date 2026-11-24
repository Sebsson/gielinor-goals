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

import com.gielinorgoals.model.GoalTree;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Var ett draget mål hamnar bland sina syskon. Panelen ser bara de synliga syskonen (med
 * "Hide completed" kan några vara dolda), så platsen räknas relativt grannarnas id och inte
 * efter radindex: "före syskon X", eller "efter syskon X" som blir "före syskonet efter X i
 * den fullständiga listan". Dolda syskon ligger då kvar på samma sida om sina grannar.
 * <p>
 * Lucka {@code k} (0 till antalet synliga syskon) ligger före det synliga syskonet {@code k};
 * den sista luckan ligger efter det sista synliga syskonet.
 */
final class DropPlacement
{
	private DropPlacement()
	{
	}

	/**
	 * Luckorna direkt före och efter målet självt flyttar det ingenstans.
	 */
	static boolean changesNothing(List<String> visibleSiblings, String movingId, int gap)
	{
		int own = visibleSiblings.indexOf(movingId);
		return own < 0 || gap == own || gap == own + 1;
	}

	/**
	 * Syskonet målet ska läggas före, eller null för sist under föräldern.
	 *
	 * @param parentId föräldern raden visas under, null för rotnivå
	 */
	@Nullable
	static String beforeId(GoalTree tree, @Nullable String parentId, String movingId, List<String> visibleSiblings,
		int gap)
	{
		if (gap < visibleSiblings.size())
		{
			return visibleSiblings.get(gap);
		}
		String last = visibleSiblings.get(visibleSiblings.size() - 1);
		return tree.siblingAfter(parentId, last, movingId);
	}
}
