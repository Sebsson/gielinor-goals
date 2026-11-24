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

import com.gielinorgoals.model.Requirement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

/**
 * En nod i ett template-förslag: titel, krav, räknarflagga och delmål. Det är bara ett
 * förslag; riktiga mål skapas först när användaren bekräftar insättningen.
 */
@Getter
public class TemplateNode
{
	private final String title;
	@Nullable
	private final Requirement requirement;
	private final boolean aggregate;
	private final List<TemplateNode> children = new ArrayList<>();

	// Användaren kan bocka ur delar i förhandsvisningen; urbockade noder och deras
	// delmål läggs inte till.
	@Setter
	private boolean included = true;

	// Bara på roten: vilken template förslaget kommer från och med vilka parametrar,
	// så att en insatt template kan byggas om och jämföras senare.
	@Setter
	@Nullable
	private String templateId;
	@Setter
	@Nullable
	private java.util.Map<String, Integer> templateParams;
	// Valparametrarna (t.ex. vilken skill), som text.
	@Setter
	@Nullable
	private java.util.Map<String, String> templateChoices;

	// Egen wikisida från en handplockad template, t.ex. "Barrows gloves".
	@Setter
	@Nullable
	private String wikiPage;

	// Anteckning som följer med till målet, t.ex. "Bought for 750 slayer points".
	@Setter
	@Nullable
	private String note;

	// Var föremålet droppar; bara på item-noder.
	@Setter
	@Nullable
	private com.gielinorgoals.model.DropSource dropSource;

	// Egen ikon för noder utan krav, t.ex. grupperna "Unlock" och "Obtain".
	@Setter
	@Nullable
	private com.gielinorgoals.model.GoalIcon icon;

	// Nycklar till mål som gör den här noden klar, t.ex. "item:12954" (dragon defender).
	@Setter
	@Nullable
	private List<String> doneWhen;

	// False om ett quest inte ska fällas ut med sin kedja trots att kedjor är påslagna.
	@Setter
	private boolean expandChain = true;

	// Delade noder i en egen template: noden med localId sätts in en gång, och en nod med
	// ref (samma titel och krav, inga egna barn) kopplar in samma mål under en till förälder.
	@Setter
	@Nullable
	private String localId;
	@Setter
	@Nullable
	private String ref;

	public TemplateNode(String title, @Nullable Requirement requirement, boolean aggregate)
	{
		this.title = title;
		this.requirement = requirement;
		this.aggregate = aggregate;
	}

	public static TemplateNode aggregate(String title)
	{
		return new TemplateNode(title, null, true);
	}

	/**
	 * Ett mål med krav och utan egen titel; titeln blir kravets standardbeskrivning.
	 */
	public static TemplateNode requirement(Requirement requirement)
	{
		return new TemplateNode("", requirement, false);
	}

	public TemplateNode add(TemplateNode child)
	{
		children.add(child);
		return this;
	}

	public List<TemplateNode> getChildren()
	{
		return Collections.unmodifiableList(children);
	}

	/**
	 * Titeln som visas: egen titel, annars kravets beskrivning.
	 */
	public String getDisplayTitle()
	{
		if (title.isEmpty() && requirement != null)
		{
			return requirement.describe();
		}
		return title;
	}

	/**
	 * Stabil nyckel för noden, t.ex. "quest:117"; se {@link TemplateKeys}.
	 */
	public String getKey()
	{
		return TemplateKeys.of(this);
	}

	/**
	 * Antal noder i trädet, inklusive den här.
	 */
	public int size()
	{
		int count = 1;
		for (TemplateNode child : children)
		{
			count += child.size();
		}
		return count;
	}
}
