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

import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nullable;
import javax.inject.Singleton;

/**
 * Kö för popup-notiser som visas en i taget. Fylls på från EDT och läses av overlayen
 * på klienttråden, därför synkroniserad. Tiden styrs helt av tidsstämplarna som
 * skickas in, så det finns inga trådar eller timers.
 * Blir kön för lång slås resten ihop till en enda "+N more"-notis.
 */
@Singleton
public class NotificationQueue
{
	static final int MAX_PENDING = 5;
	static final long FADE_IN_MS = 300;
	static final long FADE_OUT_MS = 600;

	private final Deque<PopupNotice> pending = new ArrayDeque<>();
	@Nullable
	private PopupNotice current;

	// Notiser som inte fick plats i kön och visas som en sammanslagen notis på slutet.
	private int overflowCount;
	private int overflowSound = PopupNotice.NO_SOUND;
	private boolean overflowRoot;
	private long overflowHoldMs;

	public synchronized void offer(PopupNotice notice)
	{
		if (pending.size() < MAX_PENDING)
		{
			pending.add(notice);
			return;
		}

		overflowCount++;
		overflowHoldMs = notice.getHoldMs();
		// Ett rotmål i överflödet ger rotmålets ljud, annars det första ljudet som fanns.
		if (notice.getSoundId() != PopupNotice.NO_SOUND
			&& (overflowSound == PopupNotice.NO_SOUND || (notice.isRoot() && !overflowRoot)))
		{
			overflowSound = notice.getSoundId();
		}
		overflowRoot |= notice.isRoot();
	}

	public synchronized void clear()
	{
		pending.clear();
		current = null;
		overflowCount = 0;
		overflowSound = PopupNotice.NO_SOUND;
		overflowRoot = false;
	}

	/**
	 * Notisen som ska visas just nu. Byter till nästa i kön när den förra har tonat ut.
	 *
	 * @return notisen, eller null om inget ska visas
	 */
	@Nullable
	public synchronized PopupNotice current(long now)
	{
		return current(now, false);
	}

	/**
	 * @param hovered om muspekaren är över notisen; då stannar den kvar och tonas inte ut
	 */
	@Nullable
	public synchronized PopupNotice current(long now, boolean hovered)
	{
		if (current != null && hovered)
		{
			current.hold(now);
		}
		if (current != null && now - current.getShownAt() >= totalMs(current))
		{
			current = null;
		}

		if (current == null)
		{
			current = pending.poll();
			if (current == null && overflowCount > 0)
			{
				current = mergedNotice();
			}
			if (current != null)
			{
				current.markShown(now);
			}
		}
		return current;
	}

	synchronized int size()
	{
		return pending.size() + (overflowCount > 0 ? 1 : 0) + (current != null ? 1 : 0);
	}

	private PopupNotice mergedNotice()
	{
		PopupNotice merged = new PopupNotice("Goals completed!",
			"+" + overflowCount + (overflowCount == 1 ? " more goal completed" : " more goals completed"),
			null, false, overflowRoot, overflowSound, overflowHoldMs);
		overflowCount = 0;
		overflowSound = PopupNotice.NO_SOUND;
		overflowRoot = false;
		return merged;
	}

	private static long totalMs(PopupNotice notice)
	{
		return FADE_IN_MS + notice.getHoldMs() + FADE_OUT_MS;
	}

	/**
	 * Genomskinlighet för notisen: tonar in, visas helt, tonar ut.
	 */
	public static float alpha(PopupNotice notice, long now)
	{
		long t = now - notice.getShownAt();
		if (t < 0)
		{
			return 0f;
		}
		if (t < FADE_IN_MS)
		{
			return (float) t / FADE_IN_MS;
		}
		long fadeOutStart = FADE_IN_MS + notice.getHoldMs();
		if (t < fadeOutStart)
		{
			return 1f;
		}
		return Math.max(0f, 1f - (float) (t - fadeOutStart) / FADE_OUT_MS);
	}
}
