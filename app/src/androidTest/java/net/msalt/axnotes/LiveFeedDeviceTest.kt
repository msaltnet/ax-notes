package net.msalt.axnotes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import net.msalt.axnotes.data.HttpFeedSource
import net.msalt.axnotes.data.Limits
import net.msalt.axnotes.data.FeedContract
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real-service checks, run separately from deterministic unit tests on a disposable emulator. */
@RunWith(AndroidJUnit4::class)
class LiveFeedDeviceTest {
    private val graph get() = ApplicationProvider.getApplicationContext<AxApplication>().graph
    @Test fun refreshAllLiveBodiesAndPersistPersonalRecords() = runBlocking {
        assertFalse(graph.sampleMode)
        val repo = graph.content()
        assertFalse(repo.sample)
        assertEquals(0, repo.refresh())
        val articles = repo.dao.all().filter { it.available }
        assertTrue("Live feed must contain published articles", articles.isNotEmpty())
        assertFalse(repo.dao.feed()!!.sample)
        articles.forEach {
            assertEquals(it.id, it.wantedRevision, it.cachedRevision)
            assertFalse(it.bodyHtml.isNullOrBlank())
            assertFalse(it.bodyText.isNullOrBlank())
        }
        val article = articles.first()
        if (graph.personal.dao.bookmark(article.id) == null) graph.personal.toggleBookmark(article.id)
        graph.personal.saveMemo(null, article.id, "Live feed regression", "Live feed persistence marker", "live-feed-regression")
        assertEquals(0, repo.refresh())
        assertNotNull(graph.personal.dao.bookmark(article.id))
        assertEquals("Live feed persistence marker", graph.personal.dao.memo("live-feed-regression")!!.body)
    }
    @Test fun reopenCachedBodiesAndPersonalRecordsWithNetworkBlocked() = runBlocking {
        // The harness disables connectivity before starting this separate process.
        val repo = graph.content()
        try {
            HttpFeedSource().get(BuildConfig.MANIFEST_URL, Limits.MANIFEST)
            fail("Harness must block actual network access")
        } catch (_: java.io.IOException) { }
        val articles = repo.dao.all().filter { it.available }
        assertTrue(articles.isNotEmpty())
        articles.forEach {
            repo.load(it.id)
            assertFalse(repo.dao.article(it.id)!!.bodyText.isNullOrBlank())
        }
        val memo = graph.personal.dao.memo("live-feed-regression")!!
        assertEquals("Live feed persistence marker", memo.body)
        assertNotNull(graph.personal.dao.bookmark(memo.articleId))
        assertEquals(1, graph.personal.dao.search(FeedContract.searchPattern("persistence marker")).size)
    }
}
