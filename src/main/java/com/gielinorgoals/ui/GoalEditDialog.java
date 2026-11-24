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

import com.gielinorgoals.model.CombatAchievementRequirement;
import com.gielinorgoals.model.CombatLevelRequirement;
import com.gielinorgoals.model.DiaryArea;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.DiaryTier;
import com.gielinorgoals.model.DropSource;
import com.gielinorgoals.GoalManager;
import com.gielinorgoals.model.ActivityCounters;
import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.GoalProgress;
import com.gielinorgoals.model.ItemRequirement;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.PointsActivity;
import com.gielinorgoals.model.PointsRequirement;
import com.gielinorgoals.model.QuestPointsRequirement;
import com.gielinorgoals.model.QuestRequirement;
import com.gielinorgoals.template.QuestChains;
import com.gielinorgoals.template.TemplateInserter;
import com.gielinorgoals.template.TemplateNode;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.model.SkillRequirement;
import com.gielinorgoals.model.TotalLevelRequirement;
import com.gielinorgoals.tracking.AchievementTracker;
import com.gielinorgoals.tracking.CompletionStatus;
import com.gielinorgoals.tracking.SkillTracker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import lombok.Value;
import net.runelite.api.Experience;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.QuantityFormatter;

/**
 * Modal dialog för att skapa eller redigera ett mål. Typspecifika fält ligger i ett
 * CardLayout, som alltid är lika stort som det största kortet; en JOptionPane ändrar
 * nämligen inte storlek när innehållet byts.
 *
 * Dialogen är "snäll": sådant som redan är klart (quests, diary-nivåer, CA-uppgifter,
 * levels man redan har) går inte att välja, och samma mål kan inte läggas till två gånger.
 */
final class GoalEditDialog
{
	enum GoalKind
	{
		MANUAL("Manual"),
		SKILL_LEVEL("Skill level"),
		SKILL_XP("Skill XP"),
		ITEM("Item"),
		QUEST("Quest"),
		DIARY("Achievement diary"),
		COMBAT_ACHIEVEMENT("Combat achievement"),
		TOTAL_LEVEL("Total level"),
		COMBAT_LEVEL("Combat level"),
		QUEST_POINTS("Quest points"),
		POINTS("Activity points"),
		KILL_COUNT("Kill count");

		// Ett tal mot ett mål, i samma kort; POINTS har dessutom ett val av aktivitet.
		boolean isAccountLevel()
		{
			return this == TOTAL_LEVEL || this == COMBAT_LEVEL || this == QUEST_POINTS || this == POINTS;
		}

		private final String label;

