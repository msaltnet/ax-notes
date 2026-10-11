package net.msalt.axnotes.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.LineHeightSpan
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import androidx.core.net.toUri
import net.msalt.axnotes.R
import net.msalt.axnotes.data.Article
import net.msalt.axnotes.data.SafeUrl
import kotlin.math.ceil
import kotlin.math.roundToInt

fun openExternal(context: Context, url: String, onError: (String) -> Unit) {
    if (!SafeUrl.https(url)) { onError("HTTPS 링크만 열 수 있습니다"); return }
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)) }
    catch (_: ActivityNotFoundException) { onError("링크를 열 브라우저가 없습니다") }
}

fun shareArticle(context: Context, title: String, url: String, onError: (String) -> Unit) {
    if (!SafeUrl.https(url)) return
    try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "$title\n$url") }, "글 공유")) }
    catch (_: ActivityNotFoundException) { onError("공유할 앱이 없습니다") }
}

private const val readerFallbackExplanation =
    "시스템 WebView를 사용할 수 없어 텍스트로 표시합니다. 이미지·서식은 시스템 WebView를 활성화한 뒤 확인하세요."

internal fun buildReaderFallbackText(article: Article): String =
    "$readerFallbackExplanation\n\n${article.title}\n${article.publishedAt.take(10)}\n\n${article.bodyText.orEmpty()}"

/** TypefaceSpan(Typeface) needs API 28, while AX also supports Android 8. */
internal class ReaderEditorialTypefaceSpan(private val editorialTypeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(textPaint: TextPaint) { textPaint.typeface = editorialTypeface }
    override fun updateMeasureState(textPaint: TextPaint) { textPaint.typeface = editorialTypeface }
}

/** Keeps title leading independent of body spacing without reducing the font's glyph bounds. */
private class ReaderTitleLineHeightSpan(private val minimumMetricsHeightPx: Int) : LineHeightSpan {
    override fun chooseHeight(text: CharSequence, start: Int, end: Int, spanstartv: Int, v: Int, fm: Paint.FontMetricsInt) {
        val extra = (minimumMetricsHeightPx - (fm.descent - fm.ascent)).coerceAtLeast(0)
        fm.ascent -= extra / 2
        fm.descent += extra - extra / 2
        fm.top = minOf(fm.top, fm.ascent)
        fm.bottom = maxOf(fm.bottom, fm.descent)
    }
}

internal fun applyReaderFallbackContent(view: TextView, article: Article, colors: ColorScheme) {
    // Style is applied first. Derive the effective scale from its body size so this
    // also follows a retained TextView when the system font scale changes.
    val scaledDensity = view.textSize / AxReaderStyle.bodyFontSize
    val identity = Triple(article, colors.onSurfaceVariant, view.textSize to view.lineSpacingExtra)
    if (view.tag == identity) return
    val text = buildReaderFallbackText(article)
    val selectionStart = view.selectionStart
    val selectionEnd = view.selectionEnd
    val preserveSelection = view.text.toString() == text && selectionStart >= 0 && selectionEnd >= 0
    val titleStart = readerFallbackExplanation.length + 2
    val titleEnd = titleStart + article.title.length
    val dateStart = titleEnd + 1
    val dateEnd = dateStart + article.publishedAt.take(10).length
    val styled = SpannableString(text).apply {
        val flags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        setSpan(RelativeSizeSpan(AxReaderStyle.titleFontSize.toFloat() / AxReaderStyle.bodyFontSize), titleStart, titleEnd, flags)
        // TextView adds its body line-spacing extra after this span's metrics.
        // Subtract it once here rather than adding body leading to the 44sp title.
        val titleMetricsHeight = ceil(AxReaderStyle.titleLineHeight * scaledDensity - view.lineSpacingExtra).toInt()
        setSpan(ReaderTitleLineHeightSpan(titleMetricsHeight), titleStart, titleEnd, flags)
        ResourcesCompat.getFont(view.context, R.font.nanum_myeongjo_regular)?.let {
            setSpan(ReaderEditorialTypefaceSpan(it), titleStart, titleEnd, flags)
        }
        listOf(0 to readerFallbackExplanation.length, dateStart to dateEnd).forEach { (start, end) ->
            setSpan(RelativeSizeSpan(AxReaderStyle.supportingFontSize.toFloat() / AxReaderStyle.bodyFontSize), start, end, flags)
            setSpan(ForegroundColorSpan(colors.onSurfaceVariant.toArgb()), start, end, flags)
        }
    }
    view.text = styled
    if (preserveSelection) (view.text as? Spannable)?.let { Selection.setSelection(it, selectionStart, selectionEnd) }
    view.tag = identity
}

/**
 * Only this exact font subresource can resolve to packaged bytes. Unknown paths on
 * the synthetic host are blocked rather than falling through to a network fetch.
 */
internal fun readerResourceResponse(context: Context, url: String, isMainFrame: Boolean, method: String): WebResourceResponse? {
    if (url == readerFontUrl && !isMainFrame && method == "GET") {
        // Fonts are packaged as uncompiled resource bytes. Android can open them
        // here even though openRawResource's lint annotation only names R.raw.
        @SuppressLint("ResourceType")
        val fontBytes = context.resources.openRawResource(R.font.nanum_myeongjo_regular)
        return WebResourceResponse(
            "font/ttf", null, 200, "OK",
            mapOf("Access-Control-Allow-Origin" to "*"),
            fontBytes
        )
    }
    if (isMainFrame || !SafeUrl.https(url) || url.toUri().host.equals(readerFontHost, ignoreCase = true)) {
        return WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
    }
    return null
}

