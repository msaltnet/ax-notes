package net.msalt.axnotes.reminder

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import net.msalt.axnotes.MainActivity
import net.msalt.axnotes.data.Reminder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ReminderNotificationsTest {
    private lateinit var notifications: AndroidReminderNotifications
    private lateinit var manager: NotificationManager
    private fun reminder(id: String, generation: Long = 1L) = Reminder(id, "article-$id", "글 제목", "https://ax.msalt.net/notes/$id", 100L, ReminderState.POSTING, generation)

    @Before fun prepare() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        manager = context.getSystemService(NotificationManager::class.java)
        notifications = AndroidReminderNotifications(context)
    }

    @Test fun articleIntentsAreDistinctImmutableAndTargetInternalArticle() {
        val first = notifications.articleIntent(reminder("first"))
        val second = notifications.articleIntent(reminder("second"))
        val edited = notifications.articleIntent(reminder("first", 2L))
        assertNotEquals(first, second)
        assertNotEquals(first, edited)
        val intent = shadowOf(first).savedIntent
        assertEquals(MainActivity::class.java.name, intent.component!!.className)
        assertEquals("article-first", intent.getStringExtra(ReminderScheduler.EXTRA_ARTICLE_ID))
        assertTrue(shadowOf(first).isImmutable)
        assertFalse(intent.getBooleanExtra(ReminderScheduler.EXTRA_OPEN_LIBRARY, false))
    }

    @Test fun recoverySummaryIntentOpensLibrary() {
        val pendingIntent = notifications.libraryIntent()
        val intent = shadowOf(pendingIntent).savedIntent
        assertEquals(MainActivity::class.java.name, intent.component!!.className)
        assertTrue(intent.getBooleanExtra(ReminderScheduler.EXTRA_OPEN_LIBRARY, false))
        assertNull(intent.getStringExtra(ReminderScheduler.EXTRA_ARTICLE_ID))
        assertTrue(shadowOf(pendingIntent).isImmutable)
    }

    @Test fun repeatedArticlePostUsesOneStableNotificationIdentity() {
        assertTrue(notifications.article(reminder("first"), false))
        assertTrue(notifications.article(reminder("first"), true))
        val posted = manager.activeNotifications
        assertEquals(1, posted.size)
        assertEquals(AndroidReminderNotifications.articleTag("first"), posted.single().tag)
        assertEquals(AndroidReminderNotifications.NOTIFICATION_ID, posted.single().id)
        assertTrue(posted.single().notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
    }

    @Test fun globallyDisabledNotificationsDoNotPost() {
        shadowOf(manager).setNotificationsEnabled(false)
        assertFalse(notifications.enabled())
        assertFalse(notifications.article(reminder("first"), false))
        assertEquals(0, manager.activeNotifications.size)
    }

    @Test fun blockedChannelDoesNotPostEvenWhenAppNotificationsAreEnabled() {
        manager.createNotificationChannel(NotificationChannel(ReminderScheduler.CHANNEL_ID, "나중에 읽기", NotificationManager.IMPORTANCE_NONE))
        assertFalse(notifications.enabled())
        assertFalse(notifications.summary(listOf(reminder("first")), false))
        assertEquals(0, manager.activeNotifications.size)
    }

    @Test fun cancelRemovesArticleAndAllOwnedNotifications() {
        notifications.article(reminder("first"), false)
        notifications.article(reminder("second"), false)
        notifications.summary(listOf(reminder("third")), false)
        notifications.cancelArticle("first")
        assertEquals(2, manager.activeNotifications.size)
        notifications.cancelAll()
        assertEquals(0, manager.activeNotifications.size)
    }

    @Test fun removingOneSummaryMemberPreservesOthersAndUnrelatedEditsAreNoOps() {
        notifications.summary(listOf(reminder("first"), reminder("second")), false)
        notifications.removeFromSummary("unrelated")
        assertEquals(2, manager.activeNotifications.single().notification.number)
        notifications.removeFromSummary("first")
        assertEquals(1, manager.activeNotifications.size)
        assertEquals(1, manager.activeNotifications.single().notification.number)
        notifications.removeFromSummary("second")
        assertEquals(0, manager.activeNotifications.size)
    }

    @Test fun largeOverdueSummaryHasBoundedBinderPayload() {
        val rows = (1..10_000).map { reminder("reminder-$it").copy(titleSnapshot = "긴 제목 ".repeat(100)) }
        assertTrue(notifications.summary(rows, false))
        val notification = manager.activeNotifications.single().notification
        assertEquals(10_000, notification.number)
        val parcel = Parcel.obtain()
        try {
            notification.writeToParcel(parcel, 0)
            assertTrue("Summary payload was ${parcel.dataSize()} bytes", parcel.dataSize() < 32 * 1024)
        } finally { parcel.recycle() }
    }
}
