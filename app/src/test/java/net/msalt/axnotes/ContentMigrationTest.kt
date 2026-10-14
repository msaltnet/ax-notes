package net.msalt.axnotes

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import net.msalt.axnotes.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ContentMigrationTest {
    @Test fun appGraphMigratesSchemaOneWithoutLosingOfflineOrPersonalData(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("content.db"); context.deleteDatabase("personal.db")
        // Use the unchanged exported v1 schema, including its original Room identity hash.
        val schema = JSONObject(javaClass.getResource("/content-schema-v1.json")!!.readText()).getJSONObject("database")
        context.openOrCreateDatabase("content.db", Context.MODE_PRIVATE, null).use { old ->
            val tables = schema.getJSONArray("entities")
            for (i in 0 until tables.length()) {
                val table = tables.getJSONObject(i)
                old.execSQL(table.getString("createSql").replace("\${TABLE_NAME}", table.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO articles VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", arrayOf<Any?>(
                "cached", "보존할 글", "요약", "2026-10-07", null, "https://ax.msalt.net/notes/cached/",
                "https://ax.msalt.net/app/v1/notes/cached/${"a".repeat(64)}.json", "a".repeat(64),
                "https://ax.msalt.net/series/nanobot/", "a".repeat(64), "<p>오프라인 본문</p>", "오프라인 본문", 123L, 0,
            ))
            old.execSQL("INSERT INTO feed VALUES (1, 456, 'AX Notes', 'https://ax.msalt.net/about/', '[]', 0)")
            old.version = 1
        }
        val bookmark = Bookmark("cached", "보존할 글", "https://ax.msalt.net/notes/cached/", 123)
        val memo = Memo("memo", "cached", bookmark.titleSnapshot, bookmark.urlSnapshot, "생각", "개인 메모", 123, 456)
        val reminder = Reminder("reminder", "cached", bookmark.titleSnapshot, bookmark.urlSnapshot, 999, "blocked", 4)
        Room.databaseBuilder(context, PersonalDatabase::class.java, "personal.db").build().let { db ->
            db.dao().putBookmark(bookmark); db.dao().putMemo(memo); db.dao().putReminder(reminder); db.close()
        }
        val graph = AppGraph(context)
        try {
            val article = graph.contentDb.dao().article("cached")!!
            assertEquals("보존할 글", article.title)
            assertEquals("<p>오프라인 본문</p>", article.bodyHtml)
            assertEquals("오프라인 본문", article.bodyText)
            assertEquals("a".repeat(64), article.cachedRevision)
            assertEquals(123L, article.cachedAt)
            assertFalse(article.available)
            assertNull(article.projectId); assertNull(article.projectTitle); assertNull(article.projectOrder)
            assertNull(article.seriesId); assertNull(article.seriesTitle); assertNull(article.seriesOrder)
            assertEquals(456L, graph.contentDb.dao().feed()!!.fetchedAt)
            assertEquals(bookmark, graph.personalDb.dao().bookmark("cached"))
            assertEquals(memo, graph.personalDb.dao().memo("memo"))
            assertEquals(reminder, graph.personalDb.dao().reminder("reminder"))
            val classified = article.copy(projectId = "nanobot", projectTitle = "나의 작은 에이전트 nanobot", projectOrder = 2)
            graph.contentDb.dao().put(listOf(classified))
            assertEquals(classified, graph.contentDb.dao().article("cached"))
        } finally {
            graph.contentDb.close(); graph.personalDb.close()
        }
        val reopened = AppGraph(context)
        try {
            assertEquals(2, reopened.contentDb.dao().article("cached")!!.projectOrder)
            assertEquals("오프라인 본문", reopened.contentDb.dao().article("cached")!!.bodyText)
            assertEquals(memo, reopened.personalDb.dao().memo("memo"))
        } finally {
            reopened.contentDb.close(); reopened.personalDb.close()
            context.deleteDatabase("content.db"); context.deleteDatabase("personal.db")
        }
    }
}
