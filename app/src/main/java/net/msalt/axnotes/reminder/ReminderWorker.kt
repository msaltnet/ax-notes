package net.msalt.axnotes.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.msalt.axnotes.AxApplication

class ReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val id = inputData.getString(ReminderScheduler.INPUT_ID) ?: return Result.failure()
        val generation = inputData.getLong(ReminderScheduler.INPUT_GENERATION, -1L)
        if (generation < 1L) return Result.failure()
        return try {
            (applicationContext as AxApplication).graph.scheduler.deliver(id, generation)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A committed posting claim is recoverable on retry or app startup.
            // Do not put article titles, URLs or personal content into work logs.
            Result.retry()
        }
    }
}

class ReconcileRemindersWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        (applicationContext as AxApplication).graph.scheduler.reconcile()
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        Result.retry()
    }
}

/** WorkManager owns reboot persistence; there is deliberately no boot receiver. */
class ClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIME_CHANGED || intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            // Epoch millis are unchanged on zone changes; rebuilding delays also
            // repairs wall-clock edits without doing database work in onReceive.
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Keep the receiver alive until the work request is durably
                    // enqueued; all actual reminder processing stays in WorkManager.
                    ReminderScheduler.enqueueReconcile(context).await()
                } catch (_: Exception) {
                    android.util.Log.w("AXNotesReminder", "Clock reconciliation could not be queued")
                } finally { pending.finish() }
            }
        }
    }
}
