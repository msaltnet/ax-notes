package net.msalt.axnotes.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.msalt.axnotes.MainActivity
import net.msalt.axnotes.R
import net.msalt.axnotes.data.PersonalDatabase
import net.msalt.axnotes.data.Reminder
import java.util.concurrent.TimeUnit

class ReminderScheduler(context: Context, personalDb: PersonalDatabase) {
    private val app = context.applicationContext
    private val notifications = AndroidReminderNotifications(app)
    private val coordinator = ReminderCoordinator(RoomReminderStore(personalDb), AndroidReminderWorkQueue(app), notifications,
        boot = AndroidReminderBootTracker(app))

    fun enabled(): Boolean = notifications.enabled()
    suspend fun schedule(articleId: String, title: String, url: String, dueAt: Long) = withContext(Dispatchers.IO) { coordinator.schedule(articleId, title, url, dueAt) }
    suspend fun cancel(articleId: String) = withContext(Dispatchers.IO) { coordinator.cancel(articleId) }
    suspend fun reconcile() = withContext(Dispatchers.IO) { coordinator.reconcile() }
    suspend fun deliver(id: String, generation: Long) = withContext(Dispatchers.IO) { coordinator.deliver(id, generation) }
    suspend fun markOpened(articleId: String) = withContext(Dispatchers.IO) { coordinator.markOpened(articleId) }
    suspend fun clearPersonalData() = withContext(Dispatchers.IO) { coordinator.clearPersonalData() }

    companion object {
        const val CHANNEL_ID = "reading_reminders"
        const val EXTRA_ARTICLE_ID = "articleId"
        const val EXTRA_OPEN_LIBRARY = "openLibrary"
        internal const val WORK_TAG = "axnotes.reading_reminders"
        internal const val INPUT_ID = "reminder_id"
        internal const val INPUT_GENERATION = "reminder_generation"
        internal const val RECONCILE_WORK = "axnotes.reminder.reconcile"
        internal fun workName(id: String) = "axnotes.reminder.$id"

        fun enqueueReconcile(context: Context): Operation {
            return WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                RECONCILE_WORK, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ReconcileRemindersWorker>().addTag(WORK_TAG).build(),
            )
        }
    }
}

private class AndroidReminderBootTracker(context: Context) : ReminderBootTracker {
    private val app = context.applicationContext
    private val preferences = app.getSharedPreferences("reminder_recovery", Context.MODE_PRIVATE)
    // BOOT_COUNT is available from API 24 and needs no extra permission. If an
    // unusual device does not expose it, foreground reconcile still recovers.
    private fun currentBoot(): Int = try {
        Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -1)
    } catch (_: SecurityException) { -1 }
    override fun needsReconciliation(): Boolean {
        val current = currentBoot()
        return current >= 0 && preferences.getInt("last_reconciled_boot", -1) != current
    }
    override fun markReconciled() {
        val current = currentBoot()
        if (current >= 0 && preferences.getInt("last_reconciled_boot", -1) != current) {
            // This runs under the coordinator's process gate, off the UI
            // thread in Room/worker callers. Persist before considering reboot
            // recovery complete; failed writes leave a retryable operation.
            check(preferences.edit().putInt("last_reconciled_boot", current).commit()) { "알림 복구 상태를 저장하지 못했습니다" }
        }
    }
}

private class RoomReminderStore(private val db: PersonalDatabase) : ReminderStore {
    private val dao = db.dao()
    override suspend fun byArticle(articleId: String) = dao.reminderFor(articleId)
    override suspend fun byId(id: String) = dao.reminder(id)
    override suspend fun active() = dao.activeReminders()
    override suspend fun put(rows: List<Reminder>) { db.withTransaction { rows.forEach { dao.putReminder(it) } } }
    override suspend fun clearPersonalData() {
        db.withTransaction { dao.clearBookmarks(); dao.clearMemos(); dao.clearReminders() }
    }
}

private class AndroidReminderWorkQueue(context: Context) : ReminderWorkQueue {
    private val work = WorkManager.getInstance(context.applicationContext)
    override suspend fun replace(row: Reminder, delayMillis: Long) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workDataOf(ReminderScheduler.INPUT_ID to row.id, ReminderScheduler.INPUT_GENERATION to row.generation))
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(ReminderScheduler.WORK_TAG)
            .build()
        // There are deliberately no network, expedited, or exact-alarm requirements.
        work.enqueueUniqueWork(ReminderScheduler.workName(row.id), ExistingWorkPolicy.REPLACE, request).await()
    }
    override suspend fun cancel(id: String) { work.cancelUniqueWork(ReminderScheduler.workName(id)).await() }
    override suspend fun cancelAll() { work.cancelAllWorkByTag(ReminderScheduler.WORK_TAG).await() }
}

