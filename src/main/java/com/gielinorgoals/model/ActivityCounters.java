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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * Nuvarande värden för aktiviteter (kill count, clues, NPC-kills, egna räknare) per
 * profil, nycklade på {@link KillCountRequirement#counterKey()}. Flera mål på samma
 * aktivitet läser samma räknare. Varje värde har en källa och en tidpunkt.
 *
 * <p>Reglerna för vilken källa som vinner:
 * <ul>
 * <li>chatt: spelets egna meddelanden är absoluta och vinner alltid</li>
 * <li>hiscores: släpar efter, så de används bara om inget värde finns eller om de är högre</li>
 * <li>loot: räknar upp ett från nuvarande värde, eller från 0</li>
 * <li>manuellt: användaren sätter eller justerar värdet själv</li>
 * </ul>
 * Används bara på EDT.
 */
public class ActivityCounters
{
	public enum Source
	{
		CHAT("chat"),
		HISCORES("hiscores"),
		LOOT("loot"),
		MANUAL("manual");

		@Getter
		private final String label;

		Source(String label)
		{
			this.label = label;
		}
	}

	/**
	 * Ett sparat värde. Fälten serialiseras med Gson och får inte bytas namn.
	 */
	@Getter
	public static class Counter
	{
		private long value;
		private Source source;
		// Epoch ms när värdet senast ändrades eller bekräftades.
		private long updatedAt;

		// För Gson.
		Counter()
		{
		}

		Counter(long value, Source source, long updatedAt)
		{
			this.value = value;
			this.source = source;
			this.updatedAt = updatedAt;
		}
	}

	private final Map<String, Counter> counters;

	public ActivityCounters()
	{
		this(new LinkedHashMap<>());
	}

	public ActivityCounters(Map<String, Counter> counters)
	{
		this.counters = counters;
		// Trasiga poster från sparfilen tas bort, så att de inte ser ut som riktiga värden.
		this.counters.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null
			|| entry.getValue().source == null || entry.getValue().value < 0);
	}

	public Map<String, Counter> asMap()
	{
		return Collections.unmodifiableMap(counters);
	}

	@Nullable
	public Counter get(String key)
	{
		return counters.get(key);
	}

	/**
	 * @return värdet, eller -1 om det inte är känt
	 */
	public long value(String key)
	{
		Counter counter = counters.get(key);
		return counter == null ? -1 : counter.value;
	}

	/**
	 * Spelets eget meddelande: absolut värde som alltid vinner.
	 *
	 * @return true om något ändrades
	 */
	public boolean applyChat(String key, long value, long now)
	{
		if (value < 0)
		{
			return false;
		}
		Counter old = counters.get(key);
		counters.put(key, new Counter(value, Source.CHAT, now));
		return old == null || old.value != value || old.source != Source.CHAT;
	}

	/**
	 * Värde från hiscores. Ignoreras om det är okänt (-1, ej rankad) eller inte högre än
	 * det som redan finns, eftersom hiscores släpar efter spelet.
	 */
	public boolean applyHiscore(String key, long value, long now)
	{
		if (value < 0)
		{
			return false;
		}
		Counter old = counters.get(key);
		if (old != null && value <= old.value)
		{
			return false;
		}
		counters.put(key, new Counter(value, Source.HISCORES, now));
		return true;
	}

	/**
	 * Ett kill från loot-eventet: ett upp från nuvarande värde, eller från 0.
	 */
	public void addLoot(String key, long now)
	{
		long current = Math.max(0, value(key));
		counters.put(key, new Counter(current + 1, Source.LOOT, now));
	}

	/**
	 * Användaren anger värdet själv, t.ex. för att rätta ett fel eller ange ett startvärde.
	 */
	public boolean setManual(String key, long value, long now)
	{
		long clamped = Math.max(0, value);
		Counter old = counters.get(key);
		counters.put(key, new Counter(clamped, Source.MANUAL, now));
		return old == null || old.value != clamped;
	}

	/**
	 * +1/−1 från användaren. Går aldrig under 0.
	 */
	public boolean adjustManual(String key, long delta, long now)
	{
		return setManual(key, Math.max(0, value(key)) + delta, now);
	}
}
