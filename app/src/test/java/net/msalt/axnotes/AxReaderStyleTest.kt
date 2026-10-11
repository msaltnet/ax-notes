package net.msalt.axnotes

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.text.Selection
import android.text.Spannable
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.LineHeightSpan
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.core.content.res.ResourcesCompat
import net.msalt.axnotes.data.Article
import net.msalt.axnotes.ui.AxDarkColors
import net.msalt.axnotes.ui.AxLightColors
import net.msalt.axnotes.ui.AxReaderStyle
import net.msalt.axnotes.ui.ReaderEditorialTypefaceSpan
import net.msalt.axnotes.ui.applyReaderFallbackContent
import net.msalt.axnotes.ui.applyReaderFallbackLayout
import net.msalt.axnotes.ui.applyReaderFallbackStyle
import net.msalt.axnotes.ui.buildReaderFallbackText
import net.msalt.axnotes.ui.buildReaderHtml
import net.msalt.axnotes.ui.readerCssColor
import net.msalt.axnotes.ui.readerBodyFontUrl
import net.msalt.axnotes.ui.readerUiFontUrl
import net.msalt.axnotes.ui.readerFontUrl
import net.msalt.axnotes.ui.readerResourceResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "mdpi")
class AxReaderStyleTest {
    private val article = Article(
        id = "reader-style",
        title = "읽기 좋은 AX Notes",
        description = "읽기 서식 확인",
        publishedAt = "2026-10-10T01:00:00Z",
        updatedAt = null,
        canonicalUrl = "https://ax.msalt.net/notes/reader-style/",
        detailUrl = "https://ax.msalt.net/app/v1/notes/reader-style.json",
        wantedRevision = "fixture-v1",
        projectUrl = null,
        cachedRevision = "fixture-v1",
        bodyHtml = "<p>저장된 본문과 <a href=\"https://ax.msalt.net/about/\">링크</a></p><pre><code>val ax = 1</code></pre>",
        bodyText = "저장된 본문과 링크\nval ax = 1",
        cachedAt = 1L
    )

    @Test
    fun htmlUsesTheActiveLightAndDarkMaterialColorRoles() {
        listOf(AxLightColors, AxDarkColors).forEach { colors ->
            val document = Jsoup.parse(buildReaderHtml(article, colors))
            assertEquals(colors.surface.readerCssColor(), cssRule(document, "html")["background"])
            assertEquals(colors.surface.readerCssColor(), cssRule(document, "body")["background"])
            assertEquals(colors.onSurface.readerCssColor(), cssRule(document, "body")["color"])
            assertEquals(colors.primary.readerCssColor(), cssRule(document, "a")["color"])
            assertEquals(colors.surfaceContainer.readerCssColor(), cssRule(document, "pre")["background"])
            assertEquals(colors.onSurface.readerCssColor(), cssRule(document, "pre")["color"])
            assertEquals("1px solid ${colors.outlineVariant.readerCssColor()}", cssRule(document, "td,th")["border"])
            assertEquals("2px solid ${colors.primary.readerCssColor()}", cssRule(document, "blockquote")["border-left"])
            assertEquals(colors.onSurfaceVariant.readerCssColor(), cssRule(document, "blockquote")["color"])
            assertEquals("1px solid ${colors.outlineVariant.readerCssColor()}", cssRule(document, ".article-header")["border-bottom"])
            assertEquals("1px solid ${colors.outlineVariant.readerCssColor()}", cssRule(document, "h2")["border-top"])
            assertEquals(colors.onSurfaceVariant.readerCssColor(), cssRule(document, "figcaption,.meta")["color"])
            assertFalse("Supporting text must use an opaque semantic role", document.selectFirst("style")!!.data().contains("opacity"))
        }
        assertNotEquals(buildReaderHtml(article, AxLightColors), buildReaderHtml(article, AxDarkColors))
    }

