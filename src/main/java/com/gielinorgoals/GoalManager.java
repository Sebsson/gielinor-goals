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
package com.gielinorgoals;

import com.gielinorgoals.model.ActivityCounters;
import com.gielinorgoals.model.CountedGameData;
import com.gielinorgoals.model.DiaryJournal;
import com.gielinorgoals.model.DiaryRequirement;
import com.gielinorgoals.model.GameData;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalEvaluator;
import com.gielinorgoals.model.GoalProgress;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.model.KillCountRequirement;
import com.gielinorgoals.model.Requirement;
import com.gielinorgoals.persistence.GoalSerializer;
import com.gielinorgoals.persistence.LoadResult;
import com.gielinorgoals.persistence.SchemaMigrator;
import com.gielinorgoals.template.TemplateInserter;
import com.gielinorgoals.template.TemplateNode;
import com.gielinorgoals.tracking.InitialSync;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Äger målträdet för den aktiva RuneScape-profilen, utvärderar automatiska mål mot
 * senaste spelläget och sparar varje ändring via ConfigManager. Alla metoder anropas på EDT.
 */
@Slf4j
@Singleton
public class GoalManager
{
	static final String GOALS_KEY = "goals";
	static final String CORRUPT_BACKUP_KEY = "goalsCorruptBackup";
	// Kopia av datan som den såg ut före migrering till version N.
	static final String MIGRATION_BACKUP_KEY_PREFIX = "goalsBackupBeforeV";

	/**
	 * Var målen sparas per profil. I pluginet är det ConfigManager; tester använder en karta.
	 */
	interface Storage
	{
		@Nullable
		String get(String profileKey, String key);

		void set(String profileKey, String key, String value);
	}

	private final Storage storage;
	private final GoalSerializer serializer;
	private final InitialSync initialSync;

	/**
	 * Räknas upp varje gång målen sparas eller en profil laddas, men inte när bara
	 * progressen ändras. Vyer som bara bryr sig om målen (t.ex. Completed-fliken)
	 * kan jämföra den för att slippa bygga om vid varje tick.
	 */
	@Getter
	private long revision;

	// Profilen fångas vid laddning så att ändringar efter utloggning hamnar rätt.
	@Getter
	@Nullable
	private String profileKey;
	@Getter
	private GoalTree tree = new GoalTree();
	// Nuvarande värden per aktivitet för den laddade profilen; sparas tillsammans med målen.
	@Getter
	private ActivityCounters counters = new ActivityCounters();
	@Getter
	private LoadResult.Status loadStatus = LoadResult.Status.OK;

	// Senaste spelläget och vilken profil det hör till.
	private GameData gameData = GameData.EMPTY;
	@Nullable
	private String gameDataProfileKey;
	private Map<String, GoalProgress> progress = Collections.emptyMap();

	private Runnable listener = () ->
	{
	};
	private Consumer<List<Goal>> completionListener = goals ->
	{
	};

	@Inject
	GoalManager(ConfigManager configManager, Gson gson, InitialSync initialSync)
	{
		this(new Storage()
		{
			@Override
			public String get(String profileKey, String key)
			{
				return configManager.getConfiguration(GielinorGoalsConfig.GROUP, profileKey, key);
			}

			@Override
			public void set(String profileKey, String key, String value)
			{
				configManager.setConfiguration(GielinorGoalsConfig.GROUP, profileKey, key, value);
			}
		}, gson, initialSync);
	}

	GoalManager(Storage storage, Gson gson, InitialSync initialSync)
	{
		this.storage = storage;
		this.serializer = new GoalSerializer(gson, new SchemaMigrator());
		this.initialSync = initialSync;
	}

	public void setListener(Runnable listener)
	{
		this.listener = listener;
	}

	/**
	 * Anropas med mål som bockats av automatiskt när spelläget ändrats.
	 */
	public void setCompletionListener(Consumer<List<Goal>> completionListener)
	{
		this.completionListener = completionListener;
	}

	public boolean hasProfile()
	{
		return profileKey != null;
	}

	public boolean isEditable()
	{
		return hasProfile() && loadStatus != LoadResult.Status.NEWER_VERSION;
	}

	@Nullable
	public GoalProgress getProgress(String goalId)
	{
		return progress.get(goalId);
	}