internal class AndroidReminderNotifications(private val context: Context) : ReminderNotifications {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(NotificationChannel(ReminderScheduler.CHANNEL_ID, "나중에 읽기", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "직접 예약한 글 읽기 알림입니다. 절전이나 OS 제한으로 지연될 수 있습니다."
        })
    }

    override fun enabled(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val channel = manager.getNotificationChannel(ReminderScheduler.CHANNEL_ID) ?: return false
        if (channel.importance == NotificationManager.IMPORTANCE_NONE) return false
        if (Build.VERSION.SDK_INT >= 28 && channel.group != null && manager.getNotificationChannelGroup(channel.group)?.isBlocked == true) return false
        return true
    }

    override fun article(row: Reminder, recovering: Boolean): Boolean = post(articleTag(row.id)) {
        builder(recovering)
            .setContentTitle(row.titleSnapshot)
            .setContentText("저장해 둔 글을 읽어보세요")
            .setContentIntent(articleIntent(row))
            .setWhen(row.dueAt)
    }

    override fun summary(rows: List<Reminder>, recovering: Boolean): Boolean = postSummary(
        rows.take(MAX_SUMMARY_LINES).map { it.id },
        rows.take(MAX_SUMMARY_LINES).map { it.titleSnapshot.take(MAX_SUMMARY_TITLE) }, rows.size, recovering,
    )

    private fun postSummary(ids: List<String>, titles: List<String>, totalCount: Int, recovering: Boolean): Boolean = post(SUMMARY_TAG) {
        val inbox = NotificationCompat.InboxStyle()
        titles.forEach { inbox.addLine(it) }
        if (totalCount > titles.size) inbox.setSummaryText("다른 글은 내 보관함에서 확인하세요")
        builder(recovering)
            .setContentTitle("읽기를 기다리는 글 ${totalCount}개")
            .setContentText("지나간 읽기 알림을 내 보관함에서 확인하세요")
            .setStyle(inbox)
            .setNumber(totalCount)
            .addExtras(Bundle().apply {
                // Bound the Binder payload regardless of library size. Keep
                // only displayed snippets; the total is a delivery snapshot.
                putStringArrayList(SUMMARY_IDS, ArrayList(ids))
                putStringArrayList(SUMMARY_TITLES, ArrayList(titles))
                putInt(SUMMARY_TOTAL, totalCount)
            })
            .setContentIntent(libraryIntent())
    }

    private fun builder(recovering: Boolean) = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_note)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .apply { if (recovering) setSilent(true) }

    @SuppressLint("MissingPermission") // enabled() explicitly checks API 33 permission and app/channel state.
    private inline fun post(tag: String, build: () -> NotificationCompat.Builder): Boolean {
        if (!enabled()) return false
        return try {
            manager.notify(tag, NOTIFICATION_ID, build().build())
            // Android cannot acknowledge physical display. Recheck a permission
            // race where notify may have been silently ignored by the platform.
            enabled()
        } catch (_: SecurityException) { false }
    }

    internal fun articleIntent(row: Reminder): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction("${context.packageName}.OPEN_REMINDER")
            .setData(Uri.Builder().scheme("axnotes").authority("reminder").appendPath(row.id).appendPath(row.generation.toString()).build())
            .putExtra(ReminderScheduler.EXTRA_ARTICLE_ID, row.articleId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    internal fun libraryIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction("${context.packageName}.OPEN_REMINDER_LIBRARY")
            .setData(Uri.parse("axnotes://reminder-summary"))
            .putExtra(ReminderScheduler.EXTRA_OPEN_LIBRARY, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    override fun cancelArticle(id: String) { manager.cancel(articleTag(id), NOTIFICATION_ID) }
    override fun removeFromSummary(id: String) {
        val summary = manager.activeNotifications.firstOrNull { it.tag == SUMMARY_TAG && it.id == NOTIFICATION_ID } ?: return
        val ids = summary.notification.extras.getStringArrayList(SUMMARY_IDS) ?: return
        if (id !in ids) return
        val titles = summary.notification.extras.getStringArrayList(SUMMARY_TITLES)
        val remaining = ids.indices.filter { ids[it] != id }
        val totalCount = summary.notification.extras.getInt(SUMMARY_TOTAL, ids.size)
        if (totalCount <= 1 || titles == null || titles.size != ids.size || !enabled()) {
            manager.cancel(SUMMARY_TAG, NOTIFICATION_ID)
        } else {
            // Editing/cancelling/opening one member removes its stale line but
            // retains the other unread members, without another audible alert.
            postSummary(remaining.map { ids[it] }, remaining.map { titles[it] }, totalCount - 1, recovering = true)
        }
    }
    override fun cancelAll() {
        manager.activeNotifications.filter { it.tag?.startsWith(TAG_PREFIX) == true }
            .forEach { manager.cancel(it.tag, it.id) }
    }

    companion object {
        internal const val TAG_PREFIX = "axnotes.reading."
        internal const val SUMMARY_TAG = "${TAG_PREFIX}summary"
        internal const val NOTIFICATION_ID = 1
        private const val SUMMARY_IDS = "axnotes.summary.ids"
        private const val SUMMARY_TITLES = "axnotes.summary.titles"
        private const val SUMMARY_TOTAL = "axnotes.summary.total"
        private const val MAX_SUMMARY_LINES = 5
        private const val MAX_SUMMARY_TITLE = 160
        internal fun articleTag(id: String) = "${TAG_PREFIX}article.$id"
    }
}
