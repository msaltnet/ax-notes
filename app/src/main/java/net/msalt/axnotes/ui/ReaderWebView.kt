package net.msalt.axnotes.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import android.net.Uri
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import net.msalt.axnotes.data.Article
import net.msalt.axnotes.data.SafeUrl
import org.jsoup.Jsoup

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
@Composable
fun ReaderWebView(article: Article, dark: Boolean, modifier: Modifier = Modifier, onError: (String) -> Unit) {
    val currentError by rememberUpdatedState(onError)
    val escapedTitle = Jsoup.parse("").createElement("span").text(article.title).html()
    val color = if (dark) "#e8e5da" else "#222c28"
    val background = if (dark) "#151d19" else "#faf8f0"
    val html = remember(article.id, article.cachedRevision, dark) {
        """<!doctype html><html lang="ko"><head><meta name="viewport" content="width=device-width, initial-scale=1"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src https:; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'"><style>
        body{margin:0;padding:20px 22px 52px;background:$background;color:$color;font-family:system-ui,sans-serif;font-size:18px;line-height:1.85;overflow-wrap:anywhere} h1{font-size:29px;line-height:1.45;margin-top:6px}h2{font-size:24px;margin-top:2em}h3{font-size:21px}a{color:${if(dark) "#a7d7be" else "#315e50"}}img{max-width:100%;height:auto}pre{overflow-x:auto;white-space:pre;overflow-wrap:normal;background:${if(dark) "#25382d" else "#eeeee4"};padding:16px;border-radius:8px;font-size:14px}code{font-family:monospace}table{display:block;max-width:100%;overflow-x:auto;border-collapse:collapse;font-size:15px}td,th{border:1px solid #999;padding:9px}blockquote{border-left:3px solid #8ea99b;padding-left:16px;margin:20px 0}figure{margin:20px 0}figcaption{font-size:14px;opacity:.75}.meta{font-size:13px;opacity:.65}
        </style></head><body><div class="meta">AX NOTES · ${article.publishedAt.take(10)}</div><h1>$escapedTitle</h1>${article.bodyHtml.orEmpty()}</body></html>"""
    }
    AndroidView(modifier = modifier, factory = { context ->
        FrameLayout(context).apply {
            try {
                addView(WebView(context).apply {
            settings.javaScriptEnabled = false; settings.allowFileAccess = false; settings.allowContentAccess = false
            settings.domStorageEnabled = false; settings.databaseEnabled = false
            settings.setSupportMultipleWindows(false); settings.javaScriptCanOpenWindowsAutomatically = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.safeBrowsingEnabled = true
            settings.textZoom = (resources.configuration.fontScale * 100).toInt().coerceAtLeast(1)
            setBackgroundColor(android.graphics.Color.parseColor(background))
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString().orEmpty()
                    if (request?.isForMainFrame == true && SafeUrl.https(url)) openExternal(context, url, currentError)
                    return true
                }
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    if (request == null || !SafeUrl.https(request.url.toString()) || request.isForMainFrame) return WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
                    return super.shouldInterceptRequest(view, request)
                }
                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) { handler?.cancel() }
            }
            setDownloadListener { url, _, _, _, _ -> if (SafeUrl.https(url)) openExternal(context, url, currentError) }
                }, FrameLayout.LayoutParams(-1, -1))
            } catch (_: RuntimeException) {
                // Some AOSP images and devices with a disabled/broken provider
                // cannot construct a WebView. Cached text still stays readable.
                addView(ScrollView(context).apply {
                    addView(TextView(context).apply {
                        textSize = 18f; setTextColor(android.graphics.Color.parseColor(color))
                        val pad = (22 * resources.displayMetrics.density).toInt()
                        setPadding(pad, pad, pad, pad); setTextIsSelectable(true)
                    })
                }, FrameLayout.LayoutParams(-1, -1))
            }
        }
    }, onRelease = { container ->
        val view = container.getChildAt(0) as? WebView
        container.removeAllViews()
        view?.let { it.stopLoading(); it.webViewClient = WebViewClient(); it.removeAllViews(); it.destroy() }
    }, update = { container ->
        val identity = "${article.id}:${article.cachedRevision}:$dark"
        if (container.tag != identity) {
            container.tag = identity
            val child = container.getChildAt(0)
            if (child is WebView) child.loadDataWithBaseURL(article.canonicalUrl, html, "text/html", "UTF-8", null)
            else ((child as ScrollView).getChildAt(0) as TextView).text =
                "시스템 WebView를 사용할 수 없어 텍스트로 표시합니다. 이미지·서식은 시스템 WebView를 활성화한 뒤 확인하세요.\n\n${article.title}\n${article.publishedAt.take(10)}\n\n${article.bodyText.orEmpty()}"
        }
    })
}
