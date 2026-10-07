package net.msalt.axnotes

import net.msalt.axnotes.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ContractTest {
    private val url = "https://ax.msalt.net/app/v1/manifest.json"
    private val revision = "a".repeat(64)
    private fun fixture() = """{"schemaVersion":1,"generatedAt":"2026-10-07T00:00:00Z","author":{"name":"AX Notes","aboutUrl":"https://ax.msalt.net/about/","channels":[]},"notes":[{"id":"hello","title":"한국어 글","description":"요약","publishedAt":"2026-10-07","updatedAt":null,"canonicalUrl":"https://ax.msalt.net/notes/hello/","detailUrl":"https://ax.msalt.net/app/v1/notes/hello/$revision.json","revision":"$revision","projectUrl":null}]}"""
    @Test fun contractAcceptsProductionAndSubpath() {
        assertEquals("한국어 글", FeedContract.manifest(fixture(), url).notes.single().title)
        val sub = fixture().replace("https://ax.msalt.net/", "https://msaltnet.github.io/ax-notes-public/")
        assertEquals(1, FeedContract.manifest(sub, "https://msaltnet.github.io/ax-notes-public/app/v1/manifest.json").notes.size)
    }
    @Test fun unsupportedSchemaDuplicateIdsAndForeignDetailAreRejected() {
        assertThrows(Exception::class.java) { FeedContract.manifest(fixture().replace("\"schemaVersion\":1", "\"schemaVersion\":2"), url) }
        val json = JSONObject(fixture()); json.getJSONArray("notes").put(json.getJSONArray("notes").getJSONObject(0))
        assertThrows(Exception::class.java) { FeedContract.manifest(json.toString(), url) }
        assertThrows(Exception::class.java) { FeedContract.manifest(fixture().replace("https://ax.msalt.net/app/v1/notes", "https://evil.example/app/v1/notes"), url) }
    }
    @Test fun limitsAreEnforcedEvenWithoutContentLength() {
        assertEquals("abcd", FeedContract.readBounded(ByteArrayInputStream("abcd".toByteArray()), 4))
        assertThrows(IllegalArgumentException::class.java) { FeedContract.readBounded(ByteArrayInputStream("abcde".toByteArray()), 4) }
    }
    @Test fun maliciousLinksAreRejected() {
        listOf("javascript:alert(1)", "file:///tmp/a", "content://file/a", "intent://a", "http://example.com", "https://user:pass@example.com", "https://example.com:444/path").forEach { assertFalse(it, SafeUrl.https(it)) }
        assertTrue(SafeUrl.https("https://ax.msalt.net/about/"))
    }
    @Test fun detailsRequireExactIdAndRevisionAndStripActiveContent() {
        val article = FeedContract.manifest(fixture(), url).notes.single()
        val body = JSONObject().put("schemaVersion",1).put("id","hello").put("revision",revision).put("bodyText","본문").put("bodyHtml","<p onclick='steal()'>본문</p><script>alert(1)</script><iframe src='https://evil.example'></iframe><a href='javascript:steal()'>bad</a><img src='file:///secret'>")
        val result = FeedContract.body(body.toString(), article)
        assertFalse(result.html.contains("script")); assertFalse(result.html.contains("onclick")); assertFalse(result.html.contains("iframe")); assertFalse(result.html.contains("file:")); assertFalse(result.html.contains("javascript:"))
        body.put("revision", "b".repeat(64)); assertThrows(Exception::class.java) { FeedContract.body(body.toString(), article) }
    }
    @Test fun bundledPublicFeedUsesTheExactAndroidContract() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val manifest = FeedContract.manifest(context.assets.open("sample/manifest.json").bufferedReader().use { it.readText() }, url)
        assertEquals(8,manifest.notes.size)
        manifest.notes.forEach { article ->
            val path = "sample/" + article.detailUrl.substringAfter("/app/v1/")
            val body = FeedContract.body(context.assets.open(path).bufferedReader().use { it.readText() },article)
            assertEquals(article.id,body.id); assertTrue(body.text.isNotBlank()); assertTrue(body.html.isNotBlank())
        }
    }
    @Test fun searchEscapesLiteralWildcardsAndNormalizesKorean() {
        assertEquals("%100\\%\\_\\\\'%", FeedContract.searchPattern(" 100%_\\' "))
        assertEquals("%한국%", FeedContract.searchPattern(" 한국 "))
        assertEquals(202, FeedContract.searchPattern("가".repeat(400)).length)
    }
}
