package com.gielinorgoals.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class NotificationQueueTest
{
	private static final long HOLD = 4000;
	private static final long TOTAL = NotificationQueue.FADE_IN_MS + HOLD + NotificationQueue.FADE_OUT_MS;

	private final NotificationQueue queue = new NotificationQueue();

	@Test
	public void showsOneAtATimeInOrder()
	{
		PopupNotice first = notice("A", false);
		PopupNotice second = notice("B", false);
		queue.offer(first);
		queue.offer(second);

		assertSame(first, queue.current(0));
		assertSame(first, queue.current(TOTAL - 1));
		assertSame(second, queue.current(TOTAL));
		assertNull(queue.current(TOTAL * 2));
	}

	@Test
	public void fadesInHoldsAndFadesOut()
	{
		PopupNotice notice = notice("A", false);
		queue.offer(notice);
		queue.current(0);

		assertEquals(0f, NotificationQueue.alpha(notice, 0), 0.001);
		assertEquals(1f, NotificationQueue.alpha(notice, NotificationQueue.FADE_IN_MS + 10), 0.001);
		assertEquals(0.5f, NotificationQueue.alpha(notice,
			NotificationQueue.FADE_IN_MS + HOLD + NotificationQueue.FADE_OUT_MS / 2), 0.001);
	}

	@Test
	public void longQueueIsMergedIntoOneNotice()
	{
		for (int i = 0; i < NotificationQueue.MAX_PENDING + 3; i++)
		{
			queue.offer(notice("Goal " + i, i == 6));
		}

		long t = 0;
		for (int i = 0; i < NotificationQueue.MAX_PENDING; i++)
		{
			assertEquals("Goal " + i, queue.current(t).getName());
			t += TOTAL;
		}
		PopupNotice merged = queue.current(t);
		assertEquals("+3 more goals completed", merged.getName());
		// Ett rotmål fanns bland de sammanslagna, så rotljudet används.
		assertEquals(2, merged.getSoundId());
		assertNull(queue.current(t + TOTAL));
	}

	@Test
	public void hoveredNoticeStaysUntilMouseLeaves()
	{
		PopupNotice first = notice("A", false);
		PopupNotice second = notice("B", false);
		queue.offer(first);
		queue.offer(second);
		queue.current(0);

		// Musen över notisen långt efter att den skulle ha tonat ut.
		assertSame(first, queue.current(TOTAL * 3, true));
		assertEquals(1f, NotificationQueue.alpha(first, TOTAL * 3), 0.001);

		// När musen lämnar visas den hela visningstiden och tonar sedan ut.
		long left = TOTAL * 3;
		assertSame(first, queue.current(left + HOLD, false));
		assertSame(second, queue.current(left + HOLD + NotificationQueue.FADE_OUT_MS, false));
	}

	@Test
	public void hoverDuringFadeOutBringsNoticeBack()
	{
		PopupNotice notice = notice("A", false);
		queue.offer(notice);
		queue.current(0);
		long fadingOut = NotificationQueue.FADE_IN_MS + HOLD + NotificationQueue.FADE_OUT_MS / 2;

		queue.current(fadingOut, true);

		assertEquals(1f, NotificationQueue.alpha(notice, fadingOut), 0.001);
	}

	@Test
	public void clearEmptiesQueue()
	{
		queue.offer(notice("A", false));
		queue.offer(notice("B", false));
		queue.current(0);

		queue.clear();

		assertNull(queue.current(1));
	}

	private static PopupNotice notice(String name, boolean root)
	{
		return new PopupNotice("Goal completed!", name, null, false, root, root ? 2 : 1, HOLD);
	}
}
