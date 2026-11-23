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

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Håller reda på vilka mål som var klara vid förra uppdateringen, så att kartan kan låta
 * just klarade mål pulsera. Vid första uppdateringen och efter ett profilbyte finns inget
 * att jämföra med, så då pulserar ingenting; annars skulle alla klara mål i det nya
 * kontot se nyss klarade ut. Används bara på EDT.
 */
final class CompletionWatcher
{
	@Nullable
	private Set<String> before;
	@Nullable
	private String profileKey;

	/**
	 * @return mål som är klara nu men inte var det vid förra anropet för samma profil
	 */
	Set<String> update(@Nullable String profileKey, Set<String> completedNow)
	{
		boolean comparable = before != null && Objects.equals(profileKey, this.profileKey);
		Set<String> fresh = new HashSet<>();
		if (comparable)
		{
			for (String id : completedNow)
			{
				if (!before.contains(id))
				{
					fresh.add(id);
				}
			}
		}
		before = new HashSet<>(completedNow);
		this.profileKey = profileKey;
		return comparable ? fresh : Collections.emptySet();
	}

	/**
	 * Glöm förra läget, t.ex. när fönstret öppnas efter att ha varit stängt.
	 */
	void reset()
	{
		before = null;
	}
}
