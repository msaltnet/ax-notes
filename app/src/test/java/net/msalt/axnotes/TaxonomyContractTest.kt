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
class TaxonomyContractTest {
    private val url = "https://ax.msalt.net/app/v1/manifest.json"
    // Built from ax-notes-public main 3bfdc128 + the additive taxonomy export.
    // These are authored collection values, not groups inferred from article titles.
    private fun fixture() = JSONObject(javaClass.getResource("/taxonomy-manifest.json")!!.readText())
    private fun parse(json: JSONObject) = FeedContract.manifest(json.toString(), url).notes

    @Test fun realPublicFeedPreservesProjectsSeriesAndEditorialOrder() {
        val notes = parse(fixture())
        val project = notes.filter { it.projectId == "nanobot" }.sortedBy { it.projectOrder }
        assertEquals(listOf("nanobot-requirements-first", "nanobot-bot-selection", "nanobot-ubuntu-setup", "nanobot-telegram"), project.map { it.id })
        assertEquals(listOf(1, 2, 3, 4), project.map { it.projectOrder })
        assertTrue(project.all { it.projectTitle == "나의 작은 에이전트 nanobot" && it.seriesId == null })
        assertEquals("AX Notes 앱 만들기", notes.single { it.projectId == "ax-notes-app" }.projectTitle)
        assertEquals("dots와 muse 사용기", notes.single { it.seriesId == "dots-and-muse" }.seriesTitle)
        val series = notes.single { it.id == "first-vibe-coding" }
        assertEquals("vibe-coding-workflow", series.seriesId)
        assertEquals("나의 바이브 코딩 workflow", series.seriesTitle)
        assertEquals(1, series.seriesOrder)
        assertNull(series.projectId)
        val solo = notes.single { it.id == "free-llm-apis" }
        assertNull(solo.projectId); assertNull(solo.seriesId)
    }

    @Test fun legacyManifestWithoutAnyTaxonomyRemainsReadable() {
        val json = fixture()
        val notes = json.getJSONArray("notes")
        for (i in 0 until notes.length()) fields.forEach { notes.getJSONObject(i).remove(it) }
        assertTrue(parse(json).all { it.projectId == null && it.seriesId == null })
        assertNotNull(parse(json).first { it.id == "nanobot-requirements-first" }.projectUrl)
    }

    @Test fun invalidOrIncompleteTaxonomyIsRejectedBeforeRefresh() {
        val invalid = listOf(
            "projectId" to "../nanobot", "projectId" to 42,
            "projectTitle" to " ", "projectTitle" to "가".repeat(501),
            "projectOrder" to 0, "projectOrder" to -1, "projectOrder" to 1.5,
            "projectOrder" to "2", "projectOrder" to 2147483648L,
            "projectOrder" to JSONObject.NULL,
        )
        for ((field, value) in invalid) {
            val json = fixture()
            val notes = json.getJSONArray("notes")
            val project = (0 until notes.length()).map { notes.getJSONObject(it) }.first { it.optString("projectId") == "nanobot" }
            project.put(field, value)
            assertThrows("$field=$value", Exception::class.java) { parse(json) }
        }
        val json = fixture()
        val note = json.getJSONArray("notes").getJSONObject(0)
        note.put("projectId", "nanobot").put("projectTitle", "나의 작은 에이전트 nanobot").put("projectOrder", 1)
        note.put("seriesId", "dots-and-muse").put("seriesTitle", "dots와 muse 사용기").put("seriesOrder", 1)
        assertThrows(Exception::class.java) { parse(json) }
    }

    @Test fun refreshStoresTaxonomyWithoutDiscardingCachedBodies(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ContentDatabase::class.java).build()
        val manifest = fixture()
        val article = parse(manifest).single { it.id == "nanobot-telegram" }
        val cached = article.copy(projectId = null, projectTitle = null, projectOrder = null,
            cachedRevision = article.wantedRevision, bodyHtml = "<p>오프라인 본문</p>", bodyText = "오프라인 본문", cachedAt = 123L)
        try {
            db.dao().put(listOf(cached))
            val source = object : FeedSource {
                override suspend fun get(url: String, limit: Int): String {
                    require(url == this@TaxonomyContractTest.url)
                    return manifest.toString()
                }
            }
            // Other article details are deliberately unavailable; one failure must not erase this cache.
            ContentRepository(db, source, url, false).refresh()
            assertEquals(cached.copy(projectId = article.projectId, projectTitle = article.projectTitle, projectOrder = article.projectOrder), db.dao().article(article.id))
            manifest.getJSONArray("notes").getJSONObject(0).put("seriesOrder", 0)
            val before = db.dao().all()
            try {
                ContentRepository(db, source, url, false).refresh()
                fail("Invalid taxonomy must reject the full refresh")
            } catch (_: IllegalArgumentException) {
                assertEquals(before, db.dao().all())
            }
        } finally { db.close() }
    }

    @Test fun taxonomyTitlesAreNormalizedAndSubpathManifestWorks() {
        val json = fixture()
        val notes = json.getJSONArray("notes")
        val project = (0 until notes.length()).map { notes.getJSONObject(it) }.first { it.optString("projectId") == "nanobot" }
        project.put("projectTitle", "한글")
        assertEquals("한글", parse(json).single { it.id == project.getString("id") }.projectTitle)
        val subpath = fixture()
        val productionRoot = "https://ax.msalt.net/"
        val mountedRoot = "https://msaltnet.github.io/ax-notes-public/"
        // Android JSONObject escapes URL slashes when serializing. Rebase parsed URL
        // values rather than replacing an unescaped URL in the serialized JSON.
        fun rebase(value: JSONObject, key: String) {
            if (value.isNull(key)) return
            val original = value.getString(key)
            assertTrue(original.startsWith(productionRoot))
            value.put(key, mountedRoot + original.removePrefix(productionRoot))
        }
        rebase(subpath.getJSONObject("author"), "aboutUrl")
        val mountedNotes = subpath.getJSONArray("notes")
        for (i in 0 until mountedNotes.length()) {
            val note = mountedNotes.getJSONObject(i)
            listOf("canonicalUrl", "detailUrl", "projectUrl").forEach { rebase(note, it) }
            assertTrue(note.getString("detailUrl").startsWith(mountedRoot + "app/v1/notes/"))
        }
        assertEquals(9, FeedContract.manifest(subpath.toString(), mountedRoot + "app/v1/manifest.json").notes.size)
    }

    companion object {
        private val fields = listOf("projectId", "projectTitle", "projectOrder", "seriesId", "seriesTitle", "seriesOrder")
    }
}
