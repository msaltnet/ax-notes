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
    const val titleFontSize = 28
    const val titleLineHeight = 36
    const val subheadingFontSize = 22
    const val subheadingLineHeight = 28
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
    const val sectionSpacing = 32
    const val codeCornerRadius = 8

    internal fun horizontalPadding(viewportWidthDp: Float): Float =
        (viewportWidthDp * horizontalPaddingViewportPercent / 100f)
            .coerceIn(minHorizontalPadding.toFloat(), maxHorizontalPadding.toFloat())
}

internal fun Color.readerCssColor(): String =
    String.format(Locale.ROOT, "#%06x", toArgb() and 0x00ffffff)

/** The article body is already sanitized by FeedContract before it is cached. */
internal fun buildReaderHtml(article: Article, colors: ColorScheme): String {
    fun escapeText(value: String): String = Jsoup.parse("").createElement("span").text(value).html()
    val title = escapeText(article.title)
    val publishedDate = escapeText(article.publishedAt.take(10))
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
        <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src https:; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'">
        <style>
        html{height:100%;background:$surface}
        body{box-sizing:border-box;max-width:${maxContentWidth}px;margin:0 auto;padding:${pagePadding}px ${pagePadding}px ${bottomPadding}px;padding:${pagePadding}px clamp(${minHorizontalPadding}px,${horizontalPaddingViewportPercent}vw,${maxHorizontalPadding}px) ${bottomPadding}px;background:$surface;color:$onSurface;font-family:system-ui,sans-serif;font-size:${bodyFontSize}px;line-height:${bodyLineHeight.toDouble() / bodyFontSize};overflow-wrap:break-word;overflow-wrap:anywhere}
        h1{font-size:${titleFontSize}px;line-height:${titleLineHeight.toDouble() / titleFontSize};margin-top:${compactPadding}px}
        h2,h3{font-size:${subheadingFontSize}px;line-height:${subheadingLineHeight.toDouble() / subheadingFontSize};margin-top:${sectionSpacing}px}
        a{color:$primary}
        img{max-width:100%;height:auto}
        pre{overflow-x:auto;white-space:pre;overflow-wrap:normal;background:$surfaceContainer;color:$onSurface;padding:${blockPadding}px;border-radius:${codeCornerRadius}px;font-size:${codeFontSize}px;line-height:${codeLineHeight.toDouble() / codeFontSize}}
        code,kbd{font-family:monospace;font-size:${codeFontSize}px;line-height:${codeLineHeight.toDouble() / codeFontSize}}
        table{display:block;max-width:100%;overflow-x:auto;border-collapse:collapse;font-size:${supportingFontSize}px;line-height:${supportingLineHeight.toDouble() / supportingFontSize}}
        td,th{border:1px solid $outlineVariant;padding:${compactPadding}px}
        blockquote{border-left:3px solid $outlineVariant;padding-left:${blockPadding}px;margin:${blockSpacing}px 0}
        figure{margin:${blockSpacing}px 0}
        figcaption,.meta{font-size:${supportingFontSize}px;line-height:${supportingLineHeight.toDouble() / supportingFontSize};color:$onSurfaceVariant}
        </style></head><body><div class="meta">AX NOTES · $publishedDate</div><h1>$title</h1>${article.bodyHtml.orEmpty()}</body></html>""".trimIndent()
    }
}
