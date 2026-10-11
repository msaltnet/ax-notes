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
        // Multiple records are a legacy fixture, not a supported new-save path.
        val first = legacyMemo("legacy-first", "  첫 메모\r\ne\u0301  ", "일하는 방식", 123L)
        val second = legacyMemo("legacy-second", "", "두 번째 생각", 456L)
        personal.dao.putMemo(first); personal.dao.putMemo(second)
        personal.toggleBookmark("hello"); assertNull(personal.dao.bookmark("hello")); assertEquals(2,personal.dao.memosFor("hello").size)
        files[url] = JSONObject(manifest()).put("notes",org.json.JSONArray()).toString(); repository.refresh()
        assertFalse(cache.dao().article("hello")!!.available); assertEquals(2,personal.dao.memosFor("hello").size)
        repository.clear(); assertNull(cache.dao().article("hello")); assertEquals("한국어 제목",personal.snapshot("hello").first)
        personal.saveMemo(first.id,"hello","수정하려는 제목","캐시 없어도 수정 가능")
        assertEquals(2,personal.dao.memosFor("hello").size)
        val edited = personal.dao.memo(first.id)!!
        assertEquals(first.title, edited.title)
        assertEquals(first.createdAt, edited.createdAt)
        assertEquals("캐시 없어도 수정 가능", edited.body)
        assertEquals(second, personal.dao.memo(second.id))
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
    @Test fun bookmarkUniquenessDoesNotDestroyLegacyMultipleMemos(): Unit = runBlocking {
        repository.refresh(); personal.toggleBookmark("hello"); val bookmark = personal.dao.bookmark("hello")!!
        personal.dao.putBookmark(bookmark.copy(titleSnapshot = "new")); assertEquals("new",personal.dao.bookmark("hello")!!.titleSnapshot)
        val legacy = (0..2).map { legacyMemo("legacy-$it", "제목 $it", "메모 $it", it + 1L) }
        legacy.forEach { personal.dao.putMemo(it) }
        assertEquals(legacy.toSet(), personal.dao.memosFor("hello").toSet())
    }

    @Test fun secondNewMemoIsRejectedWithoutOverwritingTheFirst(): Unit = runBlocking {
        repository.refresh()
        val firstId = personal.saveMemo(null, "hello", "", "첫 본문", "first-draft")
        val first = personal.dao.memo(firstId)!!
        for (draftId in listOf<String?>(null, "other-draft")) {
            try {
                personal.saveMemo(null, "hello", "", "저장되면 안 되는 두 번째 본문", draftId)
                fail("Another new memo for the same article must be rejected")
            } catch (_: IllegalArgumentException) { }
        }
        assertEquals(listOf(first), personal.dao.memosFor("hello"))
        assertNull(personal.dao.memo("other-draft"))
    }

    @Test fun stableDraftRetriesAndExplicitEditsKeepTheSameRecordAndCreatedAt(): Unit = runBlocking {
        repository.refresh()
        val id = personal.saveMemo(null, "hello", "", "첫 저장", "same-draft")
        val first = personal.dao.memo(id)!!
        assertEquals(id, personal.saveMemo(null, "hello", "", "다시 저장", "same-draft"))
        assertEquals(id, personal.saveMemo(id, "hello", "제목은 바뀌지 않음", "명시적 수정"))
        val edited = personal.dao.memosFor("hello").single()
        assertEquals(id, edited.id)
        assertEquals(first.createdAt, edited.createdAt)
        assertEquals(first.title, edited.title)
        assertEquals("명시적 수정", edited.body)
    }

    @Test fun legacySiblingDoesNotBlockAnExistingDraftRetry(): Unit = runBlocking {
        repository.refresh()
        val first = legacyMemo("legacy-one", "  옛 제목\r\ne\u0301  ", "원래 본문", 123L)
        val second = legacyMemo("legacy-two", "다른 제목", "건드리지 않을 본문", 456L)
        personal.dao.putMemo(first); personal.dao.putMemo(second)
        assertEquals(first.id, personal.saveMemo(null, "hello", "새 제목", "재시도 본문", first.id))
        val saved = personal.dao.memo(first.id)!!
        assertArrayEquals(first.title.toByteArray(Charsets.UTF_8), saved.title.toByteArray(Charsets.UTF_8))
        assertEquals(first.createdAt, saved.createdAt)
        assertEquals("재시도 본문", saved.body)
        assertEquals(second, personal.dao.memo(second.id))
    }

    @Test fun concurrentNewDraftsAcrossRepositoryInstancesCreateOnlyOneMemo(): Unit = runBlocking {
        repository.refresh()
        val otherRepository = PersonalRepository(cache.dao(), privateDb)
        val start = CompletableDeferred<Unit>()
        val results = listOf(personal, otherRepository).mapIndexed { index, repo ->
            async(Dispatchers.IO) {
                start.await()
                try {
                    repo.saveMemo(null, "hello", "", "경쟁하는 본문 $index", "draft-$index")
                    true
                } catch (_: IllegalArgumentException) { false }
            }
        }
        start.complete(Unit)
        assertEquals(1, results.awaitAll().count { it })
        assertEquals(1, personal.dao.memosFor("hello").size)
    }

    @Test fun deletedMemoEditAndCrossArticleDraftReuseAreRejected(): Unit = runBlocking {
        repository.refresh()
        val existing = personal.saveMemo(null, "hello", "", "보존할 본문", "existing-draft")
        val before = personal.dao.memo(existing)!!
        personal.dao.putBookmark(Bookmark("other", "다른 글", "https://ax.msalt.net/notes/other/", 1L))
        try {
            personal.saveMemo(null, "other", "", "연결을 바꾸지 않음", existing)
            fail("A stable draft ID must never change its article")
        } catch (_: IllegalArgumentException) { }
        try {
            personal.saveMemo("deleted-id", "hello", "", "삭제된 메모를 부활시키지 않음")
            fail("Editing a deleted ID must not create another memo")
        } catch (_: IllegalArgumentException) { }
        assertEquals(before, personal.dao.memo(existing))
        assertTrue(personal.dao.memosFor("other").isEmpty())
        assertNull(personal.dao.memo("deleted-id"))
    }

    @Test fun separateArticlesCanEachHaveOneMemoAndDeletionAllowsReplacement(): Unit = runBlocking {
        repository.refresh()
        val first = personal.saveMemo(null, "hello", "", "첫 글 메모")
        personal.dao.putBookmark(Bookmark("other", "다른 글", "https://ax.msalt.net/notes/other/", 1L))
        personal.saveMemo(null, "other", "", "다른 글 메모")
        personal.deleteMemo(first)
        val replacement = personal.saveMemo(null, "hello", "", "대체 메모")
        assertNotEquals(first, replacement)
        assertEquals(replacement, personal.dao.memosFor("hello").single().id)
        assertEquals(1, personal.dao.memosFor("other").size)
    }

    @Test fun reopeningSchemaOnePreservesAllLegacyMemoBytesWithoutMigration(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "legacy-multiple-memos-${System.nanoTime()}.db"
        val legacy = listOf(
            legacyMemo("old-one", "  옛 제목\r\ne\u0301  ", "첫 본문\r\n보존", 123L),
            legacyMemo("old-two", "", "\n두 번째 본문", 456L)
        )
        try {
            Room.databaseBuilder(context, PersonalDatabase::class.java, name).allowMainThreadQueries().build().let { disk ->
                try { legacy.forEach { disk.dao().putMemo(it) } } finally { disk.close() }
            }
            Room.databaseBuilder(context, PersonalDatabase::class.java, name).allowMainThreadQueries().build().let { reopened ->
                try {
                    assertEquals(1, reopened.openHelper.readableDatabase.version)
                    assertEquals(legacy.toSet(), reopened.dao().memosFor("hello").toSet())
                } finally { reopened.close() }
            }
        } finally { context.deleteDatabase(name) }
    }

    private fun legacyMemo(id: String, title: String, body: String, time: Long) = Memo(
        id, "hello", "한국어 제목", "https://ax.msalt.net/notes/hello/", title, body, time, time
    )
}
