package net.msalt.axnotes.reminder

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.msalt.axnotes.data.PersonalDataGate
import net.msalt.axnotes.data.Reminder
import java.util.UUID

internal object ReminderState {
    const val SCHEDULED = "scheduled"
    const val BLOCKED = "blocked"
    const val POSTING = "posting"
    const val POSTING_SUMMARY = "posting_summary"
    const val DELIVERED = "delivered"
    const val OPENED = "opened"
    const val CANCELLED = "cancelled"
    val active = setOf(SCHEDULED, BLOCKED, POSTING, POSTING_SUMMARY)
}

internal interface ReminderStore {
    suspend fun byArticle(articleId: String): Reminder?
    suspend fun byId(id: String): Reminder?
    suspend fun active(): List<Reminder>
    /** All rows in one transaction, including a summary's claim and completion. */
    suspend fun put(rows: List<Reminder>)
    suspend fun clearPersonalData()
}

internal interface ReminderWorkQueue {
    suspend fun replace(row: Reminder, delayMillis: Long)
    suspend fun cancel(id: String)
    suspend fun cancelAll()
}

internal interface ReminderNotifications {
    fun enabled(): Boolean
    /** False means permission or channel became blocked before/during posting. */
    fun article(row: Reminder, recovering: Boolean): Boolean
    fun summary(rows: List<Reminder>, recovering: Boolean): Boolean
    fun cancelArticle(id: String)
    fun removeFromSummary(id: String)
    fun cancelAll()
}

internal interface ReminderBootTracker {
    fun needsReconciliation(): Boolean
    fun markReconciled()
}

private object UntrackedBoot : ReminderBootTracker {
    override fun needsReconciliation() = false
    override fun markReconciled() = Unit
}

/**
 * Room is the source of truth; WorkManager requests are disposable projections.
 * All scheduler instances, UI mutations and workers share this process-wide gate.
 * No component is configured to run in a separate Android process.
 *
 * Room and NotificationManager cannot commit atomically. A durable posting claim
 * is retried with the same notification identity, silently on recovery. This gives
 * logical idempotence and bounds duplicate alerts; it cannot prove physical
 * exactly-once display (for example, if a user dismisses a post before a crash).
 */