	/**
	 * @return när banken senast sågs för den laddade profilen, eller null om det är okänt
	 */
	@Nullable
	public Long getBankSeenAt()
	{
		if (!hasProfile() || !Objects.equals(profileKey, gameDataProfileKey))
		{
			return null;
		}
		return gameData.getBankSeenAt();
	}

	/**
	 * Senaste spelläget om det hör till den laddade profilen, annars ett tomt läge.
	 */
	public GameData getCurrentGameData()
	{
		return hasGameData() ? gameData : GameData.EMPTY;
	}

	/**
	 * @param exceptGoalId mål som inte räknas, t.ex. det som håller på att redigeras
	 */
	public boolean hasGoalWithRequirement(Requirement requirement, @Nullable String exceptGoalId)
	{
		for (Goal goal : tree.flatten())
		{
			if (!goal.getId().equals(exceptGoalId) && requirement.equals(goal.getRequirement()))
			{
				return true;
			}
		}
		return false;
	}

	public boolean hasGameData()
	{
		return hasProfile() && Objects.equals(profileKey, gameDataProfileKey);
	}

	public void loadProfile(@Nullable String newProfileKey)
	{
		profileKey = newProfileKey;
		if (newProfileKey == null)
		{
			unload();
			return;
		}

		String json = storage.get(newProfileKey, GOALS_KEY);
		LoadResult result = serializer.deserialize(json);
		loadStatus = result.getStatus();
		tree = result.getTree();
		counters = result.getCounters();

		if (result.getStatus() == LoadResult.Status.CORRUPT)
		{
			// Rådatan sparas undan innan något kan skriva över den.
			log.warn("Could not read saved goals, keeping a backup");
			storage.set(newProfileKey, CORRUPT_BACKUP_KEY, json);
		}
		else if (result.isMigrated())
		{
			storage.set(newProfileKey,
				MIGRATION_BACKUP_KEY_PREFIX + SchemaMigrator.CURRENT_VERSION, json);
			save();
		}

		// Mål som nåtts medan pluginet var avstängt bockas av tyst, med ungefärligt datum.
		if (markApprox(evaluate()))
		{
			save();
		}
		revision++;
		listener.run();
	}

	public void unload()
	{
		counters = new ActivityCounters();
		profileKey = null;
		tree = new GoalTree();
		loadStatus = LoadResult.Status.OK;
		progress = Collections.emptyMap();
		revision++;
		listener.run();
	}

	/**
	 * Nytt spelläge från trackers. Data från en annan profil än den laddade ignoreras.
	 */
	public void updateGameData(@Nullable String dataProfileKey, GameData data)
	{
		gameDataProfileKey = dataProfileKey;
		gameData = data;

		List<Goal> completed = evaluate();
		if (!completed.isEmpty())
		{
			// I första synken upptäcks mål som klarades offline; då är datumet ungefärligt.
			if (initialSync.isActive())
			{
				markApprox(completed);
			}
			save();
			completionListener.accept(completed);
		}
		listener.run();
	}

	/**
	 * Spelets eget räknemeddelande i chatten. Värdet är absolut och vinner alltid.
	 *
	 * @param dataProfileKey profilen när meddelandet kom; ignoreras om en annan profil är laddad
	 */
	public void recordChatCount(@Nullable String dataProfileKey, String counterKey, long value)
	{
		changeCounters(dataProfileKey, counters -> counters.applyChat(counterKey, value, System.currentTimeMillis()));
	}

	/**
	 * Värden från hiscores. Används bara där inget värde finns eller där hiscores är högre.
	 */
	public void recordHiscores(@Nullable String dataProfileKey, Map<String, Long> values)
	{
		changeCounters(dataProfileKey, counters ->
		{
			long now = System.currentTimeMillis();
			boolean changed = false;
			for (Map.Entry<String, Long> entry : values.entrySet())
			{
				changed |= counters.applyHiscore(entry.getKey(), entry.getValue(), now);
			}
			return changed;
		});
	}

	/**
	 * En NPC dog och gav loot. Räknas bara om något mål följer just den NPC:n.
	 */
	public void recordNpcKill(@Nullable String dataProfileKey, @Nullable String npcName)
	{
		if (npcName == null)
		{
			return;
		}
		String key = KillCountRequirement.counterKey(KillCountRequirement.ActivityType.NPC, npcName);
		if (!watchedCounterKeys().contains(key))
		{
			return;
		}
		changeCounters(dataProfileKey, counters ->
		{
			counters.addLoot(key, System.currentTimeMillis());
			return true;
		});
	}