    @Test
    fun htmlUsesTheProvidedSchemeRatherThanAnIndependentReaderPalette() {
        val colors = AxLightColors.copy(
            surface = Color(0xFF123456),
            onSurface = Color(0xFF234567),
            primary = Color(0xFF345678),
            surfaceContainer = Color(0xFF456789),
            outlineVariant = Color(0xFF56789A),
            onSurfaceVariant = Color(0xFF6789AB)
        )
        val document = Jsoup.parse(buildReaderHtml(article, colors))
        assertEquals("#123456", cssRule(document, "body")["background"])
        assertEquals("#234567", cssRule(document, "body")["color"])
        assertEquals("#345678", cssRule(document, "a")["color"])
        assertEquals("#456789", cssRule(document, "pre")["background"])
        assertEquals("1px solid #56789a", cssRule(document, "td,th")["border"])
        assertEquals("#6789ab", cssRule(document, "figcaption,.meta")["color"])
    }

    @Test
    fun htmlUsesTheSharedReadingTypographyAndResponsivePageTokens() {
        val document = Jsoup.parse(buildReaderHtml(article, AxLightColors))
        assertTypography(document, "body", 18, 30)
        assertTypography(document, "h1", 32, 44)
        assertTypography(document, "h2", 24, 34)
        assertTypography(document, "h3,h4,h5,h6", 22, 32)
        assertTypography(document, "blockquote", 22, 34)
        assertTypography(document, "pre", 14, 22)
        assertTypography(document, "code,kbd", 14, 22)
        assertTypography(document, "table", 14, 20)
        assertTypography(document, "figcaption,.meta", 14, 20)
        assertEquals("${AxReaderStyle.maxContentWidth}px", cssRule(document, "body")["max-width"])
        assertEquals("24px clamp(18px,4vw,32px) 64px", cssRule(document, "body")["padding"])
        assertEquals("anywhere", cssRule(document, "body")["overflow-wrap"])
        assertEquals("auto", cssRule(document, "pre")["overflow-x"])
        assertEquals("auto", cssRule(document, "table")["overflow-x"])
        assertEquals("100%", cssRule(document, "img")["max-width"])
    }

    @Test
    fun titleIsEscapedAndCachedMarkupAndMetadataArePreserved() {
        val title = "<img src=x onerror=\"steal()\"> & \"quoted\" <script>bad()</script>"
        val html = buildReaderHtml(article.copy(title = title), AxLightColors)
        val document = Jsoup.parse(html)
        val heading = document.selectFirst("h1")!!
        assertEquals(title, heading.text())
        assertEquals(0, heading.childrenSize())
        assertTrue(document.select("script,img").isEmpty())
        assertTrue(html.contains(article.bodyHtml!!))
        assertEquals("AX NOTES", document.selectFirst(".eyebrow")!!.text())
        assertEquals("2026-10-10", document.selectFirst(".meta time")!!.text())
        assertEquals("https://ax.msalt.net/about/", document.selectFirst("a")!!.attr("href"))
        assertEquals("ko", document.selectFirst("html")!!.attr("lang"))
    }

    @Test
    fun generatedDocumentRetainsTheRestrictiveContentSecurityPolicy() {
        listOf(AxLightColors, AxDarkColors).forEach { colors ->
            val document = Jsoup.parse(buildReaderHtml(article, colors))
            assertEquals(
                "default-src 'none'; img-src https:; font-src $readerFontUrl $readerBodyFontUrl $readerUiFontUrl; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'",
                document.selectFirst("meta[http-equiv=Content-Security-Policy]")!!.attr("content")
            )
            assertTrue(document.select("script,iframe,form,base").isEmpty())
            assertEquals("width=device-width, initial-scale=1", document.selectFirst("meta[name=viewport]")!!.attr("content"))
        }
    }

