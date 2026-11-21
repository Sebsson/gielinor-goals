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
package com.gielinorgoals.persistence;

import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import java.util.List;
import lombok.Value;

/**
 * Resultatet av att läsa sparade mål.
 */
@Value
public class LoadResult
{
	public enum Status
	{
		// Inget sparat ännu eller lyckad läsning.
		OK,
		// Sparat av en nyare version av pluginet; listan får bara läsas.
		NEWER_VERSION,
		// Datan gick inte att tolka; listan startar tom och rådatan sparas undan.
		CORRUPT
	}

	Status status;
	GoalTree tree;
	boolean migrated;

	/**
	 * Rotmålen, för enkel åtkomst i tester och kod som bara bryr sig om översta nivån.
	 */
	public List<Goal> getGoals()
	{
		return tree.getRoots();
	}

	public boolean isReadOnly()
	{
		return status == Status.NEWER_VERSION;
	}
}