	/**
	 * Användaren anger ett värde själv ("Set value...").
	 */
	public void setCounter(String counterKey, long value)
	{
		changeCounters(profileKey, counters -> counters.setManual(counterKey, value, System.currentTimeMillis()));
	}

	/**
	 * +1/−1 från användaren.
	 */
	public void adjustCounter(String counterKey, long delta)
	{
		changeCounters(profileKey, counters -> counters.adjustManual(counterKey, delta, System.currentTimeMillis()));
	}

	/**
	 * Räknarnycklar som något KC-mål följer.
	 */
	public Set<String> watchedCounterKeys()
	{
		Set<String> keys = new HashSet<>();
		for (Goal goal : tree.flatten())
		{
			if (goal.getRequirement() instanceof KillCountRequirement)
			{
				keys.add(((KillCountRequirement) goal.getRequirement()).counterKey());
			}
		}
		return keys;
	}

	// Ändrar räknarna och kör sedan samma väg som ett nytt spelläge: utvärdering, popup
	// för mål som klarades (tyst med ungefärligt datum under första synken) och sparning.
	private void changeCounters(@Nullable String dataProfileKey, Predicate<ActivityCounters> change)
	{
		if (!isEditable() || !Objects.equals(profileKey, dataProfileKey) || !change.test(counters))
		{
			return;
		}

		List<Goal> completed = evaluate();
		if (!completed.isEmpty() && initialSync.isActive())
		{
			markApprox(completed);
		}
		save();
		if (!completed.isEmpty())
		{
			completionListener.accept(completed);
		}
		listener.run();
	}

	/**
	 * Lägger in uppgifterna från en öppnad diary-journal som delmål under de diary-mål
	 * som gäller samma område, och bockar av uppgifter som är överstrukna i journalen.
	 * Befintliga delmål matchas på text, så egna ändringar och manuella bockar finns kvar.
	 *
	 * @return meddelanden att visa i chatten (tomt om inget hände)
	 */
	public List<String> applyDiaryJournal(DiaryJournal journal)
	{
		List<String> messages = new ArrayList<>();
		if (!isEditable())
		{
			return messages;
		}

		long now = System.currentTimeMillis();
		boolean changed = false;
		for (Goal goal : tree.flatten())
		{
			if (!(goal.getRequirement() instanceof DiaryRequirement) || goal.isCompleted())
			{
				continue;
			}
			DiaryRequirement diary = (DiaryRequirement) goal.getRequirement();
			if (diary.getArea() != journal.getArea())
			{
				continue;
			}

			Integer mismatched = journal.getMismatchedTiers().get(diary.getTier());
			if (mismatched != null)
			{
				messages.add("Could not read the " + diary.describe() + " tasks (found " + mismatched + " of "
					+ diary.getArea().taskTotalFor(diary.getTier()) + ").");
				continue;
			}

			int added = 0;
			int ticked = 0;
			for (DiaryJournal.Task task : journal.tasks(diary.getTier()))
			{
				Goal child = findChildByTitle(goal, task.getText());
				if (child == null)
				{
					child = new Goal(task.getText());
					// Journalen säger bara att uppgiften är gjord, inte när.
					if (task.isDone())
					{
						child.markCompleted(true, now, true);
					}
					tree.add(goal.getId(), child);
					added++;
				}
				else if (task.isDone() && !child.isCompleted())
				{
					child.markCompleted(true, now, true);
					ticked++;
				}
			}

			if (added > 0)
			{
				messages.add("Added " + added + " tasks to " + goal.getDisplayTitle() + ".");
			}
			if (ticked > 0)
			{
				messages.add("Ticked off " + ticked + " " + diary.describe() + " tasks.");
			}
			changed |= added > 0 || ticked > 0;
		}

		if (changed)
		{
			markApprox(evaluate());
			save();
			listener.run();
		}
		return messages;
	}

	@Nullable
	private Goal findChildByTitle(Goal parent, String title)
	{
		for (Goal child : tree.getChildren(parent))
		{
			if (title.equalsIgnoreCase(child.getTitle()))
			{
				return child;
			}
		}
		return null;
	}