internal class ReminderCoordinator(
    private val store: ReminderStore,
    private val work: ReminderWorkQueue,
    private val notifications: ReminderNotifications,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val boot: ReminderBootTracker = UntrackedBoot,
) {
    // Once a mutation owns the gate, cancellation must not split its DB/OS/work
    // steps. A real process death remains recoverable from the durable DB state.
    private suspend fun <T> serialized(action: suspend () -> T): T = PersonalDataGate.mutex.withLock {
        withContext(NonCancellable) { action() }
    }

    suspend fun schedule(articleId: String, title: String, url: String, dueAt: Long) = serialized {
        require(articleId.isNotBlank() && title.isNotBlank()) { "연결할 글이 필요합니다" }
        require(dueAt > now()) { "미래 시각을 선택하세요" }
        val old = store.byArticle(articleId)
        val row = Reminder(
            id = old?.id ?: newId(), articleId = articleId,
            titleSnapshot = title, urlSnapshot = url, dueAt = dueAt,
            state = if (notifications.enabled()) ReminderState.SCHEDULED else ReminderState.BLOCKED,
            generation = Math.addExact(old?.generation ?: 0L, 1L),
        )
        store.put(listOf(row))
        notifications.cancelArticle(row.id)
        if (old != null) notifications.removeFromSummary(row.id)
        // Commit this edit before reboot recovery, so recovery cannot post the
        // old due generation the user is currently moving into the future.
        if (!reconcileAfterBootIfNeeded()) work.replace(row, remainingDelay(row.dueAt, now()))
    }

    suspend fun cancel(articleId: String) = serialized {
        val row = store.byArticle(articleId) ?: return@serialized
        store.put(listOf(row.copy(state = ReminderState.CANCELLED, generation = Math.addExact(row.generation, 1L))))
        notifications.cancelArticle(row.id)
        notifications.removeFromSummary(row.id)
        work.cancel(row.id)
    }

    suspend fun deliver(id: String, generation: Long) = serialized {
        val row = store.byId(id) ?: return@serialized
        if (row.generation != generation || row.state !in ReminderState.active) return@serialized
        // WorkManager may be the first app component started after reboot. Do
        // not depend on the foreground Activity racing these resumed workers.
        if (reconcileAfterBootIfNeeded()) return@serialized
        if (row.dueAt > now()) {
            reschedule(row)
        } else if (row.state == ReminderState.POSTING_SUMMARY) {
            // A worker must never turn a previously claimed summary into an
            // individual alert, including after process death or a retry.
            reconcileLocked()
        } else if (row.state != ReminderState.POSTING && store.active().count {
                it.dueAt <= now() && it.state != ReminderState.POSTING
            } > 1) {
            // Multiple workers resumed after Doze or a long process stop also
            // coalesce, even when there was no device reboot or foreground UI.
            reconcileLocked()
        } else {
            postArticle(row)
        }
    }

    suspend fun reconcile() = serialized { reconcileLocked(); boot.markReconciled() }

    private suspend fun reconcileAfterBootIfNeeded(): Boolean {
        if (!boot.needsReconciliation()) return false
        reconcileLocked()
        boot.markReconciled()
        return true
    }

    private suspend fun reconcileLocked() {
        val instant = now()
        val rows = store.active()
        for (row in rows.filter { it.dueAt > instant }) reschedule(row)

        val overdue = rows.filter { it.dueAt <= instant }
        if (overdue.isEmpty()) return
        if (!notifications.enabled()) {
            store.put(overdue.map { it.copy(state = ReminderState.BLOCKED) })
            return
        }
        // Preserve an individual claim's original identity when recovering a
        // crash between NotificationManager.notify and the completion write.
        for (row in overdue.filter { it.state == ReminderState.POSTING }) postArticle(row)

        val candidates = overdue.filter { it.state != ReminderState.POSTING }
        val summary = candidates.filter { it.dueAt <= now() }
        candidates.filter { it.dueAt > now() }.forEach { reschedule(it) }
        if (summary.isEmpty()) return
        val recovering = summary.any { it.state == ReminderState.POSTING_SUMMARY }
        val claimed = summary.map { it.copy(state = ReminderState.POSTING_SUMMARY) }
        store.put(claimed)
        val posted = notifications.summary(claimed, recovering)
        store.put(claimed.map {
            it.copy(state = if (posted) ReminderState.DELIVERED else ReminderState.BLOCKED,
                notifiedAt = if (posted) now() else null)
        })
        // Existing workers may still execute: terminal-state validation above
        // makes them no-ops. Avoid cancelling a worker that initiated recovery.
    }

    private suspend fun reschedule(row: Reminder) {
        if (row.state == ReminderState.POSTING) notifications.cancelArticle(row.id)
        if (row.state == ReminderState.POSTING_SUMMARY) notifications.removeFromSummary(row.id)
        val future = row.copy(state = if (notifications.enabled()) ReminderState.SCHEDULED else ReminderState.BLOCKED,
            notifiedAt = null)
        store.put(listOf(future))
        work.replace(future, remainingDelay(future.dueAt, now()))
    }

    private suspend fun postArticle(row: Reminder) {
        // Recheck after any preceding database/queue work in case the system
        // clock moved backwards while reconciliation was in progress.
        if (row.dueAt > now()) { reschedule(row); return }
        if (!notifications.enabled()) {
            store.put(listOf(row.copy(state = ReminderState.BLOCKED)))
            return
        }
        val recovering = row.state == ReminderState.POSTING
        val claimed = row.copy(state = ReminderState.POSTING)
        store.put(listOf(claimed))
        val posted = notifications.article(claimed, recovering)
        store.put(listOf(claimed.copy(
            state = if (posted) ReminderState.DELIVERED else ReminderState.BLOCKED,
            notifiedAt = if (posted) now() else null,
        )))
    }

    suspend fun markOpened(articleId: String) = serialized {
        val row = store.byArticle(articleId) ?: return@serialized
        if (row.state in setOf(ReminderState.DELIVERED, ReminderState.POSTING, ReminderState.POSTING_SUMMARY)) {
            store.put(listOf(row.copy(state = ReminderState.OPENED)))
            notifications.cancelArticle(row.id)
            notifications.removeFromSummary(row.id)
        }
    }

    /** Caller presents the destructive-data confirmation before invoking this. */
    suspend fun clearPersonalData() = serialized {
        // Delete first, so even an old worker surviving an OS cancellation can
        // no longer pass validation. All personal tables share one transaction.
        store.clearPersonalData()
        notifications.cancelAll()
        work.cancelAll()
    }

    internal fun remainingDelay(dueAt: Long, instant: Long): Long = when {
        dueAt <= instant -> 0L
        instant < 0 && dueAt > Long.MAX_VALUE + instant -> Long.MAX_VALUE
        else -> dueAt - instant
    }
}
