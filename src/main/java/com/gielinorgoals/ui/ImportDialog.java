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

import com.gielinorgoals.GoalManager;
import com.gielinorgoals.ProfileGuard;
import com.gielinorgoals.template.CustomTemplates;
import com.gielinorgoals.template.ShareCodec;
import com.gielinorgoals.template.TemplateInserter;
import com.gielinorgoals.template.TemplateNode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

/**
 * "Import...": klistra in en delningssträng eller välj en fil, se förslaget som för en
 * template och lägg till det som mål, spara det som egen template, eller båda. En hel profil
 * slås ihop med de befintliga målen och ersätter aldrig något.
 */
@Slf4j
final class ImportDialog
{
	private ImportDialog()
	{
	}

	static void show(Component owner, GoalManager goalManager, DialogServices services)
	{
		ProfileGuard guard = ProfileGuard.of(goalManager);
		ShareCodec.Payload payload = askForPayload(owner, services.getShareCodec());
		if (payload == null || !GoalActions.stillSameProfile(owner, guard))
		{
			return;
		}

		TemplateDialog.ImportChoice choice = TemplateDialog.showImport(owner, payload, goalManager.getTree(),
			services.currentGameData(), services.getIcons(), services.getQuestRequirements(),
			services.getTemplateBuilder(), services.getCompletionLookup());
		if (choice == null || !GoalActions.stillSameProfile(owner, guard))
		{
			return;
		}

		String savedId = null;
		if (choice.isSave())
		{
			try
			{
				savedId = services.getCustomTemplates().add(payload);
			}
			catch (CustomTemplates.TemplateException e)
			{
				JOptionPane.showMessageDialog(owner, "Could not save it as a template: " + e.getMessage(),
					"Import", JOptionPane.WARNING_MESSAGE);
				if (!choice.isAdd())
				{
					return;
				}
			}
		}

		String message;
		if (choice.isAdd())
		{
			TemplateNode node = choice.getNode();
			TemplateInserter.Result result;
			if (payload.getKind() == ShareCodec.Kind.GOALS)
			{
				result = goalManager.importGoals(node);
			}
			else
			{
				// Bara en sparad template kan uppdateras senare; annars blir det vanliga mål.
				node.setTemplateId(savedId);
				if (savedId == null)
				{
					node.setTemplateParams(null);
					node.setTemplateChoices(null);
				}
				result = goalManager.insertTemplate(node);
			}
			if (result == null)
			{
				return;
			}
			message = "Added " + result.getCreated().size() + " new " + (result.getCreated().size() == 1 ? "goal" : "goals")
				+ (result.getLinked().isEmpty() ? "" : " and linked " + result.getLinked().size() + " you already had")
				+ "." + (savedId != null ? " It is also saved under My templates." : "");
		}
		else
		{
			message = savedId != null ? "Saved '" + payload.getName() + "' under My templates." : null;
		}
		if (message != null)
		{
			JOptionPane.showMessageDialog(owner, message, "Import", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	// Frågar tills strängen går att tolka eller användaren avbryter.
	@Nullable
	private static ShareCodec.Payload askForPayload(Component owner, ShareCodec codec)
	{
		JTextArea area = new JTextArea();
		area.setLineWrap(true);
		JScrollPane scroll = new JScrollPane(area);
		scroll.setPreferredSize(new Dimension(380, 140));
		JButton fileButton = new JButton("Choose file...");
		fileButton.setFocusPainted(false);
		fileButton.addActionListener(e ->
		{
			String text = readFile(owner);
			if (text != null)
			{
				area.setText(text);
				area.setCaretPosition(0);
			}
		});

		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.add(new JLabel("<html><body style='width:360px'>Paste a share string (it starts with "
			+ ShareCodec.PREFIX + ") or choose a file. Nothing is added until you have seen what it"
			+ " contains.</body></html>"), BorderLayout.NORTH);
		content.add(scroll, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new BorderLayout());
		buttons.add(fileButton, BorderLayout.WEST);
		content.add(buttons, BorderLayout.SOUTH);

		Object[] options = {"Next", "Cancel"};
		while (true)
		{
			int choice = JOptionPane.showOptionDialog(owner, content, "Import", JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			if (choice != 0)
			{
				return null;
			}
			try
			{
				return codec.decode(area.getText());
			}
			catch (ShareCodec.ShareException e)
			{
				JOptionPane.showMessageDialog(owner, e.getMessage(), "Import", JOptionPane.WARNING_MESSAGE);
			}
		}
	}

	// Läser en vald fil, men aldrig mer än en delningssträng får vara.
	@Nullable
	private static String readFile(Component owner)
	{
		List<Filepath> chosen = new Filepath.Chooser()
			.setIsOpen()
			.setAcceptsFiles()
			.setDialogTitle("Import from file")
			.addExtensionFilter("Gielinor Goals share string", ShareDialog.FILE_EXTENSION)
			.showDialog(owner);
		if (chosen.isEmpty())
		{
			return null;
		}
		Filepath file = chosen.get(0);
		try
		{
			if (file.size() > ShareCodec.MAX_STRING_CHARS)
			{
				JOptionPane.showMessageDialog(owner, "The file is too large to be a share string.", "Import",
					JOptionPane.WARNING_MESSAGE);
				return null;
			}
			StringBuilder text = new StringBuilder();
			try (BufferedReader reader = file.openBufferedReader())
			{
				char[] buffer = new char[8192];
				int read;
				while ((read = reader.read(buffer)) != -1 && text.length() <= ShareCodec.MAX_STRING_CHARS)
				{
					text.append(buffer, 0, read);
				}
			}
			return text.toString();
		}
		catch (IOException e)
		{
			log.warn("Could not read the import file", e);
			JOptionPane.showMessageDialog(owner, "Could not read the file: " + e.getMessage(), "Import",
				JOptionPane.WARNING_MESSAGE);
			return null;
		}
	}
}
