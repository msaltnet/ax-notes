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
    val contentDb = Room.databaseBuilder(context, ContentDatabase::class.java, "content.db")
        .addMigrations(ContentDatabase.MIGRATION_1_2).build()
    val personalDb = Room.databaseBuilder(context, PersonalDatabase::class.java, "personal.db").build()
    val personal = PersonalRepository(contentDb.dao(), personalDb)
    val scheduler = ReminderScheduler(context, personalDb)
    private val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    init {
        // All user-facing builds use the live feed. Retire historical sample choices
        // without deleting cached articles or private records; debug fixtures remain test-only.
        if (!settings.getBoolean("liveFeedDefaultV3", false)) {
            settings.edit().putBoolean("sample", false).putBoolean("liveFeedDefaultV3", true).apply()
        }
    }
    var sampleMode: Boolean
        get() = BuildConfig.DEBUG && settings.getBoolean("sample", false)
        set(value) { settings.edit().putBoolean("sample", value).apply() }
    fun content(): ContentRepository = ContentRepository(contentDb,
        if (sampleMode) SampleFeedSource(context, BuildConfig.MANIFEST_URL) else HttpFeedSource(), BuildConfig.MANIFEST_URL, sampleMode)
}