	public void addGoal(@Nullable String parentId, String title, @Nullable String note, @Nullable Requirement requirement)
	{
		addGoal(parentId, title, note, requirement, false);
	}

	/**
	 * @param aggregate visa målet som räknare över sina delmål ("X/Y")
	 */
	public void addGoal(@Nullable String parentId, String title, @Nullable String note, @Nullable Requirement requirement,
		boolean aggregate)
	{
		String trimmed = title.trim();
		if (trimmed.isEmpty() && requirement == null)
		{
			return;
		}

		Goal goal = new Goal(trimmed);
		goal.setNote(emptyToNull(note));
		goal.setRequirement(requirement);
		goal.setAggregate(aggregate);
		apply(tree -> tree.add(parentId, goal));
	}

	public void editGoal(String id, String title, @Nullable String note, @Nullable Requirement requirement)
	{
		Goal goal = tree.find(id);
		editGoal(id, title, note, requirement, goal != null && goal.isAggregate());
	}

	public void editGoal(String id, String title, @Nullable String note, @Nullable Requirement requirement,
		boolean aggregate)
	{
		String trimmed = title.trim();
		if (trimmed.isEmpty() && requirement == null)
		{
			return;
		}

		apply(tree ->
		{
			Goal goal = tree.find(id);
			if (goal == null)
			{
				return false;
			}

			// Ett nytt krav kan vara olöst, så då börjar målet om som ej klart.
			if (!Objects.equals(goal.getRequirement(), requirement) && requirement != null)
			{
				goal.markCompleted(false, 0L);
			}
			goal.setTitle(trimmed);
			goal.setNote(emptyToNull(note));
			goal.setRequirement(requirement);
			applyAggregate(goal, aggregate);
			return true;
		});
	}

	/**
	 * Slår på eller av räknarvisningen. Påslagen fälls målet ihop, eftersom poängen är
	 * att se "142/170" i stället för alla delmål.
	 */
	public void setAggregate(String id, boolean aggregate)
	{
		apply(tree ->
		{
			Goal goal = tree.find(id);
			if (goal == null || goal.isAggregate() == aggregate)
			{
				return false;
			}
			applyAggregate(goal, aggregate);
			return true;
		});
	}

	private static void applyAggregate(Goal goal, boolean aggregate)
	{
		if (aggregate && !goal.isAggregate())
		{
			goal.setExpanded(false);
		}
		goal.setAggregate(aggregate);
	}

	/**
	 * Kopplar ett befintligt mål under ytterligare en förälder. Vägrar om det skulle bli en cykel.
	 */
	public void link(String parentId, String childId)
	{
		apply(tree -> tree.link(parentId, childId));
	}

	/**
	 * Tar bort kopplingen till en förälder; utan föräldrar kvar blir målet ett rotmål.
	 */
	public void unlink(String parentId, String childId)
	{
		apply(tree -> tree.unlink(parentId, childId));
	}

	public void removeGoal(String id)
	{
		apply(tree -> tree.remove(id));
	}

	public void setCompleted(String id, boolean completed)
	{
		apply(tree -> tree.setCompleted(id, completed, System.currentTimeMillis()));
	}

	/**
	 * Fäller ihop alla mål som har delmål, i en ändring och en sparning.
	 */
	public void collapseAll()
	{
		apply(tree ->
		{
			boolean changed = false;
			for (Goal goal : tree.flatten())
			{
				if (goal.hasChildren() && goal.isExpanded())
				{
					goal.setExpanded(false);
					changed = true;
				}
			}
			return changed;
		});
	}

	public void toggleExpanded(String id)
	{
		apply(tree ->
		{
			Goal goal = tree.find(id);
			if (goal == null)
			{
				return false;
			}
			goal.setExpanded(!goal.isExpanded());
			return true;
		});
	}

	/**
	 * Sätter in ett template-förslag som ett nytt rotmål, med befintliga mål med samma
	 * krav inkopplade i stället för dubbletter. Allt sker i en operation: en utvärdering,
	 * en sparning och en ombyggnad av panelen. Mål som redan är uppfyllda bockas av tyst
	 * med ungefärligt datum, som när ett redan uppfyllt mål läggs till, och ger ingen popup.
	 *
	 * @return vad som skapades och kopplades in, eller null om målen inte går att ändra
	 */
	@Nullable
	public TemplateInserter.Result insertTemplate(TemplateNode template)
	{
		if (!isEditable())
		{
			return null;
		}

		TemplateInserter.Result result = TemplateInserter.insert(tree, template);
		// Ingen completionListener här: inga popups eller chattmeddelanden för en insättning.
		markApprox(evaluate());
		save();
		listener.run();
		return result;
	}

