package net.msalt.axnotes.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

interface FeedSource { suspend fun get(url: String, limit: Int): String }
class HttpFeedSource : FeedSource {
    override suspend fun get(url: String, limit: Int): String = withContext(Dispatchers.IO) {
        require(SafeUrl.https(url))
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15000; connection.readTimeout = 20000
            connection.instanceFollowRedirects = false; connection.setRequestProperty("Accept", "application/json")
            require(connection.responseCode == 200) { "콘텐츠 서버 응답 ${connection.responseCode}" }
            require(connection.contentLengthLong <= limit) { "응답 크기 제한 초과" }
            connection.inputStream.use { FeedContract.readBounded(it, limit) }
        } finally { connection.disconnect() }
    }
}
class SampleFeedSource(private val context: Context, private val manifestUrl: String) : FeedSource {
    override suspend fun get(url: String, limit: Int): String = withContext(Dispatchers.IO) {
        val path = if (url == manifestUrl) "manifest.json" else url.substringAfter("/app/v1/")
        require(!path.contains("..") && !path.startsWith('/'))
        context.assets.open("sample/$path").use { FeedContract.readBounded(it, limit) }
    }
}
class ContentRepository(val db: ContentDatabase, private val source: FeedSource, val manifestUrl: String, val sample: Boolean) {
    private val gate = Mutex()
    val dao = db.dao()
    suspend fun refresh(): Int = withContext(Dispatchers.IO) { gate.withLock {
        val manifest = FeedContract.manifest(source.get(manifestUrl, Limits.MANIFEST), manifestUrl)
        db.withTransaction {
            val old = dao.all().associateBy { it.id }
            dao.markUnavailable()
            dao.put(manifest.notes.map { incoming ->
                old[incoming.id]?.let { previous -> incoming.copy(cachedRevision = previous.cachedRevision, bodyHtml = previous.bodyHtml, bodyText = previous.bodyText, cachedAt = previous.cachedAt) } ?: incoming
            })
            dao.setFeed(FeedState(fetchedAt = System.currentTimeMillis(), authorName = manifest.authorName, aboutUrl = manifest.aboutUrl, channelsJson = manifest.channelsJson, sample = sample))
        }
        var failed = 0
        for (note in manifest.notes) { if (dao.article(note.id)?.cachedRevision != note.wantedRevision) { try { download(note.id) } catch (e: Exception) { if (e is CancellationException) throw e; failed++ } } }
        failed
    } }
    suspend fun load(id: String) = withContext(Dispatchers.IO) { download(id) }
    private suspend fun download(id: String) {
        val article = dao.article(id) ?: return
        if (!article.available || (article.cachedRevision == article.wantedRevision && article.bodyHtml != null)) return
        val body = FeedContract.body(source.get(article.detailUrl, Limits.DETAIL), article)
        db.withTransaction {
            val current = dao.article(id) ?: return@withTransaction
            if (current.wantedRevision != body.revision) return@withTransaction
            val oldBytes = (current.bodyHtml?.toByteArray()?.size ?: 0) + (current.bodyText?.toByteArray()?.size ?: 0)
            require(dao.cacheBytes() - oldBytes + body.html.toByteArray().size + body.text.toByteArray().size <= Limits.CACHE) { "본문 캐시 한도에 도달했습니다. 설정에서 캐시를 정리하세요" }
            dao.cache(id, body.revision, body.html, body.text, System.currentTimeMillis())
        }
    }
    suspend fun clear() = gate.withLock { db.withTransaction { dao.clearArticles(); dao.clearFeed() } }
}
internal object PersonalDataGate { val mutex = Mutex() }
class PersonalRepository(private val content: ContentDao, val db: PersonalDatabase) {
    val dao = db.dao()
    private val gate = PersonalDataGate.mutex
    suspend fun snapshot(id: String): Pair<String, String> {
        require(id.isNotBlank())
        content.article(id)?.let { return it.title to it.canonicalUrl }
        dao.bookmark(id)?.let { return it.titleSnapshot to it.urlSnapshot }
        dao.memosFor(id).firstOrNull()?.let { return it.titleSnapshot to it.urlSnapshot }
        dao.reminderFor(id)?.let { return it.titleSnapshot to it.urlSnapshot }
        error("연결할 글을 찾을 수 없습니다")
    }
    suspend fun toggleBookmark(id: String) = gate.withLock {
        if (dao.bookmark(id) != null) dao.removeBookmark(id) else {
            val (title, url) = snapshot(id); dao.putBookmark(Bookmark(id, title, url, System.currentTimeMillis()))
        }
    }
    suspend fun deleteMemo(id: String) = gate.withLock { dao.deleteMemo(id) }
    suspend fun saveMemo(id: String?, articleId: String, title: String, body: String, newMemoId: String? = null): String = gate.withLock {
        require(body.isNotBlank()) { "메모 내용을 입력하세요" }
        require(title.length <= 500 && body.length <= 100000) { "메모 길이 제한을 초과했습니다" }
        val (snapshotTitle, url) = snapshot(articleId)
        val old = (id ?: newMemoId)?.let { dao.memo(it) }
        require(id == null || old != null) { "삭제된 메모입니다" }
        require(old == null || old.articleId == articleId) { "메모의 연결 글을 변경할 수 없습니다" }
        val now = System.currentTimeMillis(); val savedId = old?.id ?: newMemoId ?: UUID.randomUUID().toString()
        dao.putMemo(Memo(savedId, articleId, snapshotTitle, url, FeedContract.normalize(title.trim()), FeedContract.normalize(body), old?.createdAt ?: now, now))
        savedId
    }
}
