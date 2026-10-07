package net.msalt.axnotes.reminder

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.msalt.axnotes.data.Reminder
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderCoordinatorTest {
    private var instant = 10_000L
    private var nextId = 0
    private val store = MemoryStore()
    private val queue = FakeQueue()
    private val notifications = FakeNotifications()
    private fun coordinator() = ReminderCoordinator(store, queue, notifications, { instant }, { "id-${++nextId}" })
    private val scheduler = coordinator()
    private fun row(articleId: String = "article", due: Long = instant, state: String = ReminderState.SCHEDULED, generation: Long = 1) =
        Reminder("id-$articleId", articleId, "글 $articleId", "https://ax.msalt.net/notes/$articleId", due, state, generation)

    @Test fun scheduleThenEditKeepsIdIncrementsGenerationAndReplacesWork() = runTest {
        scheduler.schedule("article", "글", "https://ax.msalt.net/notes/article", instant + 50)
        val first = store.byArticle("article")!!
        scheduler.schedule("article", "수정된 글", first.urlSnapshot, instant + 100)
        val edited = store.byArticle("article")!!
        assertEquals(1, store.rows.size)
        assertEquals(first.id, edited.id)
        assertEquals(2L, edited.generation)
        assertEquals("수정된 글", edited.titleSnapshot)
        assertEquals(100L, queue.requests.last().second)
        assertNull(edited.notifiedAt)
    }

    @Test fun previousGenerationCannotPostAfterEdit() = runTest {
        store.put(listOf(row(generation = 2)))
        scheduler.deliver("id-article", 1)
        assertTrue(notifications.articles.isEmpty())
        assertEquals(ReminderState.SCHEDULED, store.byId("id-article")!!.state)
    }

    @Test fun duplicateWorkersPostOneLogicalNotification() = runTest {
        store.put(listOf(row()))
        scheduler.deliver("id-article", 1)
        coordinator().deliver("id-article", 1)
        assertEquals(1, notifications.articles.size)
        assertEquals(ReminderState.DELIVERED, store.byId("id-article")!!.state)
        assertEquals(instant, store.byId("id-article")!!.notifiedAt)
    }

    @Test fun resumedWorkersBundleMultipleDueRowsBeforeAnyForegroundReconcile() = runTest {
        store.put(listOf(row("first", instant - 10_000), row("second", instant - 5_000)))
        scheduler.deliver("id-first", 1)
        coordinator().deliver("id-second", 1)
        assertEquals(1, notifications.summaries.size)
        assertEquals(2, notifications.summaries.single().first.size)
        assertTrue(notifications.articles.isEmpty())
    }

    @Test fun firstWorkerAfterRebootSummarizesOverdueAndRestoresFutureWork() = runTest {
        val boot = FakeBoot(needsRecovery = true)
        val scheduler = ReminderCoordinator(store, queue, notifications, { instant }, boot = boot)
        store.put(listOf(row("missed", instant - 50), row("future", instant + 1_000)))
        scheduler.deliver("id-missed", 1)
        scheduler.deliver("id-missed", 1)
        assertEquals(1, notifications.summaries.size)
        assertTrue(notifications.articles.isEmpty())
        assertEquals(1_000L, queue.requests.single().second)
        assertEquals(1, boot.acknowledgements)
        assertFalse(boot.needsRecovery)
    }

    @Test fun normalColdWorkerInSameBootStillOpensItsSingleArticle() = runTest {
        val scheduler = ReminderCoordinator(store, queue, notifications, { instant }, boot = FakeBoot(false))
        store.put(listOf(row()))
        scheduler.deliver("id-article", 1)
        assertEquals(1, notifications.articles.size)
        assertTrue(notifications.summaries.isEmpty())
    }

    @Test fun failedRebootRecoveryIsNotAcknowledgedAndRetriesSilently() = runTest {
        val boot = FakeBoot(needsRecovery = true)
        val scheduler = ReminderCoordinator(store, queue, notifications, { instant }, boot = boot)
        store.put(listOf(row()))
        store.failCompletion = true
        assertTrue(runCatching { scheduler.deliver("id-article", 1) }.isFailure)
        assertTrue(boot.needsRecovery)
        store.failCompletion = false
        scheduler.deliver("id-article", 1)
        assertEquals(listOf(false, true), notifications.summaries.map { it.second })
        assertFalse(boot.needsRecovery)
    }

    @Test fun editingOverdueReminderAfterRebootDoesNotPostTheOldGeneration() = runTest {
        val boot = FakeBoot(needsRecovery = true)
        val scheduler = ReminderCoordinator(store, queue, notifications, { instant }, boot = boot)
        store.put(listOf(row()))
        scheduler.schedule("article", "글", "https://ax.msalt.net/notes/article", instant + 1_000)
        assertTrue(notifications.articles.isEmpty())
        assertTrue(notifications.summaries.isEmpty())
        assertEquals(2L, store.byId("id-article")!!.generation)
        assertEquals(1, queue.requests.size)
        assertEquals(1_000L, queue.requests.single().second)
    }

    @Test fun cancelledOrDeletedRowsCannotPost() = runTest {
        store.put(listOf(row()))
        scheduler.cancel("article")
        scheduler.deliver("id-article", 1)
        scheduler.deliver("id-article", 2)
        scheduler.deliver("deleted", 1)
        assertTrue(notifications.articles.isEmpty())
        assertEquals(ReminderState.CANCELLED, store.byId("id-article")!!.state)
        assertEquals(listOf("id-article"), queue.cancelled)
    }

    @Test fun cancelledRowCanBeRescheduledWithSameIdAndNewGeneration() = runTest {
        store.put(listOf(row()))
        scheduler.cancel("article")
        scheduler.schedule("article", "글", "https://ax.msalt.net/notes/article", instant + 60)
        val row = store.byArticle("article")!!
        assertEquals("id-article", row.id)
        assertEquals(3L, row.generation)
        assertEquals(ReminderState.SCHEDULED, row.state)
    }

    @Test fun workerBeforeDueTimeReplacesRemainingDelayWithoutPosting() = runTest {
        store.put(listOf(row(due = instant + 5_000)))
        scheduler.deliver("id-article", 1)
        assertTrue(notifications.articles.isEmpty())
        assertEquals(5_000L, queue.requests.single().second)
        assertEquals(1L, queue.requests.single().first.generation)
    }

    @Test fun permissionDeniedPreservesVisibleBlockedSchedule() = runTest {
        notifications.allowed = false
        scheduler.schedule("article", "글", "https://ax.msalt.net/notes/article", instant + 1_000)
        val row = store.byArticle("article")!!
        assertEquals(ReminderState.BLOCKED, row.state)
        assertEquals(1, queue.requests.size)
        instant += 1_000
        scheduler.deliver(row.id, row.generation)
        assertEquals(ReminderState.BLOCKED, store.byId(row.id)!!.state)
        assertTrue(notifications.articles.isEmpty())
    }

    @Test fun permissionRestoredReconcilesBlockedOverdueRowsOnceAsSummary() = runTest {
        store.put(listOf(row(state = ReminderState.BLOCKED), row("second", state = ReminderState.BLOCKED)))
        scheduler.reconcile()
        scheduler.reconcile()
        assertEquals(1, notifications.summaries.size)
        assertEquals(2, notifications.summaries.single().first.size)
        assertTrue(store.rows.values.all { it.state == ReminderState.DELIVERED })
    }

    @Test fun reconcileBundlesOverdueAndReschedulesFutureWithAbsoluteDueUnchanged() = runTest {
        store.put(listOf(row("first", instant - 1), row("second", instant - 2), row("future", instant + 7_000)))
        scheduler.reconcile()
        scheduler.deliver("id-first", 1)
        scheduler.deliver("id-second", 1)
        assertEquals(1, notifications.summaries.size)
        assertTrue(notifications.articles.isEmpty())
        assertEquals(7_000L, queue.requests.single().second)
        assertEquals(17_000L, store.byId("id-future")!!.dueAt)
    }

    @Test fun blockedReconcileDoesNotAttemptNotificationsAndKeepsRows() = runTest {
        notifications.allowed = false
        store.put(listOf(row("first"), row("future", instant + 1_000)))
        scheduler.reconcile()
        assertTrue(notifications.summaries.isEmpty())
        assertTrue(notifications.articles.isEmpty())
        assertEquals(2, store.rows.size)
        assertTrue(store.rows.values.all { it.state == ReminderState.BLOCKED })
        assertEquals(1, queue.requests.size)
    }

    @Test fun individualCrashClaimRecoversSameIdentitySilently() = runTest {
        store.put(listOf(row()))
        store.failCompletion = true
        val failure = runCatching { scheduler.deliver("id-article", 1) }
        assertTrue(failure.isFailure)
        assertEquals(ReminderState.POSTING, store.byId("id-article")!!.state)
        store.failCompletion = false
        coordinator().reconcile()
        assertEquals(listOf(false, true), notifications.articles.map { it.second })
        assertEquals(1, notifications.articles.map { it.first.id }.toSet().size)
        assertTrue(notifications.summaries.isEmpty())
        assertEquals(ReminderState.DELIVERED, store.byId("id-article")!!.state)
    }

    @Test fun summaryCrashClaimRecoversSilentlyWithoutIndividualNotification() = runTest {
        store.put(listOf(row(), row("second")))
        store.failCompletion = true
        assertTrue(runCatching { scheduler.reconcile() }.isFailure)
        assertTrue(store.rows.values.all { it.state == ReminderState.POSTING_SUMMARY })
        store.failCompletion = false
        coordinator().deliver("id-article", 1)
        coordinator().reconcile()
        assertEquals(listOf(false, true), notifications.summaries.map { it.second })
        assertTrue(notifications.articles.isEmpty())
        assertTrue(store.rows.values.all { it.state == ReminderState.DELIVERED })
    }

    @Test fun postingFailurePreservesClaimForRetry() = runTest {
        store.put(listOf(row()))
        notifications.throwOnPost = true
        assertTrue(runCatching { scheduler.deliver("id-article", 1) }.isFailure)
        assertEquals(ReminderState.POSTING, store.byId("id-article")!!.state)
        notifications.throwOnPost = false
        scheduler.deliver("id-article", 1)
        assertTrue(notifications.articles.single().second)
    }

    @Test fun permissionRaceKeepsBlockedRowsInsteadOfClaimingDelivery() = runTest {
        store.put(listOf(row()))
        notifications.accepted = false
        scheduler.deliver("id-article", 1)
        assertEquals(ReminderState.BLOCKED, store.byId("id-article")!!.state)
        assertNull(store.byId("id-article")!!.notifiedAt)
    }

    @Test fun clockMovingBackRepairsPostingClaimToFutureSchedule() = runTest {
        store.put(listOf(row(state = ReminderState.POSTING)))
        instant -= 2_000
        scheduler.reconcile()
        assertTrue(notifications.articles.isEmpty())
        assertEquals(ReminderState.SCHEDULED, store.byId("id-article")!!.state)
        assertEquals(2_000L, queue.requests.single().second)
        assertTrue("id-article" in notifications.cancelled)
    }

    @Test fun markOpenedChangesOnlyAlreadyDueDeliveryStates() = runTest {
        store.put(listOf(row(state = ReminderState.DELIVERED), row("future", instant + 100)))
        scheduler.markOpened("article")
        scheduler.markOpened("future")
        assertEquals(ReminderState.OPENED, store.byId("id-article")!!.state)
        assertEquals(ReminderState.SCHEDULED, store.byId("id-future")!!.state)
    }

    @Test fun personalClearRemovesRowsAndDisarmsStaleWorkers() = runTest {
        store.put(listOf(row()))
        scheduler.clearPersonalData()
        scheduler.deliver("id-article", 1)
        assertTrue(store.cleared)
        assertTrue(store.rows.isEmpty())
        assertTrue(queue.allCancelled)
        assertTrue(notifications.allCancelled)
        assertTrue(notifications.articles.isEmpty())
    }

    @Test fun separateSchedulerInstancesSerializeWorkerAndUiCancellation() = runTest {
        store.put(listOf(row()))
        val claimed = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        store.beforePut = { rows ->
            if (rows.any { it.state == ReminderState.POSTING }) { claimed.complete(Unit); release.await() }
        }
        val delivery = launch { scheduler.deliver("id-article", 1) }
        claimed.await()
        val cancellation = launch { coordinator().cancel("article") }
        runCurrent()
        assertTrue(queue.cancelled.isEmpty())
        release.complete(Unit)
        delivery.join(); cancellation.join()
        assertEquals(ReminderState.CANCELLED, store.byId("id-article")!!.state)
        assertEquals(1, notifications.articles.size)
        assertEquals(listOf("id-article"), queue.cancelled)
        assertTrue("id-article" in notifications.cancelled)
    }

    @Test fun invalidPastScheduleDoesNotCreateAnyRowOrWork() = runTest {
        assertTrue(runCatching { scheduler.schedule("article", "글", "https://ax.msalt.net/notes/article", instant) }.isFailure)
        assertTrue(store.rows.isEmpty())
        assertTrue(queue.requests.isEmpty())
    }

    @Test fun extremeClockArithmeticCannotProduceNegativeDelay() {
        assertEquals(Long.MAX_VALUE, scheduler.remainingDelay(Long.MAX_VALUE, -1))
        assertEquals(0L, scheduler.remainingDelay(1L, 2L))
        assertEquals(1L, scheduler.remainingDelay(2L, 1L))
    }

    private class MemoryStore : ReminderStore {
        val rows = linkedMapOf<String, Reminder>()
        var failCompletion = false
        var cleared = false
        var beforePut: (suspend (List<Reminder>) -> Unit)? = null
        override suspend fun byArticle(articleId: String) = rows.values.firstOrNull { it.articleId == articleId }
        override suspend fun byId(id: String) = rows[id]
        override suspend fun active() = rows.values.filter { it.state in ReminderState.active }.sortedBy { it.dueAt }
        override suspend fun put(rows: List<Reminder>) {
            beforePut?.invoke(rows)
            if (failCompletion && rows.any { it.state == ReminderState.DELIVERED }) error("simulated process interruption")
            rows.forEach { this.rows[it.id] = it }
        }
        override suspend fun clearPersonalData() { rows.clear(); cleared = true }
    }

    private class FakeQueue : ReminderWorkQueue {
        val requests = mutableListOf<Pair<Reminder, Long>>()
        val cancelled = mutableListOf<String>()
        var allCancelled = false
        override suspend fun replace(row: Reminder, delayMillis: Long) { requests += row to delayMillis }
        override suspend fun cancel(id: String) { cancelled += id }
        override suspend fun cancelAll() { allCancelled = true }
    }

    private class FakeBoot(var needsRecovery: Boolean) : ReminderBootTracker {
        var acknowledgements = 0
        override fun needsReconciliation() = needsRecovery
        override fun markReconciled() { acknowledgements++; needsRecovery = false }
    }

    private class FakeNotifications : ReminderNotifications {
        var allowed = true
        var accepted = true
        var throwOnPost = false
        var allCancelled = false
        val articles = mutableListOf<Pair<Reminder, Boolean>>()
        val summaries = mutableListOf<Pair<List<Reminder>, Boolean>>()
        val cancelled = mutableListOf<String>()
        override fun enabled() = allowed
        override fun article(row: Reminder, recovering: Boolean): Boolean {
            if (throwOnPost) error("simulated notification service failure")
            articles += row to recovering
            return accepted
        }
        override fun summary(rows: List<Reminder>, recovering: Boolean): Boolean {
            if (throwOnPost) error("simulated notification service failure")
            summaries += rows to recovering
            return accepted
        }
        override fun cancelArticle(id: String) { cancelled += id }
        override fun removeFromSummary(id: String) = Unit
        override fun cancelAll() { allCancelled = true }
    }
}