    @Test
    fun theSameNativeFallbackUpdatesColorsAndFontScaleAcrossThemeChanges() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = TextView(context)
        view.text = buildReaderFallbackText(article)
        listOf(AxLightColors, AxDarkColors, AxLightColors).forEach { colors ->
            applyReaderFallbackStyle(view, colors, fontScale = 1f)
            assertEquals(colors.onSurface.toArgb(), view.currentTextColor)
            assertEquals(colors.surface.toArgb(), (view.background as ColorDrawable).color)
            assertEquals(colors.primary.toArgb(), view.linkTextColors.defaultColor)
            assertEquals(18f, view.textSize, 0.01f)
            assertEquals(30, view.lineHeight)
        }
        applyReaderFallbackStyle(view, AxDarkColors, fontScale = 2f)
        assertEquals(36f, view.textSize, 0.01f)
        assertEquals(60, view.lineHeight)
        assertFalse(view.includeFontPadding)
        assertEquals(buildReaderFallbackText(article), view.text.toString())
    }

    @Test
    fun nativeFallbackUsesResponsivePaddingAndTheReadableWidthLimit() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = TextView(context)
        applyReaderFallbackLayout(view, viewportWidthPx = 360)
        assertEquals(18, view.paddingLeft)
        assertEquals(18, view.paddingRight)
        assertEquals(24, view.paddingTop)
        assertEquals(64, view.paddingBottom)
        assertEquals(360, view.layoutParams.width)

        applyReaderFallbackLayout(view, viewportWidthPx = 600)
        assertEquals(24, view.paddingLeft)
        assertEquals(600, view.layoutParams.width)

        applyReaderFallbackLayout(view, viewportWidthPx = 1200)
        assertEquals(32, view.paddingLeft)
        assertEquals(AxReaderStyle.maxContentWidth, view.layoutParams.width)
        assertEquals(Gravity.CENTER_HORIZONTAL, (view.layoutParams as FrameLayout.LayoutParams).gravity)
    }

    @Test
    fun nativeFallbackRetainsTheExplanationTitleDateAndCachedText() {
        val text = buildReaderFallbackText(article)
        assertTrue(text.startsWith("시스템 WebView를 사용할 수 없어 텍스트로 표시합니다."))
        assertTrue(text.contains("${article.title}\n2026-10-10\n\n${article.bodyText}"))
        val uncached = article.copy(bodyHtml = null, bodyText = null)
        assertFalse(buildReaderFallbackText(uncached).contains("null"))
        assertFalse(buildReaderHtml(uncached, AxLightColors).contains("null"))
    }


    @Test
    fun editorialHierarchyUsesTheThreeOfflineWebsiteFontFamilies() {
        val document = Jsoup.parse(buildReaderHtml(article, AxLightColors))
        assertTrue(cssRule(document, "body")["font-family"]!!.startsWith("'AX Body',"))
        assertTrue(cssRule(document, "h1,h2,h3,h4,h5,h6,blockquote")["font-family"]!!.startsWith("'AX Editorial',"))
        assertTrue(cssRule(document, "figcaption,.meta")["font-family"]!!.startsWith("'AX UI',"))
        assertEquals("600", cssRule(document, "h1,h2,h3,h4,h5,h6,blockquote")["font-weight"])
        val faces = Regex("@font-face\\{([^}]*)}").findAll(document.selectFirst("style")!!.data()).map { match ->
            match.groupValues[1].split(';').filter { it.contains(':') }
                .associate { it.substringBefore(':') to it.substringAfter(':') }
        }.toList()
        assertEquals(3, faces.size)
        val expected = mapOf(
            "'AX Editorial'" to ("url('$readerFontUrl') format('opentype')" to "600"),
            "'AX Body'" to ("url('$readerBodyFontUrl') format('truetype')" to "100 900"),
            "'AX UI'" to ("url('$readerUiFontUrl') format('truetype')" to "400")
        )
        assertEquals(expected.keys, faces.map { it["font-family"] }.toSet())
        faces.forEach { face ->
            val (source, weight) = expected.getValue(face.getValue("font-family"))
            assertEquals(source, face["src"])
            assertEquals(weight, face["font-weight"])
            assertEquals("swap", face["font-display"])
        }
        assertEquals("4px", cssRule(document, "pre")["border-radius"])
        assertEquals("0 0 40px", cssRule(document, ".article-header")["margin"])
        assertEquals("underline", cssRule(document, "a")["text-decoration"])
        assertFalse(document.selectFirst("style")!!.data().contains("user-select:none"))
        assertEquals(1, document.select("main > article > header.article-header > h1").size)
    }

    @Test
    fun onlyRealFeedTaxonomyAppearsInTheEyebrowAndItIsEscaped() {
        val projectTitle = "실험 <img src=x onerror='steal()'> & 기록"
        val project = Jsoup.parse(buildReaderHtml(article.copy(projectTitle = projectTitle), AxLightColors))
        assertEquals(projectTitle, project.selectFirst(".eyebrow")!!.text())
        assertEquals(0, project.selectFirst(".eyebrow")!!.childrenSize())
        val series = Jsoup.parse(buildReaderHtml(article.copy(seriesTitle = "개발 노트"), AxLightColors))
        assertEquals("개발 노트", series.selectFirst(".eyebrow")!!.text())
        val header = Jsoup.parse(buildReaderHtml(article, AxLightColors)).selectFirst(".article-header")!!
        assertEquals("AX NOTES ${article.title} 2026-10-10", header.text())
        assertFalse(header.text().contains("분"))
        assertFalse(header.text().contains("%"))
    }

    @Test
    fun allThreeFontsAreServedOnlyForTheirExactGetSubresourceAndNeverFetched() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resources = listOf(
            Triple(readerFontUrl, R.font.noto_serif_kr_semibold, "font/otf"),
            Triple(readerBodyFontUrl, R.font.pretendard_variable, "font/ttf"),
            Triple(readerUiFontUrl, R.font.ibm_plex_sans_kr_regular, "font/ttf")
        )
        resources.forEach { (url, resource, mimeType) ->
            val response = readerResourceResponse(context, url, isMainFrame = false, method = "GET")!!
            assertEquals(mimeType, response.mimeType)
            assertEquals(200, response.statusCode)
            assertEquals("*", response.responseHeaders["Access-Control-Allow-Origin"])
            val bundledBytes = context.resources.openRawResource(resource).use { it.readBytes() }
            assertTrue(bundledBytes.isNotEmpty())
            assertTrue("$url must return the exact packaged font", response.data.use { it.readBytes() }.contentEquals(bundledBytes))
            listOf(
                url to true,
                "$url?redirect=other" to false,
                "$url#fragment" to false,
                url.replace("/ax-font/", "/ax-font/../ax-font/") to false,
                url.replace("https://", "http://") to false
            ).forEach { (target, mainFrame) -> assertBlockedFontResource(context, target, mainFrame, "GET") }
            listOf("POST", "HEAD", "OPTIONS", "get").forEach { method -> assertBlockedFontResource(context, url, false, method) }
        }
        listOf(
            "https://appassets.androidplatform.net/ax-font/other.ttf" to false,
            "https://appassets.androidplatform.net/ax-font/editorial.ttf" to false,
            "https://APPASSETS.ANDROIDPLATFORM.NET/unknown" to false,
            "file:///android_res/font/noto_serif_kr_semibold.otf" to false,
            "content://fonts/editorial" to false,
            "http://example.com/image.png" to false,
            "https://example.com/page" to true
        ).forEach { (url, mainFrame) -> assertBlockedFontResource(context, url, mainFrame, "GET") }
        // Existing HTTPS article images still use WebView's normal request path and CSP.
        assertEquals(null, readerResourceResponse(context, "https://ax.msalt.net/image.png", false, "GET"))
    }

    private fun assertBlockedFontResource(context: Context, url: String, mainFrame: Boolean, method: String) {
        val blocked = readerResourceResponse(context, url, mainFrame, method)!!
        assertEquals("Blocked resource $method $url", 0, blocked.data.use { it.readBytes().size })
    }

    @Test
    fun nativeFallbackUsesTheSameBundledTitleFontAndPreservesSelectionOnThemeChanges() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = TextView(context).apply { setTextIsSelectable(true) }
        applyReaderFallbackStyle(view, AxLightColors, fontScale = 1f)
        assertEquals(ResourcesCompat.getFont(context, R.font.pretendard_variable), view.typeface)
        applyReaderFallbackContent(view, article, AxLightColors)
        val styled = view.text as Spanned
        val titleStart = styled.toString().indexOf(article.title)
        val titleEnd = titleStart + article.title.length
        val size = styled.getSpans(titleStart, titleEnd, RelativeSizeSpan::class.java).single()
        assertEquals(32f / 18f, size.sizeChange, 0.00001f)
        val titleFont = styled.getSpans(titleStart, titleEnd, ReaderEditorialTypefaceSpan::class.java).single()
        val paint = TextPaint()
        titleFont.updateMeasureState(paint)
        assertEquals(ResourcesCompat.getFont(context, R.font.noto_serif_kr_semibold), paint.typeface)
        assertTrue(view.isTextSelectable)
        val bodyStart = styled.toString().indexOf(article.bodyText!!)
        Selection.setSelection(view.text as Spannable, bodyStart, bodyStart + 3)
        applyReaderFallbackStyle(view, AxDarkColors, fontScale = 2f)
        applyReaderFallbackContent(view, article, AxDarkColors)
        assertEquals(buildReaderFallbackText(article), view.text.toString())
        assertEquals(bodyStart, view.selectionStart)
        assertEquals(bodyStart + 3, view.selectionEnd)
        assertEquals(36f, view.textSize, 0.01f)
        assertTrue((view.text as Spanned).getSpans(0, view.length(), ForegroundColorSpan::class.java)
            .all { it.foregroundColor == AxDarkColors.onSurfaceVariant.toArgb() })
    }

    @Test
    fun nativeFallbackTitleHasScaledEditorialLeadingWhileBodyLeadingStaysUnchanged() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = TextView(context).apply { setTextIsSelectable(true) }
        val multilineArticle = article.copy(
            title = "한국어 제목 첫 줄\n한국어 제목 둘째 줄",
            bodyText = "본문 첫 번째 줄\n본문 두 번째 줄\n본문 세 번째 줄"
        )
        // Reuse the same view, article, and colors to exercise font-scale invalidation.
        listOf(1f, 2f, 1f).forEach { scale ->
            applyReaderFallbackStyle(view, AxLightColors, scale)
            applyReaderFallbackContent(view, multilineArticle, AxLightColors)
            val styled = view.text as Spanned
            val titleStart = styled.toString().indexOf(multilineArticle.title)
            val titleEnd = titleStart + multilineArticle.title.length
            val bodyStart = styled.toString().indexOf(multilineArticle.bodyText!!)
            val titleLeading = styled.getSpans(titleStart, titleEnd, LineHeightSpan::class.java).single()
            assertEquals(titleStart, styled.getSpanStart(titleLeading))
            assertEquals(titleEnd, styled.getSpanEnd(titleLeading))
            assertTrue(styled.getSpans(bodyStart, styled.length, LineHeightSpan::class.java).isEmpty())
            val metrics = Paint.FontMetricsInt().apply { ascent = -20; descent = 4; top = -20; bottom = 4 }
            titleLeading.chooseHeight(styled, titleStart, titleEnd, 0, 0, metrics)
            assertTrue(metrics.descent - metrics.ascent + view.lineSpacingExtra >= 44f * scale)
            assertEquals((30 * scale).toInt(), view.lineHeight)

            view.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val layout = view.layout
            val firstTitleLine = layout.getLineForOffset(titleStart)
            val lastTitleLine = layout.getLineForOffset(titleEnd - 1)
            for (line in firstTitleLine..lastTitleLine) {
                assertTrue("Title line must be at least 44sp at scale $scale",
                    layout.getLineBottom(line) - layout.getLineTop(line) >= 44 * scale)
            }
            val bodyLine = layout.getLineForOffset(bodyStart)
            assertEquals((30 * scale).toInt(), layout.getLineBottom(bodyLine) - layout.getLineTop(bodyLine))
        }
    }

    private fun assertTypography(document: Document, selector: String, fontSize: Int, lineHeight: Int) {
        val rule = cssRule(document, selector)
        assertEquals("${fontSize}px", rule["font-size"])
        val lineHeightRatio = checkNotNull(rule["line-height"]).toDouble()
        assertEquals(lineHeight.toDouble() / fontSize, lineHeightRatio, 0.000001)
        // Unitless leading scales with zoomed text instead of retaining a fixed height.
        assertEquals(lineHeight * 2.0, fontSize * 2.0 * lineHeightRatio, 0.000001)
    }

    private fun cssRule(document: Document, selector: String): Map<String, String> {
        val css = document.selectFirst("style")!!.data()
        val rule = Regex("(?:^|[}\\n])\\s*${Regex.escape(selector)}\\{([^}]*)}").find(css)
        assertTrue("Missing CSS rule for $selector", rule != null)
        return rule!!.groupValues[1].split(';').filter { it.contains(':') }
            .associate { it.substringBefore(':') to it.substringAfter(':') }
    }
}
