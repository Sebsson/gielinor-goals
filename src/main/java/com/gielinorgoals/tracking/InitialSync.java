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
package com.gielinorgoals.tracking;

import java.util.function.LongSupplier;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Tidsfönstret för första synken efter inloggning, världsbyte, profilbyte eller att
 * pluginet slås på. Mål som klaras inom fönstret klarades troligen offline: de får
 * ingen popup och deras datum markeras som ungefärligt.
 * Första synken kommer i flera steg (skills, föremål, quests, diaries) under de
 * första sekunderna, så fönstret gäller en stund och inte bara en batch.
 */
@Singleton
public class InitialSync
{
	public static final long WINDOW_MS = 10_000;

	private final LongSupplier clock;

	// Sätts från både klienttråden och EDT.
	private volatile long activeUntil;

	@Inject
	InitialSync()
	{
		this(System::currentTimeMillis);
	}

	public InitialSync(LongSupplier clock)
	{
		this.clock = clock;
	}

	public void start()
	{
		activeUntil = clock.getAsLong() + WINDOW_MS;
	}

	public boolean isActive()
	{
		return clock.getAsLong() < activeUntil;
	}
}
