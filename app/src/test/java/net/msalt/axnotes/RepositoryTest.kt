package net.msalt.axnotes

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import net.msalt.axnotes.data.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RepositoryTest {
    private lateinit var cache: ContentDatabase
    private lateinit var privateDb: PersonalDatabase
    private lateinit var repository: ContentRepository
    private lateinit var personal: PersonalRepository
    private val files = mutableMapOf<String,String>()
    private val url = "https://ax.msalt.net/app/v1/manifest.json"
    private val a = "a".repeat(64); private val b = "b".repeat(64)
    private fun manifest(revision: String = a) = """{"schemaVersion":1,"generatedAt":"2026-10-07T00:00:00Z","author":{"name":"AX","aboutUrl":"https://ax.msalt.net/about/","channels":[]},"notes":[{"id":"hello","title":"한국어 제목","description":"설명 100%_","publishedAt":"2026-10-07","canonicalUrl":"https://ax.msalt.net/notes/hello/","detailUrl":"https://ax.msalt.net/app/v1/notes/hello/$revision.json","revision":"$revision"}]}"""
    private fun body(revision: String, content: String) = JSONObject().put("schemaVersion",1).put("id","hello").put("revision",revision).put("bodyHtml","<p>$content</p>").put("bodyText",content).toString()
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        cache = Room.inMemoryDatabaseBuilder(context, ContentDatabase::class.java).allowMainThreadQueries().build()
        privateDb = Room.inMemoryDatabaseBuilder(context, PersonalDatabase::class.java).allowMainThreadQueries().build()
        val source = object : FeedSource { override suspend fun get(url: String, limit: Int) = files[url] ?: error("offline") }
        repository = ContentRepository(cache, source, url, false)
        personal = PersonalRepository(cache.dao(), privateDb)
        files[url] = manifest(); files["https://ax.msalt.net/app/v1/notes/hello/$a.json"] = body(a,"오프라인으로 읽는 본문")
    }
    @After fun close() { cache.close(); privateDb.close() }
    @Test fun failedNewBodyKeepsSeparateOldRevisionAndReadableCache(): Unit = runBlocking {
        assertEquals(0,repository.refresh()); files[url] = manifest(b)
        assertEquals(1,repository.refresh())
        val article = cache.dao().article("hello")!!
        assertEquals(b,article.wantedRevision); assertEquals(a,article.cachedRevision); assertTrue(article.bodyText!!.contains("오프라인"))
        files[article.detailUrl] = body(a,"wrong revision"); assertEquals(1, repository.refresh()); assertEquals(a,cache.dao().article("hello")!!.cachedRevision)
        files[article.detailUrl] = body(b,"새 본문"); assertEquals(0,repository.refresh()); assertEquals(b,cache.dao().article("hello")!!.cachedRevision)
    }
    @Test fun malformedManifestNeverDeletesOrMarksUnavailable(): Unit = runBlocking {
        repository.refresh(); files[url] = "not-json"
        try { repository.refresh(); fail() } catch (_: Exception) { }
        assertTrue(cache.dao().article("hello")!!.available)
    }
    @Test fun removalUnbookmarkAndCacheResetPreserveMultipleLinkedMemos(): Unit = runBlocking {
        repository.refresh(); personal.toggleBookmark("hello")
        val first = personal.saveMemo(null,"hello","첫 메모","일하는 방식"); val second = personal.saveMemo(null,"hello","","두 번째 생각")
        assertNotEquals(first,second); personal.toggleBookmark("hello"); assertNull(personal.dao.bookmark("hello")); assertEquals(2,personal.dao.memosFor("hello").size)
        files[url] = JSONObject(manifest()).put("notes",org.json.JSONArray()).toString(); repository.refresh()
        assertFalse(cache.dao().article("hello")!!.available); assertEquals(2,personal.dao.memosFor("hello").size)
        repository.clear(); assertNull(cache.dao().article("hello")); assertEquals("한국어 제목",personal.snapshot("hello").first)
        personal.saveMemo(first,"hello","수정","캐시 없어도 수정 가능"); assertEquals(2,personal.dao.memosFor("hello").size)
    }
    @Test fun orphanMemoAndEmptyBodyAreRejected(): Unit = runBlocking {
        try { personal.saveMemo(null,"missing","","본문"); fail() } catch (_: Exception) { }
        repository.refresh()
        try { personal.saveMemo(null,"hello",""," "); fail() } catch (_: Exception) { }
        assertTrue(personal.dao.memosFor("hello").isEmpty())
    }
    @Test fun koreanAndSpecialCharactersSearchWorksOffline(): Unit = runBlocking {
        repository.refresh(); personal.saveMemo(null,"hello","적용","일하는 방식과 100%_ 그리고 '따옴표' \\ 경로")
        files.clear()
        assertEquals(1, cache.dao().search(FeedContract.searchPattern("국어")).size)
        assertEquals(1, cache.dao().search(FeedContract.searchPattern("프라인")).size)
        assertEquals(1, personal.dao.search(FeedContract.searchPattern("하는 방")).size)
        assertEquals(1, personal.dao.search(FeedContract.searchPattern("100%_")).size)
        assertEquals(0, personal.dao.search(FeedContract.searchPattern("100X_")).size)
        assertEquals(1, personal.dao.search(FeedContract.searchPattern("'따옴표'")).size)
        assertEquals(1, personal.dao.search(FeedContract.searchPattern("\\")).size)
    }
    @Test fun requestedDetailDoesNotWaitForRefreshBatch(): Unit = runBlocking {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val root = JSONObject(manifest()); val notes = root.getJSONArray("notes")
        notes.put(JSONObject(notes.getJSONObject(0).toString()).put("id","second").put("canonicalUrl","https://ax.msalt.net/notes/second/").put("detailUrl","https://ax.msalt.net/app/v1/notes/second/$a.json"))
        files[url] = root.toString()
        files["https://ax.msalt.net/app/v1/notes/second/$a.json"] = JSONObject(body(a,"우선 다운로드")).put("id","second").toString()
        val source = object : FeedSource {
            override suspend fun get(target: String, limit: Int): String {
                if(target.endsWith("/hello/$a.json")) { entered.complete(Unit); release.await() }
                return files.getValue(target)
            }
        }
        val repo = ContentRepository(cache,source,url,false)
        val refreshing = async { repo.refresh() }
        withTimeout(5000) { entered.await(); repo.load("second") }
        assertEquals(a,cache.dao().article("second")!!.cachedRevision)
        release.complete(Unit); refreshing.await()
    }
    @Test fun bookmarkUniquenessDoesNotConstrainMemos(): Unit = runBlocking {
        repository.refresh(); personal.toggleBookmark("hello"); val bookmark = personal.dao.bookmark("hello")!!
        personal.dao.putBookmark(bookmark.copy(titleSnapshot = "new")); assertEquals("new",personal.dao.bookmark("hello")!!.titleSnapshot)
        repeat(3) { personal.saveMemo(null,"hello","","메모 $it") }
        assertEquals(3,personal.dao.memosFor("hello").size)
    }
}
