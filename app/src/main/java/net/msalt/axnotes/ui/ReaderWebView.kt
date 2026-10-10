package net.msalt.axnotes.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
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
import androidx.core.widget.TextViewCompat
import net.msalt.axnotes.data.Article
import net.msalt.axnotes.data.SafeUrl
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

internal fun buildReaderFallbackText(article: Article): String =
    "시스템 WebView를 사용할 수 없어 텍스트로 표시합니다. 이미지·서식은 시스템 WebView를 활성화한 뒤 확인하세요.\n\n${article.title}\n${article.publishedAt.take(10)}\n\n${article.bodyText.orEmpty()}"

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
    val fallbackText = remember(article) { buildReaderFallbackText(article) }
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
                                    if (request == null || !SafeUrl.https(request.url.toString()) || request.isForMainFrame) {
                                        return WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
                                    }
                                    return super.shouldInterceptRequest(view, request)
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
                    if (textView.text.toString() != fallbackText) textView.text = fallbackText
                }
            }
        )
    }
}
