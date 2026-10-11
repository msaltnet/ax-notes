package net.msalt.axnotes.data

import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.io.InputStream
import java.net.URI
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate

object Limits {
    const val MANIFEST = 2 * 1024 * 1024
    const val DETAIL = 1024 * 1024
    const val CACHE = 50L * 1024 * 1024
    const val NOTES = 1000
}
data class Manifest(val notes: List<Article>, val authorName: String, val aboutUrl: String, val channelsJson: String)
data class Body(val id: String, val revision: String, val html: String, val text: String)
object SafeUrl {
    fun https(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.port in listOf(-1, 443)
    }.getOrDefault(false)
    fun sameFeedOrigin(value: String, manifest: String): Boolean = runCatching {
        val uri = URI(value); val base = URI(manifest)
        https(value) && uri.host == base.host && uri.port == base.port && uri.path.startsWith(base.path.substringBeforeLast('/') + "/notes/") && uri.query == null && uri.fragment == null && !uri.path.contains("..")
    }.getOrDefault(false)
}
object FeedContract {
    private val idPattern = Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,127}")
    private val revisionPattern = Regex("[a-f0-9]{64}")
    fun readBounded(input: InputStream, max: Int): String {
        val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
        while (true) { val read = input.read(buffer); if (read < 0) break; require(out.size() + read <= max) { "응답 크기 제한 초과" }; out.write(buffer, 0, read) }
        return out.toString(Charsets.UTF_8.name())
    }
    fun manifest(raw: String, url: String): Manifest {
        require(raw.toByteArray().size <= Limits.MANIFEST)
        val root = JSONObject(raw); require(root.getInt("schemaVersion") == 1) { "지원하지 않는 콘텐츠 버전" }
        Instant.parse(root.getString("generatedAt"))
        val author = root.getJSONObject("author")
        val about = author.getString("aboutUrl"); require(SafeUrl.https(about))
        val name = author.getString("name").also { require(it.isNotBlank() && it.length <= 200) }
        val channels = author.getJSONArray("channels"); require(channels.length() <= 20)
        for (i in 0 until channels.length()) { val channel = channels.getJSONObject(i); require(channel.getString("label").length <= 100 && SafeUrl.https(channel.getString("url"))) }
        val notes = root.getJSONArray("notes"); require(notes.length() <= Limits.NOTES)
        val result = (0 until notes.length()).map { index ->
            val n = notes.getJSONObject(index)
            val id = n.getString("id"); require(idPattern.matches(id))
            val revision = n.getString("revision"); require(revisionPattern.matches(revision))
            val detail = n.getString("detailUrl"); require(SafeUrl.sameFeedOrigin(detail, url))
            require(URI(detail).path.endsWith("/$id/$revision.json"))
            val canonical = n.getString("canonicalUrl"); require(SafeUrl.https(canonical))
            val project = n.optionalString("projectUrl"); require(project == null || SafeUrl.https(project))
            val projectId = n.taxonomyId("projectId")
            val projectTitle = n.taxonomyTitle("projectTitle")
            val projectOrder = n.taxonomyOrder("projectOrder")
            val seriesId = n.taxonomyId("seriesId")
            val seriesTitle = n.taxonomyTitle("seriesTitle")
            val seriesOrder = n.taxonomyOrder("seriesOrder")
            requireTaxonomy(projectId, projectTitle, projectOrder)
            requireTaxonomy(seriesId, seriesTitle, seriesOrder)
            require(projectId == null || seriesId == null) { "글은 하나의 모음에만 연결할 수 있습니다" }
            val date = n.getString("publishedAt"); validDate(date)
            val updated = n.optionalString("updatedAt"); if (updated != null) validDate(updated)
            val title = n.getString("title"); val description = n.getString("description")
            require(title.isNotBlank() && title.length <= 500 && description.length <= 5000)
            Article(id, normalize(title), normalize(description), date, updated, canonical, detail, revision, project,
                projectId = projectId, projectTitle = projectTitle, projectOrder = projectOrder,
                seriesId = seriesId, seriesTitle = seriesTitle, seriesOrder = seriesOrder)
        }
        require(result.map { it.id }.distinct().size == result.size) { "중복 글 ID" }
        return Manifest(result, name, about, channels.toString())
    }
    fun body(raw: String, expected: Article): Body {
        require(raw.toByteArray().size <= Limits.DETAIL)
        val n = JSONObject(raw); require(n.getInt("schemaVersion") == 1)
        require(n.getString("id") == expected.id && n.getString("revision") == expected.wantedRevision) { "목록과 본문 버전이 다릅니다" }
        val html = n.getString("bodyHtml"); val text = n.getString("bodyText")
        // Defense in depth even when the publisher already sanitized the article.
        val safe = Safelist.relaxed().addTags("figure", "figcaption", "section", "article", "hr", "del", "s", "kbd")
            .removeProtocols("a", "href", "http", "ftp", "mailto").removeProtocols("img", "src", "http")
            .addAttributes("img", "width", "height", "loading").addAttributes("th", "scope")
        val clean = Jsoup.clean(html, expected.canonicalUrl, safe)
        return Body(expected.id, expected.wantedRevision, clean, normalize(text))
    }
    private fun validDate(value: String) { if (value.length == 10) LocalDate.parse(value) else Instant.parse(value) }
    private fun JSONObject.optionalString(key: String): String? = if (isNull(key) || !has(key)) null else getString(key)
    private fun JSONObject.taxonomyId(key: String): String? = taxonomyString(key)?.also { require(idPattern.matches(it)) }
    private fun JSONObject.taxonomyTitle(key: String): String? = taxonomyString(key)?.also {
        require(it.isNotBlank() && it.length <= 500)
    }?.let(::normalize)
    private fun JSONObject.taxonomyString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return (get(key) as? String) ?: error("모음 문자열 형식이 올바르지 않습니다")
    }
    private fun JSONObject.taxonomyOrder(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        val value = (get(key) as? Number)?.toDouble() ?: error("모음 순서 형식이 올바르지 않습니다")
        require(value.isFinite() && value >= 1 && value <= Int.MAX_VALUE && value % 1.0 == 0.0)
        return value.toInt()
    }
    private fun requireTaxonomy(id: String?, title: String?, order: Int?) {
        require((id == null && title == null && order == null) || (id != null && title != null && order != null)) {
            "모음 ID, 제목, 순서가 함께 필요합니다"
        }
    }
    fun normalize(value: String) = Normalizer.normalize(value, Normalizer.Form.NFC)
    fun searchPattern(query: String): String = "%" + normalize(query.trim().take(200)).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
}
