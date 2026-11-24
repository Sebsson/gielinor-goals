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

import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.Value;
import net.runelite.client.callback.ClientThread;

/**
 * Bygger template-förslag. Templates som bara använder statisk data byggs direkt på EDT;
 * de som behöver spelets data ({@link GoalTemplate#needsClientData()}) byggs på klienttråden
 * och lämnas sedan tillbaka på EDT. En template som inte kan byggas just nu (t.ex. utloggad)
 * ger ett meddelande i stället för ett förslag.
 */
@Singleton
public class TemplateBuilder
{
	/**
	 * Antingen ett förslag eller ett meddelande om varför det inte gick.
	 */
	@Value
	public static class Result
	{
		@Nullable
		TemplateNode node;
		@Nullable
		String message;

		public boolean isReady()
		{
			return node != null;
		}
	}

	private final Consumer<Runnable> onClientThread;
	private final Consumer<Runnable> onEdt;

	@Inject
	TemplateBuilder(ClientThread clientThread)
	{
		this(clientThread::invokeLater, SwingUtilities::invokeLater);
	}

	// För tester: egna "trådar".
	TemplateBuilder(Consumer<Runnable> onClientThread, Consumer<Runnable> onEdt)
	{
		this.onClientThread = onClientThread;
		this.onEdt = onEdt;
	}

	/**
	 * Bygger templaten med de givna värdena. {@code callback} körs på EDT, direkt för statiska
	 * templates och senare för de som behöver spelets data.
	 */
	public void build(GoalTemplate template, Map<String, Integer> numbers, Map<String, String> choices,
		Consumer<Result> callback)
	{
		if (!template.needsClientData())
		{
			callback.accept(tryBuild(template, numbers, choices));
			return;
		}
		onClientThread.accept(() ->
		{
			Result result = tryBuild(template, numbers, choices);
			onEdt.accept(() -> callback.accept(result));
		});
	}

	private static Result tryBuild(GoalTemplate template, Map<String, Integer> numbers, Map<String, String> choices)
	{
		try
		{
			return new Result(template.build(numbers, choices), null);
		}
		catch (TemplateUnavailableException e)
		{
			return new Result(null, e.getMessage());
		}
	}
}
