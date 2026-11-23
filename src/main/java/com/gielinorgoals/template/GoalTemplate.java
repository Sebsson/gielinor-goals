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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * En färdig mall som bygger ett förslag på mål. Rotnoden blir ett nytt rotmål när
 * förslaget sätts in.
 */
public interface GoalTemplate
{
	/**
	 * Stabilt id som sparas på template-roten, t.ex. "quest-cape". Får inte ändras.
	 */
	String getId();

	String getName();

	String getDescription();

	/**
	 * Kategorin i template-dialogen, se {@link TemplateCategories}.
	 */
	String getCategory();

	/**
	 * Sökord utöver namnet, t.ex. "rfd" eller "ironman".
	 */
	default List<String> getTags()
	{
		return Collections.emptyList();
	}

	/**
	 * Ett krav vars ikon representerar templaten i listan, eller null för ingen ikon.
	 */
	@Nullable
	default Requirement getIconRequirement()
	{
		return null;
	}

	/**
	 * Egen ikon för templaten i listan när den saknar ett ikonkrav.
	 */
	@Nullable
	default com.gielinorgoals.model.GoalIcon getIcon()
	{
		return null;
	}

	/**
	 * Om "Show full quest chains" ska vara ibockad från början i template-dialogen.
	 */
	default boolean defaultExpandQuestChains()
	{
		return false;
	}

	default List<TemplateParameter> getParameters()
	{
		return Collections.emptyList();
	}

	/**
	 * Bygger ett nytt förslag. Varje anrop ger ett nytt träd, så att användarens
	 * urbockningar i ett förslag inte påverkar nästa.
	 *
	 * @param parameters värden för {@link #getParameters()}; saknade får standardvärdet
	 */
	TemplateNode build(Map<String, Integer> parameters);
}
