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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

/**
 * Visar en delningssträng: den kopieras direkt till urklipp och kan även sparas som fil.
 * Inget skickas någonstans; delningen sker bara via urklipp eller fil.
 */
@Slf4j
final class ShareDialog
{
	static final String FILE_EXTENSION = "txt";
	// Längre strängar visas förkortade; urklippet och filen får alltid hela.
	private static final int SHOWN_CHARS = 20_000;

	private ShareDialog()
	{
	}

	static void show(Component owner, String title, String what, String share, String fileName)
	{
		copyToClipboard(share);
		JTextArea area = new JTextArea(share.length() > SHOWN_CHARS ? share.substring(0, SHOWN_CHARS) + "..." : share);
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(false);
		area.setCaretPosition(0);
		JScrollPane scroll = new JScrollPane(area);
		scroll.setPreferredSize(new Dimension(380, 140));

		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.add(new JLabel("<html><body style='width:360px'>" + UiText.escapeHtml("Copied " + what
			+ " to the clipboard (" + share.length() + " characters). Paste it in Import... on any account, or"
			+ " save it as a file.") + "</body></html>"), BorderLayout.NORTH);
		content.add(scroll, BorderLayout.CENTER);

		Object[] options = {"Save as file...", "Close"};
		int choice = JOptionPane.showOptionDialog(owner, content, title, JOptionPane.DEFAULT_OPTION,
			JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
		if (choice == 0)
		{
			saveToFile(owner, share, fileName);
		}
	}

	static void copyToClipboard(String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	private static void saveToFile(Component owner, String share, String fileName)
	{
		List<Filepath> chosen = new Filepath.Chooser()
			.setIsSave()
			.setAcceptsFiles()
			.setDialogTitle("Save share string")
			.addExtensionFilter("Gielinor Goals share string", FILE_EXTENSION)
			.setDefaultExtension(FILE_EXTENSION)
			.setFileName(safeFileName(fileName))
			.showDialog(owner);
		if (chosen.isEmpty())
		{
			return;
		}
		try
		{
			chosen.get(0).write(share);
			JOptionPane.showMessageDialog(owner, "Saved " + chosen.get(0).getFileName() + ".", "Gielinor Goals",
				JOptionPane.INFORMATION_MESSAGE);
		}
		catch (IOException e)
		{
			log.warn("Could not save the share string", e);
			JOptionPane.showMessageDialog(owner, "Could not save the file: " + e.getMessage(), "Gielinor Goals",
				JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * Ett filnamn med bara bokstäver, siffror och bindestreck, t.ex. "gielinor-goals-raid-prep.txt".
	 */
	static String safeFileName(String name)
	{
		String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
		if (slug.length() > 40)
		{
			slug = slug.substring(0, 40).replaceAll("-+$", "");
		}
		return "gielinor-goals" + (slug.isEmpty() ? "" : "-" + slug) + "." + FILE_EXTENSION;
	}
}
