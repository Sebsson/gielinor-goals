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

import java.awt.image.BufferedImage;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * En färdig notis: all text är förberedd när den skapas, så att overlayens render()
 * bara behöver rita. Ikonen kan laddas asynkront och sätts då i efterhand.
 */
@Getter
public class PopupNotice
{
	public static final int NO_SOUND = -1;

	private final String title;
	private final String name;
	// Extra rad, t.ex. progress i föräldern; null om den inte ska visas.
	@Nullable
	private final String detail;
	// Om notisen har en ikon (som kanske inte har laddats än), så att platsen reserveras.
	private final boolean hasIcon;
	private final boolean root;
	private final int soundId;
	private final long holdMs;

	@Nullable
	private volatile BufferedImage icon;

	// Sätts av kön när notisen börjar visas; bara läst/skriven under köns lås eller på klienttråden.
	private long shownAt = -1;
	private boolean soundPending;

	// Storlek räknas ut av overlayen vid första framen och sparas här.
	int width = -1;
	int height;

	public PopupNotice(String title, String name, @Nullable String detail, boolean hasIcon, boolean root,
		int soundId, long holdMs)
	{
		this.title = title;
		this.name = name;
		this.detail = detail;
		this.hasIcon = hasIcon;
		this.root = root;
		this.soundId = soundId;
		this.holdMs = holdMs;
	}

	public void setIcon(@Nullable BufferedImage icon)
	{
		this.icon = icon;
	}

	/**
	 * Håller notisen helt synlig: flyttar starttiden så att den ligger kvar i början av
	 * visningstiden, efter intoningen. Används medan muspekaren är över den.
	 */
	void hold(long now)
	{
		if (now - shownAt > NotificationQueue.FADE_IN_MS)
		{
			shownAt = now - NotificationQueue.FADE_IN_MS;
		}
	}

	void markShown(long now)
	{
		shownAt = now;
		soundPending = soundId != NO_SOUND;
	}

	/**
	 * @return true första gången efter att notisen börjat visas, om den har ett ljud
	 */
	boolean takeSound()
	{
		boolean pending = soundPending;
		soundPending = false;
		return pending;
	}
}
