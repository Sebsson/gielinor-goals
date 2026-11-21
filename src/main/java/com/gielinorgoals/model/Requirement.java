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

import javax.annotation.Nullable;

/**
 * Ett automatiskt krav som avgör när ett mål är klart. Varje subklass registreras
 * med ett typnamn i {@link RequirementType} så att den kan sparas som JSON.
 */
public abstract class Requirement
{
	public abstract RequirementType getType();

	/**
	 * Standardtitel när användaren inte har skrivit någon egen.
	 */
	public abstract String describe();

	/**
	 * Kontrollerar att fälten är rimliga efter inläsning från JSON.
	 */
	public abstract boolean isValid();

	/**
	 * @return aktuell progress, eller null om spelets data inte är känd än
	 */
	@Nullable
	public abstract GoalProgress progress(GameData data);

	/**
	 * Exakta siffror för hover-texten i panelen, en rad per radbrytning.
	 *
	 * @return texten, eller null om kravet inte har några siffror att visa
	 */
	@Nullable
	public String describeProgress(GoalProgress progress)
	{
		return null;
	}
}