	/**
	 * Fäller ut alla förfäder så att målet syns i trädet.
	 *
	 * @return false om målet inte finns
	 */
	public boolean reveal(String id)
	{
		if (tree.find(id) == null)
		{
			return false;
		}
		apply(tree -> expandAncestors(tree, id));
		return true;
	}

	private static boolean expandAncestors(GoalTree tree, String id)
	{
		boolean changed = false;
		for (Goal parent : tree.getParents(id))
		{
			if (!parent.isExpanded())
			{
				parent.setExpanded(true);
				changed = true;
			}
			changed |= expandAncestors(tree, parent.getId());
		}
		return changed;
	}

	// Flyttningarna gäller bara kopplingen i föräldern raden visas under (null = rotnivå).

	public void moveUp(@Nullable String parentId, String id)
	{
		apply(tree -> tree.moveUp(parentId, id));
	}

	public void moveDown(@Nullable String parentId, String id)
	{
		apply(tree -> tree.moveDown(parentId, id));
	}

	public void indent(@Nullable String parentId, String id)
	{
		apply(tree -> tree.indent(parentId, id));
	}

	public void outdent(@Nullable String grandparentId, String parentId, String id)
	{
		apply(tree -> tree.outdent(grandparentId, parentId, id));
	}

	// Kör en ändring, och sparar och uppdaterar UI:t bara om något faktiskt ändrades.
	// Mål som blir klara direkt av användarens ändring ger inget chattmeddelande.
	private void apply(Predicate<GoalTree> change)
	{
		if (!isEditable())
		{
			return;
		}

		if (change.test(tree))
		{
			// Ett mål med krav som blir klart direkt av ändringen var redan uppfyllt när det
			// lades till, så datumet är ungefärligt. Ett räknarmål som blir klart för att
			// användaren bockade av sista delmålet klarades däremot just nu, och får popup.
			List<Goal> completed = evaluate();
			List<Goal> counters = new ArrayList<>();
			List<Goal> others = new ArrayList<>();
			for (Goal goal : completed)
			{
				(goal.isAggregate() && goal.getRequirement() == null ? counters : others).add(goal);
			}
			markApprox(others);
			save();
			if (!counters.isEmpty())
			{
				completionListener.accept(counters);
			}
			listener.run();
		}
	}

	/**
	 * Utvärderar automatiska mål mot senaste spelläget, om det hör till aktuell profil.
	 *
	 * @return mål som just bockades av
	 */
	private List<Goal> evaluate()
	{
		if (!isEditable())
		{
			progress = Collections.emptyMap();
			return Collections.emptyList();
		}

		// Utan spelläge för profilen ger kraven ingen progress, men räknarmål räknas ändå,
		// eftersom de bara beror på delmålen.
		GameData data = Objects.equals(profileKey, gameDataProfileKey) ? gameData : GameData.EMPTY;
		// Räknarna hör till profilen och finns även utan spelläge.
		GoalEvaluator.Result result = GoalEvaluator.evaluate(tree, new CountedGameData(data, counters),
			System.currentTimeMillis());
		progress = result.getProgress();
		return result.getNewlyCompleted();
	}

	/**
	 * Markerar datumet som ungefärligt på mål som just bockades av.
	 *
	 * @return true om listan inte var tom
	 */
	private static boolean markApprox(List<Goal> completed)
	{
		for (Goal goal : completed)
		{
			goal.setCompletedAtApprox(true);
		}
		return !completed.isEmpty();
	}

	private void save()
	{
		if (!isEditable())
		{
			return;
		}

		// ConfigManager håller värdet i minnet och skriver till disk själv i bakgrunden.
		storage.set(profileKey, GOALS_KEY,
			serializer.serialize(tree, counters));
		revision++;
		// En lyckad sparning ersätter korrupt data, så varningen behövs inte längre.
		loadStatus = LoadResult.Status.OK;
	}

	@Nullable
	private static String emptyToNull(@Nullable String value)
	{
		if (value == null)
		{
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
