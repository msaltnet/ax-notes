package net.msalt.axnotes.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import net.msalt.axnotes.data.Article
import org.jsoup.Jsoup
import java.util.Locale

/**
 * AX's long-form reading tokens, shared by HTML and the native-text fallback.
 * Typography uses CSS pixels in WebView and scaled pixels (sp) in native text.
 * HTML emits line-height as a unitless ratio so text zoom scales leading too;
 * layout uses CSS pixels and density-independent pixels (dp), respectively.
 */
object AxReaderStyle {
    const val bodyFontSize = 18
    const val bodyLineHeight = 30
    const val titleFontSize = 32
    const val titleLineHeight = 44
    const val headingFontSize = 24
    const val headingLineHeight = 34
    const val subheadingFontSize = 22
    const val subheadingLineHeight = 32
    const val quoteFontSize = 22
    const val quoteLineHeight = 34
    const val codeFontSize = 14
    const val codeLineHeight = 22
    const val supportingFontSize = 14
    const val supportingLineHeight = 20
    const val maxContentWidth = 764
    const val pagePadding = 24
    const val minHorizontalPadding = 18
    const val maxHorizontalPadding = 32
    const val horizontalPaddingViewportPercent = 4
    const val bottomPadding = 64
    const val blockPadding = 16
    const val compactPadding = 8
    const val blockSpacing = 24
    const val sectionSpacing = 40
    const val codeCornerRadius = 4

    internal fun horizontalPadding(viewportWidthDp: Float): Float =
        (viewportWidthDp * horizontalPaddingViewportPercent / 100f)
            .coerceIn(minHorizontalPadding.toFloat(), maxHorizontalPadding.toFloat())
}

// A single packaged font is served by ReaderWebView. This URL is never fetched from the network.
internal const val readerFontHost = "appassets.androidplatform.net"
internal const val readerFontUrl = "https://$readerFontHost/ax-font/editorial.ttf"

internal fun Color.readerCssColor(): String =
    String.format(Locale.ROOT, "#%06x", toArgb() and 0x00ffffff)

/** The article body is already sanitized by FeedContract before it is cached. */
internal fun buildReaderHtml(article: Article, colors: ColorScheme): String {
    fun escapeText(value: String): String = Jsoup.parse("").createElement("span").text(value).html()
    val title = escapeText(article.title)
    val publishedDate = escapeText(article.publishedAt.take(10))
    // Taxonomy appears only when the feed supplies it; no invented topic, progress, or reading time.
    val collectionTitle = (article.projectTitle ?: article.seriesTitle)?.takeIf { it.isNotBlank() }
    val eyebrow = escapeText(collectionTitle ?: "AX NOTES")
    val surface = colors.surface.readerCssColor()
    val onSurface = colors.onSurface.readerCssColor()
    val primary = colors.primary.readerCssColor()
    val surfaceContainer = colors.surfaceContainer.readerCssColor()
    val outlineVariant = colors.outlineVariant.readerCssColor()
    val onSurfaceVariant = colors.onSurfaceVariant.readerCssColor()
    return with(AxReaderStyle) {
        """<!doctype html>
        <html lang="ko"><head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src https:; font-src $readerFontUrl; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'">
        <style>
        @font-face{font-family:'AX Editorial';src:url('$readerFontUrl') format('truetype');font-weight:400;font-style:normal;font-display:swap}
        html{height:100%;background:$surface}
        body{box-sizing:border-box;max-width:${maxContentWidth}px;margin:0 auto;padding:${pagePadding}px ${pagePadding}px ${bottomPadding}px;padding:${pagePadding}px clamp(${minHorizontalPadding}px,${horizontalPaddingViewportPercent}vw,${maxHorizontalPadding}px) ${bottomPadding}px;background:$surface;color:$onSurface;font-family:system-ui,sans-serif;font-size:${bodyFontSize}px;line-height:${bodyLineHeight.toDouble() / bodyFontSize};overflow-wrap:break-word;overflow-wrap:anywhere}
        .article-header{margin:0 0 ${sectionSpacing}px;padding-bottom:${blockSpacing}px;border-bottom:1px solid $outlineVariant}
        .eyebrow{margin:0 0 ${blockPadding}px;font-family:system-ui,sans-serif;font-size:${supportingFontSize}px;line-height:${supportingLineHeight.toDouble() / supportingFontSize};font-weight:600;letter-spacing:0.04em;color:$onSurfaceVariant}
        h1,h2,h3,h4,h5,h6,blockquote{font-family:'AX Editorial','Noto Serif CJK KR',serif;font-weight:400;word-break:keep-all;overflow-wrap:anywhere}
        h1{font-size:${titleFontSize}px;line-height:${titleLineHeight.toDouble() / titleFontSize};margin:0 0 ${blockSpacing}px;letter-spacing:-0.025em}
        h2{font-size:${headingFontSize}px;line-height:${headingLineHeight.toDouble() / headingFontSize};margin:${sectionSpacing}px 0 ${blockPadding}px;padding-top:${blockSpacing}px;border-top:1px solid $outlineVariant}
        h3,h4,h5,h6{font-size:${subheadingFontSize}px;line-height:${subheadingLineHeight.toDouble() / subheadingFontSize};margin:${sectionSpacing}px 0 ${blockPadding}px}
        p,ul,ol,dl{margin:0 0 ${blockSpacing}px}
        li+li{margin-top:${compactPadding}px}
        li>p{margin-bottom:${compactPadding}px}
        a{color:$primary;text-decoration:underline;text-underline-offset:0.18em}
        a:focus-visible{outline:2px solid $primary;outline-offset:2px}
        img{display:block;max-width:100%;height:auto;margin:${blockSpacing}px auto}
        pre{overflow-x:auto;white-space:pre;overflow-wrap:normal;background:$surfaceContainer;color:$onSurface;margin:${blockSpacing}px 0;padding:${blockPadding}px;border:1px solid $outlineVariant;border-radius:${codeCornerRadius}px;font-size:${codeFontSize}px;line-height:${codeLineHeight.toDouble() / codeFontSize}}
        code,kbd{font-family:monospace;font-size:${codeFontSize}px;line-height:${codeLineHeight.toDouble() / codeFontSize}}
        table{display:block;max-width:100%;overflow-x:auto;border-collapse:collapse;margin:${blockSpacing}px 0;font-size:${supportingFontSize}px;line-height:${supportingLineHeight.toDouble() / supportingFontSize}}
        td,th{border:1px solid $outlineVariant;padding:${compactPadding}px;text-align:start}
        blockquote{border-left:2px solid $primary;padding-left:${blockSpacing}px;margin:${sectionSpacing}px 0;color:$onSurfaceVariant;font-size:${quoteFontSize}px;line-height:${quoteLineHeight.toDouble() / quoteFontSize}}
        blockquote>:last-child{margin-bottom:0}
        figure{margin:${blockSpacing}px 0}
        figcaption,.meta{font-family:system-ui,sans-serif;font-size:${supportingFontSize}px;line-height:${supportingLineHeight.toDouble() / supportingFontSize};color:$onSurfaceVariant}
        .meta{margin:0}
        hr{border:0;border-top:1px solid $outlineVariant;margin:${sectionSpacing}px 0}
        </style></head><body><main><article><header class="article-header"><p class="eyebrow">$eyebrow</p><h1>$title</h1><p class="meta"><time>$publishedDate</time></p></header>${article.bodyHtml.orEmpty()}</article></main></body></html>""".trimIndent()
    }
}