		GoalKind(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@Value
	static class Result
	{
		String title;
		String note;
		@Nullable
		Requirement requirement;
		// Visa som räknare över delmålen ("X/Y").
		boolean aggregate;
		// Nuvarande värde för ett KC-mål om användaren angav eller ändrade det, annars null.
		@Nullable
		Long startCount;
		// Lägg till questets krav (förkrav-quests, skills, QP) som delmål.
		boolean expandQuest;
		// Var föremålet droppar, för torrhetsmätaren; bara för item-mål.
		@Nullable
		DropSource dropSource;
	}

	/**
	 * Lägger till målet från dialogen under {@code parentId} (null = rotnivå). Med
	 * "Add the quest's requirements as sub-goals" sätts questet in med hela sin kedja,
	 * med samma dubblettregel som templates.
	 */
	static void addGoal(GoalManager goalManager, DialogServices services, @Nullable String parentId, Result result)
	{
		if (result.isExpandQuest() && result.getRequirement() instanceof QuestRequirement)
		{
			QuestRequirement quest = (QuestRequirement) result.getRequirement();
			TemplateNode node = QuestChains.forQuest(result.getTitle(), quest, result.isAggregate(),
				services.getQuestRequirements(), goalManager.getTree());
			TemplateInserter.Result inserted = goalManager.insertTemplate(parentId, node);
			String note = result.getNote() == null ? "" : result.getNote().trim();
			if (inserted != null && !note.isEmpty())
			{
				goalManager.editGoal(inserted.getRoot().getId(), result.getTitle(), note, quest, result.isAggregate());
			}
			return;
		}
		goalManager.addGoal(parentId, result.getTitle(), result.getNote(), result.getRequirement(), result.isAggregate(),
			result.getDropSource());
		applyStartCount(goalManager, result);
	}

	/**
	 * Ett val i KC-sökningen: en hiscore-aktivitet, eller "annan NPC" / "egen räknare".
	 */
	@Value
	static class KcOption
	{
		KillCountRequirement.ActivityType type;
		@Nullable
		HiscoreSkill skill;
		String label;

		@Override
		public String toString()
		{
			return label;
		}
	}

	static final KcOption OTHER_NPC = new KcOption(KillCountRequirement.ActivityType.NPC, null,
		"Other NPC (by name)...");
	static final KcOption MANUAL_COUNTER = new KcOption(KillCountRequirement.ActivityType.MANUAL, null,
		"Manual counter...");
	// Första valet i drop-källan: ingen torrhetsmätare.
	static final KcOption NO_DROP_SOURCE = new KcOption(null, null, "None");

	/**
	 * Sätter KC-målets nuvarande värde efter att målet lagts till eller ändrats.
	 */
	static void applyStartCount(GoalManager goalManager, Result result)
	{
		if (result.getStartCount() != null && result.getRequirement() instanceof KillCountRequirement)
		{
			goalManager.setCounter(((KillCountRequirement) result.getRequirement()).counterKey(), result.getStartCount());
		}
	}

	@Value
	static class QuestOption
	{
		int id;
		String name;
		boolean inProgress;

		@Override
		public String toString()
		{
			return inProgress ? name + " (in progress)" : name;
		}
	}

	@Value
	static class DiaryOption
	{
		DiaryArea area;
		DiaryTier tier;

		@Override
		public String toString()
		{
			return area.getDisplayName() + " - " + tier.getDisplayName();
		}
	}

	static final int MAX_LIST_RESULTS = 500;

	private final DialogServices services;
	@Nullable
	private final String editingGoalId;
	// Kravet målet hade när dialogen öppnades; det får sparas igen även om det redan är nått.
	@Nullable
	private final Requirement originalRequirement;
	private CompletionStatus status = CompletionStatus.UNKNOWN;

	private final JComboBox<GoalKind> kindBox = new JComboBox<>(GoalKind.values());
	private final JTextField titleField = new JTextField(20);
	private final JTextArea noteArea = new JTextArea(3, 20);
	private final JCheckBox aggregateBox = new JCheckBox("Show as counter (sub-goals done, e.g. 142/170)");
	private final JCheckBox questChainBox = new JCheckBox("Add the quest's requirements as sub-goals");
	private final CardLayout cards = new CardLayout();
	private final JPanel cardPanel = new JPanel(cards);

	// Skill-kortet.
	private final JComboBox<Skill> skillBox = new JComboBox<>(trackableSkills());
	private final JLabel targetLabel = new JLabel();
	private final JTextField targetField = new JTextField(20);
	private final JLabel currentSkillLabel = hintLabel();
	// Av: målet är en total XP. På: målet är XP att tjäna räknat från nu.
	private final JCheckBox gainBox = new JCheckBox("Count XP gained from now");

	// Kortet för total level, combat level och quest points: ett nummer med gränser.
	private static final String ACCOUNT_CARD = "ACCOUNT";
	private final JLabel accountTargetLabel = new JLabel();
	private final JTextField accountTargetField = new JTextField(20);
	// Aktiviteten för POINTS, t.ex. slayer points; syns bara för den typen.
	private final JComboBox<PointsActivity> pointsBox = new JComboBox<>(PointsActivity.values());
	private final JLabel accountCurrentLabel = hintLabel();

	// KC-kortet: sökbar aktivitet, namn för NPC/egen räknare, målantal och nuvarande värde.
	private static final String KILL_COUNT_CARD = "KILL_COUNT";
	private final SearchPanel<KcOption> kcPanel;
	private final JLabel kcNameLabel = new JLabel();
	private final JTextField kcNameField = new JTextField(20);
	private final JTextField kcTargetField = new JTextField(20);
	private final JTextField kcCurrentField = new JTextField(20);
	private final JLabel kcCurrentHint = hintLabel();
	private final Map<HiscoreSkill, Icon> activityIcons = new HashMap<>();

	// Föremålskortet.
	private final SearchPanel<ItemSearch.ItemOption> itemPanel;
	private final JTextField quantityField = new JTextField("1", 20);
	private final JCheckBox variantsBox = new JCheckBox("Also count variants (e.g. charged/uncharged)");
	private final JLabel ownedLabel = hintLabel();
	private final JComboBox<KcOption> dropBox = new JComboBox<>();
	private final JLabel dropNameLabel = new JLabel();
	private final JTextField dropNameField = new JTextField(20);
	private final JLabel dropRateLabel = new JLabel("Drop rate: 1 in (e.g. 5000 or 5k)");
	private final JTextField dropRateField = new JTextField(20);
	// Föremål per drop från en template; dialogen visar det inte men behåller det.
	private int dropPerDrop = 1;
	private final Map<Integer, Icon> itemIcons = new HashMap<>();

	// Quest-, diary- och CA-korten.
	private final SearchPanel<QuestOption> questPanel;
	private final SearchPanel<DiaryOption> diaryPanel;
	private final SearchPanel<CombatAchievementCatalog.Task> caPanel;

	private GoalEditDialog(DialogServices services, @Nullable String editingGoalId, String title,
		@Nullable String note, @Nullable Requirement requirement, boolean aggregate)
	{
		this.services = services;
		aggregateBox.setSelected(aggregate);
		aggregateBox.setToolTipText("Shows how many sub-goals are done and collapses the list. Without a requirement"
			+ " of its own, the goal is completed when all its sub-goals are.");
		this.editingGoalId = editingGoalId;
		this.originalRequirement = requirement;
		titleField.setText(title);
		noteArea.setText(note == null ? "" : note);
		noteArea.setLineWrap(true);
		noteArea.setWrapStyleWord(true);
		noteArea.putClientProperty("JTextField.placeholderText", "Optional note, e.g. where to get it");

		itemPanel = new SearchPanel<>("Type an item name, e.g. dragon pickaxe", services.getItemSearch()::search,
			ItemSearch.ItemOption::getName, item -> itemIcon(item.getId()), 2);
		itemPanel.setHint("Untradeable items are included.");
		itemPanel.setOnSelect(item -> updateOwned());

		questPanel = new SearchPanel<>("Filter quests", this::searchQuests, QuestOption::toString, quest -> null, 0);
		diaryPanel = new SearchPanel<>("Filter diaries, e.g. varrock", this::searchDiaries, DiaryOption::toString,
			diary -> null, 0);
		caPanel = new SearchPanel<>("Filter tasks or bosses", this::searchCombatAchievements,
			CombatAchievementCatalog.Task::toString, task -> null, 0);
		caPanel.setOnSelect(task -> caPanel.setToolTipText(task.getDescription()));

		cardPanel.add(new JPanel(), GoalKind.MANUAL.name());
		cardPanel.add(buildSkillCard(), GoalKind.SKILL_LEVEL.name());
		cardPanel.add(buildItemCard(), GoalKind.ITEM.name());
		cardPanel.add(questPanel, GoalKind.QUEST.name());
		cardPanel.add(diaryPanel, GoalKind.DIARY.name());
		cardPanel.add(caPanel, GoalKind.COMBAT_ACHIEVEMENT.name());
		cardPanel.add(buildAccountCard(), ACCOUNT_CARD);

		kcPanel = new SearchPanel<>("Search bosses, clues and activities", this::searchActivities, KcOption::getLabel,
			this::activityIcon, 0);
		kcPanel.setOnSelect(option -> updateKillCountFields());
		kcPanel.refresh();
		kcNameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
		{
			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent e)
			{
				prefillCurrentCount();
			}

			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent e)
			{
				prefillCurrentCount();
			}

			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent e)
			{
				prefillCurrentCount();
			}
		});
		cardPanel.add(buildKillCountCard(), KILL_COUNT_CARD);

		questPanel.setHint("Loading your quests...");
		diaryPanel.setHint("Loading your diaries...");
		caPanel.setHint("Loading your combat achievements...");

		prefill(requirement);
		kindBox.addActionListener(e -> updateFields());
		skillBox.addActionListener(e -> updateCurrentSkill());
		gainBox.addActionListener(e -> updateFields());
		updateFields();
		updateCurrentSkill();

		// Läs vad som redan är klart och fyll listorna när det är klart.
		services.getCompletionLookup().load(this::onStatusLoaded);
	}

	/**
	 * @param editingGoalId målet som redigeras (räknas inte som dubblett), eller null för ett nytt mål
	 * @return null om användaren avbröt
	 */
	@Nullable
	static Result show(Component parent, DialogServices services, String dialogTitle, @Nullable String editingGoalId,
		String title, @Nullable String note, @Nullable Requirement requirement, boolean aggregate)
	{
		return show(parent, services, dialogTitle, editingGoalId, title, note, requirement, aggregate, null);
	}

	/**
	 * @param dropSource målets drop-källa, eller null
	 */
	@Nullable
	static Result show(Component parent, DialogServices services, String dialogTitle, @Nullable String editingGoalId,
		String title, @Nullable String note, @Nullable Requirement requirement, boolean aggregate,
		@Nullable DropSource dropSource)
	{
		GoalEditDialog dialog = new GoalEditDialog(services, editingGoalId, title, note, requirement, aggregate);
		dialog.prefillDropSource(dropSource);
		JPanel content = dialog.buildContent();

		// Visa dialogen igen tills värdena är giltiga eller användaren avbryter.
		while (true)
		{
			int choice = JOptionPane.showConfirmDialog(parent, content, dialogTitle,
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (choice != JOptionPane.OK_OPTION)
			{
				return null;
			}

			try
			{
				return dialog.toResult();
			}
			catch (IllegalArgumentException e)
			{
				JOptionPane.showMessageDialog(parent, e.getMessage(), dialogTitle, JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	private void onStatusLoaded(CompletionStatus loaded)
	{
		status = loaded;
		String suffix = loaded.isKnown() ? " you have completed are hidden." : "";
		questPanel.setHint(loaded.isKnown() ? "Quests" + suffix : "Log in to hide quests you have done.");
		diaryPanel.setHint(loaded.isKnown()
			? "Done ones are hidden. Open the journal in game to add its tasks."
			: "Log in to hide diaries you have done.");
		caPanel.setHint(loaded.isKnown() ? "Tasks" + suffix : "Log in to hide tasks you have done.");
		questPanel.refresh();
		diaryPanel.refresh();
		caPanel.refresh();
	}

	private void prefill(@Nullable Requirement requirement)
	{
		if (requirement instanceof SkillRequirement)
		{
			SkillRequirement skill = (SkillRequirement) requirement;
			kindBox.setSelectedItem(skill.getMode() == SkillRequirement.Mode.LEVEL ? GoalKind.SKILL_LEVEL : GoalKind.SKILL_XP);
			gainBox.setSelected(skill.getMode() == SkillRequirement.Mode.XP_GAIN);
			skillBox.setSelectedItem(skill.getSkill());
			targetField.setText(String.valueOf(skill.getTarget()));
		}
		else if (requirement instanceof ItemRequirement)
		{
			ItemRequirement item = (ItemRequirement) requirement;
			kindBox.setSelectedItem(GoalKind.ITEM);
			itemPanel.preselect(new ItemSearch.ItemOption(item.getItemId(), item.getItemName()));
			quantityField.setText(String.valueOf(item.getQuantity()));
			variantsBox.setSelected(item.isIncludeVariants());
			updateOwned();
		}
		else if (requirement instanceof QuestRequirement)
		{
			QuestRequirement quest = (QuestRequirement) requirement;
			kindBox.setSelectedItem(GoalKind.QUEST);
			questPanel.preselect(new QuestOption(quest.getQuestId(), quest.getQuestName(), false));
		}
		else if (requirement instanceof DiaryRequirement)
		{
			DiaryRequirement diary = (DiaryRequirement) requirement;
			kindBox.setSelectedItem(GoalKind.DIARY);
			diaryPanel.preselect(new DiaryOption(diary.getArea(), diary.getTier()));
		}
		else if (requirement instanceof TotalLevelRequirement)
		{
			kindBox.setSelectedItem(GoalKind.TOTAL_LEVEL);
			accountTargetField.setText(String.valueOf(((TotalLevelRequirement) requirement).getTarget()));
		}
		else if (requirement instanceof CombatLevelRequirement)
		{
			kindBox.setSelectedItem(GoalKind.COMBAT_LEVEL);
			accountTargetField.setText(String.valueOf(((CombatLevelRequirement) requirement).getTarget()));
		}
		else if (requirement instanceof KillCountRequirement)
		{
			KillCountRequirement kc = (KillCountRequirement) requirement;
			kindBox.setSelectedItem(GoalKind.KILL_COUNT);
			HiscoreSkill skill = kc.hiscoreSkill();
			if (skill != null)
			{
				kcPanel.preselect(new KcOption(KillCountRequirement.ActivityType.HISCORE, skill, skill.getName()));
			}
			else
			{
				kcPanel.preselect(kc.getActivityType() == KillCountRequirement.ActivityType.NPC ? OTHER_NPC : MANUAL_COUNTER);
				kcNameField.setText(kc.getActivityName());
			}
			kcTargetField.setText(String.valueOf(kc.getTarget()));
			updateKillCountFields();
		}
		else if (requirement instanceof QuestPointsRequirement)
		{
			kindBox.setSelectedItem(GoalKind.QUEST_POINTS);
			accountTargetField.setText(String.valueOf(((QuestPointsRequirement) requirement).getTarget()));
		}
		else if (requirement instanceof PointsRequirement)
		{
			PointsRequirement points = (PointsRequirement) requirement;
			pointsBox.setSelectedItem(points.getActivity());
			kindBox.setSelectedItem(GoalKind.POINTS);
			accountTargetField.setText(String.valueOf(points.getTarget()));
		}
		else if (requirement instanceof CombatAchievementRequirement)
		{
			CombatAchievementRequirement task = (CombatAchievementRequirement) requirement;
			kindBox.setSelectedItem(GoalKind.COMBAT_ACHIEVEMENT);
			caPanel.preselect(new CombatAchievementCatalog.Task(task.getTaskId(), task.getTaskName(), "",
				task.getTierName()));
		}
		else
		{
			kindBox.setSelectedItem(GoalKind.MANUAL);
		}
	}

	private JPanel buildContent()
	{
		kindBox.setMaximumRowCount(GoalKind.values().length);
		JPanel top = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		top.add(new JLabel("What kind of goal?"));
		top.add(kindBox);

		JPanel titlePanel = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		titlePanel.add(new JLabel("Title"));
		titlePanel.add(titleField);

		JScrollPane noteScroll = new JScrollPane(noteArea);
		noteScroll.setPreferredSize(new Dimension(280, 56));
		JPanel bottom = new JPanel(new BorderLayout(0, 4));
		bottom.add(titlePanel, BorderLayout.NORTH);
		bottom.add(noteScroll, BorderLayout.CENTER);
		questChainBox.setToolTipText("Adds the quests, skill levels and quest points it needs, recursively."
			+ " Goals you already have are linked in instead of copied.");
		JPanel checkBoxes = new JPanel(new DynamicGridLayout(0, 1, 0, 2));
		checkBoxes.add(questChainBox);
		checkBoxes.add(aggregateBox);
		bottom.add(checkBoxes, BorderLayout.SOUTH);

		JPanel content = new JPanel(new BorderLayout(0, 10));
		content.add(top, BorderLayout.NORTH);
		content.add(cardPanel, BorderLayout.CENTER);
		content.add(bottom, BorderLayout.SOUTH);

		kindBox.addAncestorListener(new FocusOnShow());
		return content;
	}

	private JPanel buildSkillCard()
	{
		skillBox.setMaximumRowCount(12);
		skillBox.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
			{
				Object text = value instanceof Skill ? ((Skill) value).getName() : value;
				return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
			}
		});

		JPanel card = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		card.add(new JLabel("Skill"));
		card.add(skillBox);
		card.add(currentSkillLabel);
		gainBox.setToolTipText("Off: reach a total amount of XP. On: gain this much XP starting from your XP now.");
		card.add(gainBox);
		card.add(targetLabel);
		card.add(targetField);
		return northAligned(card);
	}

	private JPanel buildKillCountCard()
	{
		JPanel options = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		options.add(kcNameLabel);
		options.add(kcNameField);
		options.add(new JLabel("How many? (e.g. 500 or 3k)"));
		options.add(kcTargetField);
		options.add(new JLabel("Current count (optional)"));
		options.add(kcCurrentField);
		options.add(kcCurrentHint);

		JPanel card = new JPanel(new BorderLayout(0, 6));
		card.add(kcPanel, BorderLayout.CENTER);
		card.add(options, BorderLayout.SOUTH);
		return card;
	}

	// Specialvalen först, sedan bossar, clues och aktiviteter som matchar söktexten.
	private void searchActivities(String query, Consumer<List<KcOption>> callback)
	{
		String needle = query.toLowerCase(Locale.ROOT);
		List<KcOption> options = new ArrayList<>();
		options.add(OTHER_NPC);
		options.add(MANUAL_COUNTER);
		List<KcOption> activities = new ArrayList<>();
		for (HiscoreSkill skill : HiscoreSkill.values())
		{
			if ((skill.getType() == HiscoreSkillType.BOSS || skill.getType() == HiscoreSkillType.ACTIVITY)
				&& skill.getName().toLowerCase(Locale.ROOT).contains(needle))
			{
				activities.add(new KcOption(KillCountRequirement.ActivityType.HISCORE, skill, skill.getName()));
			}
		}
		activities.sort(java.util.Comparator.comparing(KcOption::getLabel, String.CASE_INSENSITIVE_ORDER));
		options.addAll(activities);
		callback.accept(options);
	}

	// Ikonerna laddas en gång och listan ritas om när de finns.
	@Nullable
	private Icon activityIcon(KcOption option)
	{
		HiscoreSkill skill = option.getSkill();
		if (skill == null)
		{
			return null;
		}
		if (!activityIcons.containsKey(skill))
		{
			activityIcons.put(skill, null);
			services.getIcons().loadActivityImage(skill, image -> SwingUtilities.invokeLater(() ->
			{
				activityIcons.put(skill, new ImageIcon(image));
				kcPanel.repaintList();
			}));
		}
		return activityIcons.get(skill);
	}

	private void updateKillCountFields()
	{
		KcOption option = kcPanel.getSelected();
		boolean needsName = option != null && option.getType() != KillCountRequirement.ActivityType.HISCORE;
		kcNameLabel.setText(option != null && option.getType() == KillCountRequirement.ActivityType.MANUAL
			? "Counter name, e.g. Birdhouse runs"
			: "NPC name, e.g. Lizardman shaman");
		kcNameLabel.setVisible(needsName);
		kcNameField.setVisible(needsName);
		prefillCurrentCount();
	}

	// Nuvarande värde från räknarlagret (chatt, hiscores, loot eller eget värde) om det finns.
	private void prefillCurrentCount()
	{
		String key = selectedCounterKey();
		ActivityCounters.Counter counter = key == null ? null : services.counters().get(key);
		if (counter != null)
		{
			kcCurrentField.setText(String.valueOf(counter.getValue()));
			kcCurrentHint.setText(UiText.counterSource(counter) + ".");
			return;
		}
		kcCurrentField.setText("");
		KcOption option = kcPanel.getSelected();
		if (option == null)
		{
			kcCurrentHint.setText(" ");
		}
		else if (option.getType() == KillCountRequirement.ActivityType.HISCORE)
		{
			kcCurrentHint.setText("Fetched from the hiscores after you add the goal.");
		}
		else
		{
			kcCurrentHint.setText("Starts at 0 unless you enter a value.");
		}
	}

	@Nullable
	private String selectedCounterKey()
	{
		KcOption option = kcPanel.getSelected();
		if (option == null)
		{
			return null;
		}
		if (option.getSkill() != null)
		{
			return KillCountRequirement.counterKey(KillCountRequirement.ActivityType.HISCORE, option.getSkill().name());
		}
		String name = kcNameField.getText().trim();
		return name.isEmpty() ? null : KillCountRequirement.counterKey(option.getType(), name);
	}

	private JPanel buildAccountCard()
	{
		JPanel card = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		pointsBox.addActionListener(e -> accountCurrentLabel.setText(accountCurrentText(GoalKind.POINTS)));
		card.add(pointsBox);
		card.add(accountCurrentLabel);
		card.add(accountTargetLabel);
		card.add(accountTargetField);
		return northAligned(card);
	}

	private JPanel buildItemCard()
	{
		JPanel options = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		options.add(ownedLabel);
		options.add(new JLabel("How many? (e.g. 1, 500 or 10k)"));
		options.add(quantityField);
		options.add(variantsBox);
		options.add(new JLabel("Drops from (optional, shows how dry you are)"));
		options.add(dropBox);
		options.add(dropNameLabel);
		options.add(dropNameField);
		options.add(dropRateLabel);
		options.add(dropRateField);

		dropBox.addItem(NO_DROP_SOURCE);
		dropBox.addItem(OTHER_NPC);
		dropBox.addItem(MANUAL_COUNTER);
		List<KcOption> activities = new ArrayList<>();
		for (HiscoreSkill skill : HiscoreSkill.values())
		{
			if (skill.getType() == HiscoreSkillType.BOSS || skill.getType() == HiscoreSkillType.ACTIVITY)
			{
				activities.add(new KcOption(KillCountRequirement.ActivityType.HISCORE, skill, skill.getName()));
			}
		}
		activities.sort(Comparator.comparing(KcOption::getLabel, String.CASE_INSENSITIVE_ORDER));
		activities.forEach(dropBox::addItem);
		dropBox.setMaximumRowCount(16);
		dropBox.addActionListener(e -> updateDropFields());
		updateDropFields();

		JPanel card = new JPanel(new BorderLayout(0, 6));
		card.add(itemPanel, BorderLayout.CENTER);
		card.add(options, BorderLayout.SOUTH);
		return card;
	}

	private void prefillDropSource(@Nullable DropSource source)
	{
		if (source == null || !source.isValid())
		{
			dropBox.setSelectedItem(NO_DROP_SOURCE);
			updateDropFields();
			return;
		}
		HiscoreSkill skill = source.hiscoreSkill();
		if (skill != null)
		{
			dropBox.setSelectedItem(new KcOption(KillCountRequirement.ActivityType.HISCORE, skill, skill.getName()));
		}
		else
		{
			dropBox.setSelectedItem(source.getActivityType() == KillCountRequirement.ActivityType.NPC
				? OTHER_NPC : MANUAL_COUNTER);
			dropNameField.setText(source.getActivityName());
		}
		dropRateField.setText(String.valueOf(source.getRate()));
		dropPerDrop = source.getPerDrop();
		updateDropFields();
	}

	// Namnfältet behövs för NPC och egna räknare, raten för alla utom "None".
	private void updateDropFields()
	{
		KcOption option = (KcOption) dropBox.getSelectedItem();
		boolean any = option != null && option != NO_DROP_SOURCE;
		boolean needsName = any && option.getSkill() == null;
		dropNameLabel.setText(option == MANUAL_COUNTER ? "Counter name, e.g. Slayer tasks"
			: "NPC name, e.g. Lizardman shaman");
		dropNameLabel.setVisible(needsName);
		dropNameField.setVisible(needsName);
		dropRateLabel.setVisible(any);
		dropRateField.setVisible(any);
	}

	@Nullable
	private DropSource buildDropSource()
	{
		KcOption option = (KcOption) dropBox.getSelectedItem();
		if (option == null || option == NO_DROP_SOURCE)
		{
			return null;
		}
		String rateText = dropRateField.getText().trim();
		// "1/5000" och "5000" betyder samma sak.
		if (rateText.startsWith("1/"))
		{
			rateText = rateText.substring(2).trim();
		}
		long rate = parseAmount(rateText);
		if (rate < 1 || rate > DropSource.MAX_RATE)
		{
			throw new IllegalArgumentException("Enter the drop rate as the number after \"1 in\", e.g. 5000.");
		}
		DropSource source;
		if (option.getSkill() != null)
		{
			source = DropSource.hiscore(option.getSkill(), (int) rate);
		}
		else
		{
			source = option == OTHER_NPC ? DropSource.npc(dropNameField.getText(), (int) rate)
				: DropSource.manual(dropNameField.getText(), (int) rate);
		}
		return dropPerDrop == 1 ? source : source.withPerDrop(dropPerDrop);
	}

	private static JPanel northAligned(JPanel panel)
	{
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.add(panel, BorderLayout.NORTH);
		return wrapper;
	}

	private static JLabel hintLabel()
	{
		JLabel label = new JLabel(" ");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private void updateFields()
	{
		GoalKind kind = (GoalKind) kindBox.getSelectedItem();
		// Bara när ett nytt quest-mål skapas; ett befintligt mål ändras inte i efterhand.
		questChainBox.setVisible(kind == GoalKind.QUEST && editingGoalId == null);
		titleField.putClientProperty("JTextField.placeholderText", kind == GoalKind.MANUAL
			? "What do you want to do?"
			: "Optional - named after the goal if empty");
		titleField.repaint();
		if (kind != GoalKind.SKILL_XP)
		{
			targetLabel.setText("Target level (" + SkillRequirement.MIN_LEVEL + "-" + SkillRequirement.MAX_LEVEL + ")");
		}
		else if (gainBox.isSelected())
		{
			targetLabel.setText("XP to gain (e.g. 500k or 1.5m)");
		}
		else
		{
			targetLabel.setText("Target total XP (e.g. 13034431 or 13m)");
		}
		gainBox.setVisible(kind == GoalKind.SKILL_XP);

		if (kind == GoalKind.KILL_COUNT)
		{
			cards.show(cardPanel, KILL_COUNT_CARD);
			return;
		}

		if (kind.isAccountLevel())
		{
			pointsBox.setVisible(kind == GoalKind.POINTS);
			int[] range = accountRange(kind);
			accountTargetLabel.setText("Target " + kind.toString().toLowerCase(Locale.ROOT)
				+ " (" + range[0] + "-" + range[1] + ")");
			accountCurrentLabel.setText(accountCurrentText(kind));
			cards.show(cardPanel, ACCOUNT_CARD);
			return;
		}

		// Level och XP delar samma kort.
		GoalKind card = kind == GoalKind.SKILL_XP ? GoalKind.SKILL_LEVEL : kind;
		cards.show(cardPanel, card.name());
	}

	private static int[] accountRange(GoalKind kind)
	{
		switch (kind)
		{
			case TOTAL_LEVEL:
				return new int[]{TotalLevelRequirement.MIN, TotalLevelRequirement.MAX};
			case COMBAT_LEVEL:
				return new int[]{CombatLevelRequirement.MIN, CombatLevelRequirement.MAX};
			case POINTS:
				return new int[]{PointsRequirement.MIN, PointsRequirement.MAX};
			default:
				return new int[]{QuestPointsRequirement.MIN, QuestPointsRequirement.MAX};
		}
	}

	private Requirement accountRequirement(GoalKind kind, int target)
	{
		switch (kind)
		{
			case TOTAL_LEVEL:
				return new TotalLevelRequirement(target);
			case COMBAT_LEVEL:
				return new CombatLevelRequirement(target);
			case POINTS:
				return new PointsRequirement((PointsActivity) pointsBox.getSelectedItem(), target);
			default:
				return new QuestPointsRequirement(target);
		}
	}

	// Nuvarande värde räknas på samma sätt som kravet, med lägsta målet som sond.
	private String accountCurrentText(GoalKind kind)
	{
		GoalProgress progress = accountRequirement(kind, accountRange(kind)[0]).progress(services.currentGameData());
		if (progress == null)
		{
			if (kind == GoalKind.POINTS)
			{
				// Poängen läses först när ett mål bevakar dem, så de är okända innan målet finns.
				return "Your current points are read from the game once the goal is added.";
			}
			return "Log in to see your current " + kind.toString().toLowerCase(Locale.ROOT) + ".";
		}
		return "You have " + QuantityFormatter.formatNumber(progress.getCurrent()) + " now.";
	}

	private void updateCurrentSkill()
	{
		Skill skill = (Skill) skillBox.getSelectedItem();
		long xp = services.currentGameData().getXp(skill);
		if (xp < 0)
		{
			currentSkillLabel.setText("Log in to see your current level.");
			return;
		}
		int level = Experience.getLevelForXp((int) Math.min(xp, SkillRequirement.MAX_XP));
		currentSkillLabel.setText("You are level " + level + " (" + QuantityFormatter.formatNumber(xp) + " XP).");
	}

	private void updateOwned()
	{
		ItemSearch.ItemOption item = itemPanel.getSelected();
		if (item == null)
		{
			ownedLabel.setText(" ");
			return;
		}
		long owned = services.currentGameData().getItemCount(item.getId());
		ownedLabel.setText(owned < 0 ? " " : "You have " + QuantityFormatter.formatNumber(owned) + " right now.");
	}

	// Ikonerna cachas så att listan bara ritas om en gång per föremål när bilden laddats.
	private Icon itemIcon(int itemId)
	{
		return itemIcons.computeIfAbsent(itemId, id ->
		{
			AsyncBufferedImage image = services.getItemSearch().getImage(id);
			image.onLoaded(itemPanel::repaintList);
			return new ImageIcon(image);
		});
	}

	private Result toResult()
	{
		GoalKind kind = (GoalKind) kindBox.getSelectedItem();
		String title = titleField.getText().trim();
		String note = noteArea.getText();
		Requirement requirement = buildRequirement(kind, title);

		if (requirement != null && services.isDuplicate(requirement, editingGoalId))
		{
			throw new IllegalArgumentException("You already have this goal in your list.");
		}
		return new Result(title, note, requirement, aggregateBox.isSelected(), startCount(requirement),
			questChainBox.isVisible() && questChainBox.isSelected(),
			requirement instanceof ItemRequirement ? buildDropSource() : null);
	}

	// Ett angivet nuvarande värde som skiljer från räknarlagret, annars null.
	@Nullable
	private Long startCount(@Nullable Requirement requirement)
	{
		String text = kcCurrentField.getText().trim();
		if (!(requirement instanceof KillCountRequirement) || text.isEmpty())
		{
			return null;
		}
		long value = parseAmount(text);
		if (value < 0)
		{
			throw new IllegalArgumentException("Enter a current count of 0 or more, or leave it empty.");
		}
		String key = ((KillCountRequirement) requirement).counterKey();
		return value == services.counters().value(key) ? null : value;
	}

	@Nullable
	private Requirement buildRequirement(GoalKind kind, String title)
	{
		switch (kind)
		{
			case SKILL_LEVEL:
			{
				Skill skill = (Skill) skillBox.getSelectedItem();
				SkillRequirement requirement = SkillRequirement.level(skill, parseLevel(targetField.getText().trim()));
				rejectIfReached(requirement, "You already have level " + requirement.getTarget() + " " + skill.getName() + ".");
				return requirement;
			}
			case SKILL_XP:
			{
				Skill skill = (Skill) skillBox.getSelectedItem();
				long amount = parseXp(targetField.getText().trim());
				if (gainBox.isSelected())
				{
					return buildGain(skill, amount);
				}
				SkillRequirement requirement = SkillRequirement.xp(skill, amount);
				rejectIfReached(requirement, "You already have that much " + skill.getName() + " XP.");
				return requirement;
			}
			case ITEM:
			{
				ItemSearch.ItemOption item = requireSelected(itemPanel, "Search for an item and pick it in the list.");
				ItemRequirement requirement = new ItemRequirement(item.getId(), item.getName(),
					parseQuantity(quantityField.getText().trim()), variantsBox.isSelected());
				// Motsvarigheter från en template behålls när samma föremål redigeras.
				if (originalRequirement instanceof ItemRequirement
					&& ((ItemRequirement) originalRequirement).getItemId() == item.getId())
				{
					requirement = requirement.withCountsAlso(((ItemRequirement) originalRequirement).getCountsAlso());
				}
				return requirement;
			}
			case QUEST:
			{
				QuestOption quest = requireSelected(questPanel, "Pick a quest in the list.");
				return new QuestRequirement(quest.getId(), quest.getName());
			}
			case DIARY:
			{
				DiaryOption diary = requireSelected(diaryPanel, "Pick a diary in the list.");
				return new DiaryRequirement(diary.getArea(), diary.getTier());
			}
			case TOTAL_LEVEL:
			case COMBAT_LEVEL:
			case QUEST_POINTS:
			case POINTS:
			{
				// Ett redan uppnått mål är tillåtet; det bockas av med ungefärligt datum.
				int[] range = accountRange(kind);
				return accountRequirement(kind, parseInRange(accountTargetField.getText().trim(), range[0], range[1],
					kind.toString().toLowerCase(Locale.ROOT)));
			}
			case KILL_COUNT:
			{
				KcOption option = requireSelected(kcPanel, "Pick a boss or activity, \"Other NPC\" or \"Manual counter\".");
				int target = parseInRange(kcTargetField.getText().trim(), 1, KillCountRequirement.MAX_TARGET, "target");
				switch (option.getType())
				{
					case NPC:
						return KillCountRequirement.npc(kcNameField.getText(), target);
					case MANUAL:
						return KillCountRequirement.manual(kcNameField.getText(), target);
					default:
						return KillCountRequirement.hiscore(option.getSkill(), target);
				}
			}
			case COMBAT_ACHIEVEMENT:
			{
				CombatAchievementCatalog.Task task = requireSelected(caPanel, "Pick a task in the list.");
				return new CombatAchievementRequirement(task.getId(), task.getName(), task.getTier());
			}
			case MANUAL:
			default:
				if (title.isEmpty())
				{
					throw new IllegalArgumentException("Give the goal a title, e.g. \"Get a Fire cape\".");
				}
				return null;
		}
	}

	private SkillRequirement buildGain(Skill skill, long gain)
	{
		// Vid redigering behålls startpunkten om det redan var ett XP-gain-mål i samma skill,
		// annars räknas det från spelarens XP just nu.
		long startXp;
		if (originalRequirement instanceof SkillRequirement
			&& ((SkillRequirement) originalRequirement).getMode() == SkillRequirement.Mode.XP_GAIN
			&& ((SkillRequirement) originalRequirement).getSkill() == skill)
		{
			startXp = ((SkillRequirement) originalRequirement).getStartXp();
		}
		else
		{
			startXp = services.currentGameData().getXp(skill);
			if (startXp < 0)
			{
				throw new IllegalArgumentException("Log in first, so the goal can start from your current "
					+ skill.getName() + " XP.");
			}
		}

		if (startXp + gain > SkillRequirement.MAX_XP)
		{
			throw new IllegalArgumentException("You can gain at most "
				+ QuantityFormatter.formatNumber(SkillRequirement.MAX_XP - startXp) + " more " + skill.getName() + " XP.");
		}
		SkillRequirement requirement = SkillRequirement.xpGain(skill, startXp, gain);
		rejectIfReached(requirement, "You have already gained that much " + skill.getName() + " XP.");
		return requirement;
	}

	// Ett skill-mål som redan är nått vore klart direkt, så det stoppas med en vänlig förklaring.
	private void rejectIfReached(SkillRequirement requirement, String message)
	{
		if (requirement.equals(originalRequirement))
		{
			return;
		}
		GameData data = services.currentGameData();
		if (data.getXp(requirement.getSkill()) >= requirement.targetXp())
		{
			throw new IllegalArgumentException(message);
		}
	}

	private static <T> T requireSelected(SearchPanel<T> panel, String message)
	{
		T selected = panel.getSelected();
		if (selected == null)
		{
			throw new IllegalArgumentException(message);
		}
		return selected;
	}

	private void searchQuests(String query, Consumer<List<QuestOption>> callback)
	{
		callback.accept(filterQuests(status, query));
	}

	private void searchDiaries(String query, Consumer<List<DiaryOption>> callback)
	{
		callback.accept(filterDiaries(status, query));
	}

	private void searchCombatAchievements(String query, Consumer<List<CombatAchievementCatalog.Task>> callback)
	{
		CompletionStatus current = status;
		services.getCombatAchievements().search(query, MAX_LIST_RESULTS, tasks ->
		{
			List<CombatAchievementCatalog.Task> remaining = new ArrayList<>();
			for (CombatAchievementCatalog.Task task : tasks)
			{
				if (!current.isAlreadyComplete(new CombatAchievementRequirement(task.getId(), task.getName(), task.getTier())))
				{
					remaining.add(task);
				}
			}
			callback.accept(remaining);
		});
	}

	/**
	 * Quests som inte är klara, i bokstavsordning (påbörjade markeras).
	 */
	static List<QuestOption> filterQuests(CompletionStatus status, String query)
	{
		List<QuestOption> all = new ArrayList<>();
		AchievementTracker.questsById().values().stream()
			.sorted(Comparator.comparing(net.runelite.api.Quest::getName))
			.forEach(quest ->
			{
				QuestState state = status.getQuestState(quest.getId());
				if (state != QuestState.FINISHED)
				{
					all.add(new QuestOption(quest.getId(), quest.getName(), state == QuestState.IN_PROGRESS));
				}
			});
		return SearchRanking.rank(all, QuestOption::getName, query, MAX_LIST_RESULTS);
	}

	/**
	 * Diary-nivåer som inte är klara, område för område.
	 */
	static List<DiaryOption> filterDiaries(CompletionStatus status, String query)
	{
		List<DiaryOption> all = new ArrayList<>();
		for (DiaryArea area : DiaryArea.values())
		{
			for (DiaryTier tier : DiaryTier.values())
			{
				if (!status.isAlreadyComplete(new DiaryRequirement(area, tier)))
				{
					all.add(new DiaryOption(area, tier));
				}
			}
		}
		return SearchRanking.rank(all, DiaryOption::toString, query, MAX_LIST_RESULTS);
	}

	static int parseInRange(String text, int min, int max, String what)
	{
		try
		{
			long value = parseAmount(text);
			if (value >= min && value <= max)
			{
				return (int) value;
			}
		}
		catch (IllegalArgumentException e)
		{
			// Faller igenom till meddelandet nedan.
		}
		throw new IllegalArgumentException("Enter a " + what + " between " + min + " and " + max + ".");
	}

	static int parseLevel(String text)
	{
		try
		{
			int level = Integer.parseInt(text);
			if (level >= SkillRequirement.MIN_LEVEL && level <= SkillRequirement.MAX_LEVEL)
			{
				return level;
			}
		}
		catch (NumberFormatException e)
		{
			// Faller igenom till felmeddelandet nedan.
		}
		throw new IllegalArgumentException("Enter a level between " + SkillRequirement.MIN_LEVEL
			+ " and " + SkillRequirement.MAX_LEVEL + ".");
	}

	static long parseXp(String text)
	{
		long xp = parseAmount(text);
		if (xp >= 1 && xp <= SkillRequirement.MAX_XP)
		{
			return xp;
		}
		throw new IllegalArgumentException("Enter an XP amount between 1 and 200m.");
	}

	static long parseQuantity(String text)
	{
		long quantity = parseAmount(text);
		if (quantity >= 1 && quantity <= ItemRequirement.MAX_QUANTITY)
		{
			return quantity;
		}
		throw new IllegalArgumentException("Enter a quantity between 1 and "
			+ QuantityFormatter.formatNumber(ItemRequirement.MAX_QUANTITY) + ".");
	}

	// Klientens parser förstår både "13,034,431" och förkortningar som "13m"; -1 vid fel.
	private static long parseAmount(String text)
	{
		try
		{
			return QuantityFormatter.parseQuantity(text.replace(",", "").replace(" ", ""));
		}
		catch (ParseException | NumberFormatException e)
		{
			return -1;
		}
	}

	private static Skill[] trackableSkills()
	{
		List<Skill> skills = new ArrayList<>();
		for (Skill skill : Skill.values())
		{
			if (SkillTracker.isTrackable(skill))
			{
				skills.add(skill);
			}
		}
		return skills.toArray(new Skill[0]);
	}

	private static final class FocusOnShow implements AncestorListener
	{
		@Override
		public void ancestorAdded(AncestorEvent event)
		{
			event.getComponent().requestFocusInWindow();
		}

		@Override
		public void ancestorRemoved(AncestorEvent event)
		{
		}

		@Override
		public void ancestorMoved(AncestorEvent event)
		{
		}
	}
}
