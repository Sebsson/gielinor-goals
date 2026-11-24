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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;

/**
 * Delningssträngar: {@code GG1:} följt av base64 av gzippad JSON i template-formatet. Används för
 * att dela en template, exportera ett mål med delgraf och säkerhetskopiera alla mål. Strängen
 * kopieras eller sparas som fil av användaren; inget skickas till eller hämtas från någon server.
 * <p>
 * En inklistrad sträng är opålitlig: storleken begränsas både före och efter uppackning (mot
 * gzip-bomber), strukturen kontrolleras fält för fält och tolkas sedan bara via Gson och
 * template-parsern. Ett fel ger ett begripligt meddelande och ingen import alls.
 */
@Singleton
public class ShareCodec
{
	public static final String PREFIX = "GG1:";
	// Strängen som klistras in eller läses från fil.
	public static final int MAX_STRING_CHARS = 4_000_000;
	// JSON-texten efter uppackning.
	public static final int MAX_UNPACKED_BYTES = 4 * 1024 * 1024;

	/**
	 * Vad strängen innehåller: en template, eller mål som slås ihop med profilens.
	 */
	public enum Kind
	{
		TEMPLATE,
		GOALS
	}

	/**
	 * Ett fel i en inklistrad sträng, med ett meddelande som kan visas för användaren.
	 */
	public static final class ShareException extends Exception
	{
		public ShareException(String message)
		{
			super(message);
		}
	}

	// Fälten fylls av Gson.
	private static final class Envelope
	{
		Integer formatVersion;
		String kind;
		JsonTemplate.FileSpec template;
	}

	/**
	 * En tolkad och kontrollerad sträng.
	 */
	public static final class Payload
	{
		@Getter
		private final Kind kind;
		private final JsonTemplate.FileSpec spec;
		@Getter
		private final JsonTemplate template;

		private Payload(Kind kind, JsonTemplate.FileSpec spec, JsonTemplate template)
		{
			this.kind = kind;
			this.spec = spec;
			this.template = template;
		}

		JsonTemplate.FileSpec getSpec()
		{
			return spec;
		}

		public String getName()
		{
			return spec.name;
		}

		public int getNodeCount()
		{
			return CustomTemplates.countNodes(spec.root);
		}
	}

	private final Gson gson;

	@Inject
	public ShareCodec(Gson gson)
	{
		this.gson = gson;
	}

	/**
	 * Ett mål med delgraf, eller alla mål ({@link TemplateExport#exportAll}).
	 */
	public String encode(TemplateExport.Result export, Kind kind)
	{
		return encode(export.getSpec(), kind);
	}

	/**
	 * En egen template.
	 *
	 * @return strängen, eller null om templaten inte finns längre
	 */
	@Nullable
	public String encode(CustomTemplates custom, String templateId)
	{
		JsonTemplate.FileSpec spec = custom.spec(templateId);
		if (spec == null)
		{
			return null;
		}
		// Id:t hör till den som delar; mottagaren får ett eget när den sparar.
		spec.id = null;
		spec.category = null;
		return encode(spec, Kind.TEMPLATE);
	}

	String encode(JsonTemplate.FileSpec spec, Kind kind)
	{
		Envelope envelope = new Envelope();
		envelope.formatVersion = JsonTemplate.FORMAT_VERSION;
		envelope.kind = kind.name().toLowerCase(Locale.ROOT);
		envelope.template = spec;
		byte[] json = gson.toJson(envelope).getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(bytes))
		{
			gzip.write(json);
		}
		catch (IOException e)
		{
			// Skrivs till minnet; kan inte hända.
			throw new IllegalStateException(e);
		}
		return PREFIX + Base64.getEncoder().encodeToString(bytes.toByteArray());
	}

	/**
	 * Tolkar och kontrollerar en sträng.
	 *
	 * @throws ShareException med vad som var fel
	 */
	public Payload decode(String text) throws ShareException
	{
		if (text.length() > MAX_STRING_CHARS)
		{
			throw new ShareException("The share string is too long (more than " + MAX_STRING_CHARS / 1_000_000
				+ " million characters).");
		}
		// Radbrytningar och mellanslag kan komma med när strängen kopieras.
		String compact = text.replaceAll("\\s+", "");
		if (compact.isEmpty())
		{
			throw new ShareException("Paste a share string first.");
		}
		if (!compact.startsWith(PREFIX))
		{
			if (compact.matches("GG\\d+:.*"))
			{
				throw new ShareException("The share string is from a newer version of Gielinor Goals. Update the plugin"
					+ " and try again.");
			}
			throw new ShareException("This is not a Gielinor Goals share string; it should start with " + PREFIX);
		}

		byte[] packed;
		try
		{
			packed = Base64.getDecoder().decode(compact.substring(PREFIX.length()));
		}
		catch (IllegalArgumentException e)
		{
			throw new ShareException("The share string is damaged: it contains characters that don't belong there.");
		}
		String json = new String(unpack(packed), StandardCharsets.UTF_8);

		JsonElement element;
		try
		{
			element = gson.fromJson(json, JsonElement.class);
		}
		catch (JsonParseException e)
		{
			throw new ShareException("The share string is damaged: its contents can't be read.");
		}
		if (element == null)
		{
			throw new ShareException("The share string is empty.");
		}

		try
		{
			ImportValidator.validateEnvelope(element);
			Envelope envelope = gson.fromJson(element, Envelope.class);
			if (envelope.formatVersion == null || envelope.formatVersion < 1
				|| envelope.formatVersion > JsonTemplate.FORMAT_VERSION)
			{
				throw new ShareException("The share string is from a newer version of Gielinor Goals. Update the"
					+ " plugin and try again.");
			}
			Kind kind = kindOf(envelope.kind);
			JsonTemplate.FileSpec spec = envelope.template;
			// Det delade id:t och kategorin används inte; en sparad import får egna.
			spec.id = null;
			spec.category = null;
			JsonTemplate template = JsonTemplate.from(spec, "import");
			// Bygg en gång till för att få fel i enskilda noder (t.ex. okända quests) nu.
			template.build(Map.of());
			return new Payload(kind, spec, template);
		}
		catch (JsonParseException | NumberFormatException e)
		{
			throw new ShareException("The share string has a value of the wrong kind, e.g. text where a number"
				+ " should be.");
		}
		catch (IllegalArgumentException e)
		{
			throw new ShareException("The share string can't be imported: " + e.getMessage());
		}
	}

	private static Kind kindOf(@Nullable String kind) throws ShareException
	{
		if ("template".equals(kind))
		{
			return Kind.TEMPLATE;
		}
		if ("goals".equals(kind))
		{
			return Kind.GOALS;
		}
		throw new ShareException("The share string has an unknown kind of content.");
	}

	// Packar upp, men aldrig mer än gränsen: en liten sträng kan annars bli gigabyte.
	private static byte[] unpack(byte[] packed) throws ShareException
	{
		try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(packed)))
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) != -1)
			{
				if (out.size() + read > MAX_UNPACKED_BYTES)
				{
					throw new ShareException("The share string is too large when unpacked (more than "
						+ MAX_UNPACKED_BYTES / (1024 * 1024) + " MB).");
				}
				out.write(buffer, 0, read);
			}
			return out.toByteArray();
		}
		catch (EOFException e)
		{
			throw new ShareException("The share string is incomplete. Was it cut off when it was copied?");
		}
		catch (ZipException e)
		{
			throw new ShareException("The share string is damaged: its contents can't be unpacked.");
		}
		catch (IOException e)
		{
			throw new ShareException("The share string is damaged: its contents can't be unpacked.");
		}
	}
}