internal fun applyReaderFallbackStyle(view: TextView, colors: ColorScheme, fontScale: Float) {
    val scaledDensity = view.resources.displayMetrics.density * fontScale
    view.setTextColor(colors.onSurface.toArgb())
    view.setBackgroundColor(colors.surface.toArgb())
    view.setLinkTextColor(colors.primary.toArgb())
    view.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    view.includeFontPadding = false
    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, AxReaderStyle.bodyFontSize * scaledDensity)
    TextViewCompat.setLineHeight(view, (AxReaderStyle.bodyLineHeight * scaledDensity).roundToInt())
}

internal fun applyReaderFallbackLayout(view: TextView, viewportWidthPx: Int) {
    val density = view.resources.displayMetrics.density
    val horizontalPadding = if (viewportWidthPx > 0) {
        AxReaderStyle.horizontalPadding(viewportWidthPx / density)
    } else {
        AxReaderStyle.pagePadding.toFloat()
    }
    view.setPadding(
        (horizontalPadding * density).roundToInt(),
        (AxReaderStyle.pagePadding * density).roundToInt(),
        (horizontalPadding * density).roundToInt(),
        (AxReaderStyle.bottomPadding * density).roundToInt()
    )
    val width = if (viewportWidthPx > 0) {
        minOf(viewportWidthPx, (AxReaderStyle.maxContentWidth * density).roundToInt())
    } else {
        ViewGroup.LayoutParams.MATCH_PARENT
    }
    val previous = view.layoutParams as? FrameLayout.LayoutParams
    if (previous == null || previous.width != width || previous.gravity != Gravity.CENTER_HORIZONTAL) {
        view.layoutParams = FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL)
    }
}

@Composable
fun ReaderWebView(article: Article, modifier: Modifier = Modifier, onError: (String) -> Unit) {
    val currentError by rememberUpdatedState(onError)
    var scrollFraction by rememberSaveable(article.id) { mutableFloatStateOf(0f) }
    val colors = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    val html = remember(
        article, colors.surface, colors.onSurface, colors.primary,
        colors.surfaceContainer, colors.outlineVariant, colors.onSurfaceVariant
    ) { buildReaderHtml(article, colors) }
    key(article.id) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                FrameLayout(context).apply {
                    try {
                        addView(WebView(context).apply {
                            settings.javaScriptEnabled = false
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.domStorageEnabled = false
                            settings.databaseEnabled = false
                            settings.setSupportMultipleWindows(false)
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.safeBrowsingEnabled = true
                            settings.textZoom = (resources.configuration.fontScale * 100).toInt().coerceAtLeast(1)
                            setBackgroundColor(colors.surface.toArgb())
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                            var restoringPosition = true
                            setOnScrollChangeListener { _, _, y, _, _ ->
                                val range = (contentHeight * resources.displayMetrics.density - height).coerceAtLeast(1f)
                                if (!restoringPosition) scrollFraction = (y / range).coerceIn(0f, 1f)
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    restoringPosition = true
                                }

                                override fun onPageFinished(view: WebView, url: String?) {
                                    // Restore relative reading position after rotation/reflow; never execute JavaScript.
                                    val restore = scrollFraction
                                    view.post {
                                        val range = (view.contentHeight * view.resources.displayMetrics.density - view.height).coerceAtLeast(0f)
                                        view.scrollTo(0, (range * restore).toInt())
                                        restoringPosition = false
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString().orEmpty()
                                    if (request?.isForMainFrame == true && SafeUrl.https(url)) openExternal(context, url, currentError)
                                    return true
                                }

                                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                    return readerResourceResponse(
                                        context, request?.url?.toString().orEmpty(),
                                        request?.isForMainFrame ?: true, request?.method.orEmpty()
                                    ) ?: super.shouldInterceptRequest(view, request)
                                }

                                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                                    handler?.cancel()
                                }
                            }
                            setDownloadListener { url, _, _, _, _ ->
                                if (SafeUrl.https(url)) openExternal(context, url, currentError)
                            }
                        }, FrameLayout.LayoutParams(-1, -1))
                    } catch (_: RuntimeException) {
                        // A disabled or broken WebView provider must not hide cached text.
                        addView(ScrollView(context).apply {
                            val textView = TextView(context).apply { setTextIsSelectable(true) }
                            addView(textView)
                            applyReaderFallbackLayout(textView, width)
                            addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
                                applyReaderFallbackLayout(textView, right - left)
                            }
                        }, FrameLayout.LayoutParams(-1, -1))
                    }
                }
            },
            onRelease = { container ->
                val view = container.getChildAt(0) as? WebView
                view?.setOnScrollChangeListener(null)
                container.removeAllViews()
                view?.let {
                    it.stopLoading()
                    it.webViewClient = WebViewClient()
                    it.removeAllViews()
                    it.destroy()
                }
            },
            update = { container ->
                val background = colors.surface.toArgb()
                container.setBackgroundColor(background)
                val child = container.getChildAt(0)
                if (child is WebView) {
                    // AndroidView is retained on theme changes; update its native canvas too.
                    child.setBackgroundColor(background)
                    child.settings.textZoom = (fontScale * 100).toInt().coerceAtLeast(1)
                    val identity = article to html
                    if (container.tag != identity) {
                        container.tag = identity
                        child.loadDataWithBaseURL(article.canonicalUrl, html, "text/html", "UTF-8", null)
                    }
                } else if (child is ScrollView) {
                    child.setBackgroundColor(background)
                    val textView = child.getChildAt(0) as TextView
                    applyReaderFallbackStyle(textView, colors, fontScale)
                    applyReaderFallbackLayout(textView, child.width)
                    applyReaderFallbackContent(textView, article, colors)
                }
            }
        )
    }
}
