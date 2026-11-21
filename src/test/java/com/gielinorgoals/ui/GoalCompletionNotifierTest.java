package com.gielinorgoals.ui;

import com.gielinorgoals.CompletionSound;
import com.gielinorgoals.GielinorGoalsConfig;
import com.gielinorgoals.PopupLevel;
import com.gielinorgoals.model.Goal;
import com.gielinorgoals.model.GoalTree;
import com.gielinorgoals.tracking.InitialSync;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class GoalCompletionNotifierTest
{
	private long now = 1_000_000L;
	private PopupLevel level = PopupLevel.ALL;
	private boolean sound = true;

	private final GielinorGoalsConfig config = new GielinorGoalsConfig()
	{
		@Override
		public PopupLevel popupLevel()
		{
			return level;
		}

		@Override
		public boolean soundEnabled()
		{
			return sound;
		}
	};

	private NotificationQueue queue;
	private InitialSync initialSync;
	private GoalCompletionNotifier notifier;
	private GoalTree tree;
	private Goal diary;
	private Goal task;

	@Before
	public void setUp()
	{
		queue = new NotificationQueue();
		initialSync = new InitialSync(() -> now);
		notifier = new GoalCompletionNotifier(config, queue, (requirement, consumer) ->
		{
		}, initialSync);
		tree = new GoalTree();
		diary = new Goal("Varrock Easy");
		task = new Goal("Mine some iron");
		tree.add(null, diary);
		tree.add(diary.getId(), task);
	}

	@Test
	public void firstSyncAfterLoginIsQuiet()
	{
		initialSync.start();
		task.markCompleted(true, now);

		notifier.onAutoCompleted(tree, List.of(task));

		assertNull(queue.current(now));
	}

	@Test
	public void popupsResumeAfterQuietPeriod()
	{
		initialSync.start();
		now += InitialSync.WINDOW_MS;
		task.markCompleted(true, now);

		notifier.onAutoCompleted(tree, List.of(task));

		PopupNotice notice = queue.current(now);
		assertNotNull(notice);
		assertEquals("Mine some iron", notice.getName());
		assertEquals("1/1 sub-goals done in Varrock Easy", notice.getDetail());
		assertEquals(CompletionSound.TROPHY_DING.getSoundId(), notice.getSoundId());
	}

	@Test
	public void diaryWithTasksGivesOnePopupWithRootSound()
	{
		List<Goal> batch = new ArrayList<>();
		for (int i = 0; i < 12; i++)
		{
			Goal extra = new Goal("Task " + i);
			tree.add(diary.getId(), extra);
			batch.add(extra);
		}
		batch.add(0, diary);
		batch.forEach(goal -> goal.markCompleted(true, now));

		notifier.onAutoCompleted(tree, batch);

		PopupNotice notice = queue.current(now);
		assertEquals("Varrock Easy (+12 sub-goals)", notice.getName());
		assertNull(notice.getDetail());
		assertEquals(CompletionSound.FANFARE.getSoundId(), notice.getSoundId());
		assertEquals(1, queue.size());
	}

	@Test
	public void rootsOnlyIgnoresSubGoals()
	{
		level = PopupLevel.ROOTS_ONLY;
		task.markCompleted(true, now);

		notifier.onAutoCompleted(tree, List.of(task));

		assertNull(queue.current(now));
	}

	@Test
	public void offShowsNothing()
	{
		level = PopupLevel.OFF;
		diary.markCompleted(true, now);

		notifier.onAutoCompleted(tree, List.of(diary));

		assertNull(queue.current(now));
	}

	@Test
	public void soundDisabledGivesSilentPopup()
	{
		sound = false;
		diary.markCompleted(true, now);

		notifier.onAutoCompleted(tree, List.of(diary));

		PopupNotice notice = queue.current(now);
		assertEquals(PopupNotice.NO_SOUND, notice.getSoundId());
		assertFalse(notice.takeSound());
	}

	@Test
	public void soundIsTakenOncePerShownNotice()
	{
		diary.markCompleted(true, now);
		notifier.onAutoCompleted(tree, List.of(diary));

		PopupNotice notice = queue.current(now);
		assertTrue(notice.takeSound());
		assertFalse(notice.takeSound());
	}
}
