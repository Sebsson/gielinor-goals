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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

/**
 * Ett val som användaren gör innan templaten byggs: en siffra (t.ex. N i Base N) eller ett av
 * flera värden (t.ex. en skill). Siffror sparas i template-rotens {@code templateParams},
 * val i {@code templateChoices}, så att "Update from template" kan bygga om likadant.
 */
@Getter
@EqualsAndHashCode
public final class TemplateParameter
{
	public enum Kind
	{
		NUMBER,
		CHOICE
	}

	/**
	 * Ett värde i ett val: det som sparas (stabilt, t.ex. "ATTACK") och det som visas.
	 */
	@Value
	public static class Option
	{
		String value;
		String label;

		@Override
		public String toString()
		{
			return label;
		}
	}

	private final Kind kind;
	private final String key;
	private final String label;
	private final int min;
	private final int max;
	private final int defaultValue;
	private final List<Option> options;
	@Nullable
	private final String defaultChoice;

	private TemplateParameter(Kind kind, String key, String label, int min, int max, int defaultValue,
		List<Option> options, @Nullable String defaultChoice)
	{
		this.kind = kind;
		this.key = key;
		this.label = label;
		this.min = min;
		this.max = max;
		this.defaultValue = defaultValue;
		this.options = options;
		this.defaultChoice = defaultChoice;
	}

	public static TemplateParameter number(String key, String label, int min, int max, int defaultValue)
	{
		return new TemplateParameter(Kind.NUMBER, key, label, min, max, defaultValue, Collections.emptyList(), null);
	}

	/**
	 * @param defaultChoice värdet som är valt från början; måste finnas bland {@code options}
	 */
	public static TemplateParameter choice(String key, String label, List<Option> options, String defaultChoice)
	{
		if (options.isEmpty() || options.stream().noneMatch(option -> option.getValue().equals(defaultChoice)))
		{
			throw new IllegalArgumentException("The default must be one of the options");
		}
		return new TemplateParameter(Kind.CHOICE, key, label, 0, 0, 0, List.copyOf(options), defaultChoice);
	}

	/**
	 * Siffran från parameterkartan, inom gränserna; standardvärdet om den saknas.
	 */
	public int valueIn(Map<String, Integer> values)
	{
		Integer value = values.get(key);
		if (value == null)
		{
			return defaultValue;
		}
		return Math.max(min, Math.min(max, value));
	}

	/**
	 * Valet från valkartan, eller standardvalet om det saknas eller inte längre finns bland
	 * alternativen (t.ex. i äldre sparad data).
	 */
	public String choiceIn(Map<String, String> choices)
	{
		String value = choices.get(key);
		for (Option option : options)
		{
			if (option.getValue().equals(value))
			{
				return value;
			}
		}
		return defaultChoice;
	}
}
