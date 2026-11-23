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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Ett mål i grafen. Ordningen i {@link #childIds} är användarens egen sortering.
 * Fälten serialiseras direkt med Gson, så namnen får inte bytas utan migrering.
 */
@Getter
@Setter
public class Goal
{
	private String id;
	private String title;
	private String note;
	private boolean completed;
	// Tidpunkt (epoch ms) då målet bockades av, null om det inte är klart.
	private Long completedAt;
	// True när completedAt bara är när målet upptäcktes som klart (första synken efter
	// inloggning, profilbyte, eller redan uppfyllt när det lades till), inte när det klarades.
	private boolean completedAtApprox;
	// Tidpunkt (epoch ms) då målet skapades; null för mål från före schemaversion 6.
	private Long createdAt;
	private boolean expanded = true;
	// Visas som räknare "X/Y" över de direkta delmålen. Utan eget krav blir målet klart
	// när alla direkta delmål är klara. Valfritt fält, false för äldre data.
	private boolean aggregate;
	// Templaten målet är rot för (t.ex. "quest-cape"); null för vanliga mål.
	private String templateId;
	// Parametrarna templaten byggdes med (t.ex. N i Base N); bara på template-roten.
	private java.util.Map<String, Integer> templateParams;
	// Nycklar för noder användaren valt bort; de föreslås inte vid uppdatering. Bara på roten.
	private List<String> templateIgnoredKeys;
	// Stabil nyckel för noden i templaten målet skapades från, t.ex. "quest:117".
	private String templateNodeKey;
	// Egen wikisida från en handplockad template; går före sidan som härleds ur kravet.
	private String wikiPage;
	// Var föremålet droppar och hur ofta; bara på item-mål, för torrhetsmätaren.
	private DropSource dropSource;
	// Räknarens värde när ett mål med drop-källa klarades; null om det inte var känt.
	private Long obtainedAtKc;
	// Null betyder ett manuellt mål som bara bockas av för hand.
	private Requirement requirement;
	// Delmålens id:n i användarens ordning. Målen själva ligger i GoalTree, så att ett
	// delmål kan finnas under flera föräldrar men bara lagras en gång.
	private List<String> childIds = new ArrayList<>();

	public Goal()
	{
	}

	public Goal(String title)
	{
		this.id = UUID.randomUUID().toString();
		this.title = title;
		this.createdAt = System.currentTimeMillis();
	}

	/**
	 * Egen titel om den finns, annars kravets standardbeskrivning.
	 */
	public String getDisplayTitle()
	{
		if ((title == null || title.trim().isEmpty()) && requirement != null)
		{
			return requirement.describe();
		}
		return title == null ? "" : title;
	}

	public boolean hasChildren()
	{
		return childIds != null && !childIds.isEmpty();
	}

	/**
	 * Bockar av eller avmarkerar målet med ett exakt datum. Avmarkering nollställer
	 * både datumet och approx-flaggan.
	 */
	public void markCompleted(boolean completed, long now)
	{
		markCompleted(completed, now, false);
	}

	/**
	 * @param approx true om {@code now} bara är när målet upptäcktes som klart
	 */
	public void markCompleted(boolean completed, long now, boolean approx)
	{
		this.completed = completed;
		this.completedAt = completed ? now : null;
		this.completedAtApprox = completed && approx;
		if (!completed)
		{
			obtainedAtKc = null;
		}
	}
}
