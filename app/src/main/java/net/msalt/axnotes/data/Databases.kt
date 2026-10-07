package net.msalt.axnotes.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey val id: String, val title: String, val description: String,
    val publishedAt: String, val updatedAt: String?, val canonicalUrl: String,
    val detailUrl: String, val wantedRevision: String, val projectUrl: String?,
    val cachedRevision: String? = null, val bodyHtml: String? = null, val bodyText: String? = null,
    val cachedAt: Long? = null, val available: Boolean = true
)
@Entity(tableName = "feed")
data class FeedState(@PrimaryKey val key: Int = 1, val fetchedAt: Long, val authorName: String, val aboutUrl: String, val channelsJson: String, val sample: Boolean)
@Dao
interface ContentDao {
    @Query("SELECT * FROM articles WHERE available = 1 ORDER BY publishedAt DESC, id") fun observeArticles(): Flow<List<Article>>
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC, id") suspend fun all(): List<Article>
    @Query("SELECT * FROM articles WHERE id = :id") suspend fun article(id: String): Article?
    @Query("SELECT * FROM articles WHERE id = :id") fun observeArticle(id: String): Flow<Article?>
    @Upsert suspend fun put(articles: List<Article>)
    @Query("UPDATE articles SET available = 0") suspend fun markUnavailable()
    @Query("UPDATE articles SET bodyHtml = :html, bodyText = :text, cachedRevision = :revision, cachedAt = :now WHERE id = :id AND wantedRevision = :revision") suspend fun cache(id: String, revision: String, html: String, text: String, now: Long): Int
    @Query("SELECT COALESCE(SUM(LENGTH(CAST(bodyHtml AS BLOB)) + LENGTH(CAST(bodyText AS BLOB))), 0) FROM articles") suspend fun cacheBytes(): Long
    @Query("SELECT * FROM articles WHERE title LIKE :pattern ESCAPE '\\' OR description LIKE :pattern ESCAPE '\\' OR bodyText LIKE :pattern ESCAPE '\\' ORDER BY CASE WHEN title LIKE :pattern ESCAPE '\\' THEN 0 ELSE 1 END, publishedAt DESC LIMIT :limit OFFSET :offset") suspend fun search(pattern: String, limit: Int = 50, offset: Int = 0): List<Article>
    @Query("SELECT * FROM feed WHERE `key` = 1") fun observeFeed(): Flow<FeedState?>
    @Query("SELECT * FROM feed WHERE `key` = 1") suspend fun feed(): FeedState?
    @Upsert suspend fun setFeed(state: FeedState)
    @Query("DELETE FROM articles") suspend fun clearArticles()
    @Query("DELETE FROM feed") suspend fun clearFeed()
}
@Database(entities = [Article::class, FeedState::class], version = 1, exportSchema = true)
abstract class ContentDatabase : RoomDatabase() { abstract fun dao(): ContentDao }

@Entity(tableName = "bookmarks")
data class Bookmark(@PrimaryKey val articleId: String, val titleSnapshot: String, val urlSnapshot: String, val createdAt: Long)
@Entity(tableName = "memos", indices = [Index("articleId")])
data class Memo(@PrimaryKey val id: String, val articleId: String, val titleSnapshot: String, val urlSnapshot: String, val title: String, val body: String, val createdAt: Long, val updatedAt: Long)
@Entity(tableName = "reminders", indices = [Index(value = ["articleId"], unique = true)])
data class Reminder(@PrimaryKey val id: String, val articleId: String, val titleSnapshot: String, val urlSnapshot: String, val dueAt: Long, val state: String, val generation: Long, val notifiedAt: Long? = null)
@Dao
interface PersonalDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC") fun observeBookmarks(): Flow<List<Bookmark>>
    @Query("SELECT * FROM memos ORDER BY updatedAt DESC") fun observeMemos(): Flow<List<Memo>>
    @Query("SELECT * FROM reminders WHERE state != 'cancelled' ORDER BY dueAt") fun observeReminders(): Flow<List<Reminder>>
    @Query("SELECT * FROM bookmarks WHERE articleId = :id") suspend fun bookmark(id: String): Bookmark?
    @Query("SELECT * FROM memos WHERE id = :id") suspend fun memo(id: String): Memo?
    @Query("SELECT * FROM memos WHERE articleId = :id ORDER BY updatedAt DESC") suspend fun memosFor(id: String): List<Memo>
    @Query("SELECT * FROM reminders WHERE articleId = :id") suspend fun reminderFor(id: String): Reminder?
    @Query("SELECT * FROM reminders WHERE id = :id") suspend fun reminder(id: String): Reminder?
    @Query("SELECT * FROM reminders WHERE state IN ('scheduled','blocked','posting','posting_summary') ORDER BY dueAt") suspend fun activeReminders(): List<Reminder>
    @Upsert suspend fun putBookmark(value: Bookmark)
    @Upsert suspend fun putMemo(value: Memo)
    @Upsert suspend fun putReminder(value: Reminder)
    @Query("DELETE FROM bookmarks WHERE articleId = :id") suspend fun removeBookmark(id: String)
    @Query("DELETE FROM memos WHERE id = :id") suspend fun deleteMemo(id: String)
    @Query("UPDATE reminders SET state = 'opened' WHERE articleId = :articleId AND state = 'delivered'") suspend fun markOpened(articleId: String)
    @Query("SELECT * FROM memos WHERE title LIKE :pattern ESCAPE '\\' OR body LIKE :pattern ESCAPE '\\' OR titleSnapshot LIKE :pattern ESCAPE '\\' ORDER BY CASE WHEN title LIKE :pattern ESCAPE '\\' THEN 0 ELSE 1 END, updatedAt DESC LIMIT :limit OFFSET :offset") suspend fun search(pattern: String, limit: Int = 50, offset: Int = 0): List<Memo>
    @Query("DELETE FROM bookmarks") suspend fun clearBookmarks()
    @Query("DELETE FROM memos") suspend fun clearMemos()
    @Query("DELETE FROM reminders") suspend fun clearReminders()
}
@Database(entities = [Bookmark::class, Memo::class, Reminder::class], version = 1, exportSchema = true)
abstract class PersonalDatabase : RoomDatabase() { abstract fun dao(): PersonalDao }
