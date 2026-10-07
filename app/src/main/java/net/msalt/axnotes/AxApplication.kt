package net.msalt.axnotes

import android.app.Application
import android.content.Context
import androidx.room.Room
import net.msalt.axnotes.data.*
import net.msalt.axnotes.reminder.ReminderScheduler

class AxApplication : Application() {
    val graph by lazy { AppGraph(this) }
}
class AppGraph(private val context: Context) {
    val contentDb = Room.databaseBuilder(context, ContentDatabase::class.java, "content.db").build()
    val personalDb = Room.databaseBuilder(context, PersonalDatabase::class.java, "personal.db").build()
    val personal = PersonalRepository(contentDb.dao(), personalDb)
    val scheduler = ReminderScheduler(context, personalDb)
    private val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var sampleMode: Boolean
        get() = settings.getBoolean("sample", BuildConfig.DEBUG)
        set(value) { settings.edit().putBoolean("sample", value).apply() }
    fun content(): ContentRepository = ContentRepository(contentDb,
        if (sampleMode) SampleFeedSource(context, BuildConfig.MANIFEST_URL) else HttpFeedSource(), BuildConfig.MANIFEST_URL, sampleMode)
}
