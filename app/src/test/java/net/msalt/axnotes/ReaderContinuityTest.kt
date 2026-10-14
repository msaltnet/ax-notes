package net.msalt.axnotes

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import net.msalt.axnotes.ui.AxViewModel
import net.msalt.axnotes.ui.AxTheme
import net.msalt.axnotes.ui.AxDarkColors
import net.msalt.axnotes.ui.buildReaderHtml
import net.msalt.axnotes.ui.ReaderWebView
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w411dp-h891dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class ReaderContinuityTest {
    @get:Rule(order = 0)
    val work = object : ExternalResource() {
        override fun before() {
            resetFactory()
            RuntimeEnvironment.setFontScale(1f)
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                .putBoolean("sample", true).putBoolean("liveFeedDefaultV3", true).commit()
            WorkManagerTestInitHelper.initializeTestWorkManager(
                context, Configuration.Builder().setExecutor(SynchronousExecutor()).build()
            )
        }
        override fun after() {
            WorkManagerTestInitHelper.closeWorkDatabase()
            resetFactory()
            RuntimeEnvironment.setFontScale(1f)
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val articleTitle = "AX Notes 앱도 만들어볼까?"

    @Test
    fun memoCancelDiscardAndSaveKeepTheSameLoadedReader() {
        waitForNotes()
        openArticle()
        val initial = reader()
        val loaded = shadowOf(initial).lastLoadDataWithBaseURL
        assertNotNull(loaded)
        repeat(2) {
            compose.onNodeWithTag("reader_memo_action").performScrollTo().performClick()
            compose.onNodeWithTag("memo_editor").assertExists()
            assertSame("Memo entry must not destroy the already rendered WebView", initial, reader())
            assertFalse(shadowOf(initial).wasDestroyCalled())
            compose.runOnIdle {
                val container = initial.parent as View
                assertEquals(View.INVISIBLE, container.visibility)
                assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, container.importantForAccessibility)
            }
            compose.onNodeWithTag("reader_actions").assertDoesNotExist()
            compose.onNodeWithTag("reader_memo_action", useUnmergedTree = true).assertIsNotEnabled()
            compose.onRoot().performKeyInput { repeat(6) { pressKey(Key.Tab) } }
            compose.onNodeWithTag("reader_memo_action", useUnmergedTree = true).assert(isFocused().not())
            compose.onNodeWithTag("memo_editor").assertExists()
            // A hardware Tab can insert a tab into this multiline field. Restore
            // the empty draft before checking the clean-cancel path.
            compose.onNodeWithText("기억할 생각이나 적용할 아이디어").performTextReplacement("")
            compose.onNodeWithContentDescription("뒤로").performClick()
            compose.onNodeWithTag("reader_pane").assertExists()
            assertSame(initial, reader())
            assertSame("Cancel must not reload the article HTML", loaded, shadowOf(reader()).lastLoadDataWithBaseURL)
        }
        compose.onNodeWithTag("reader_memo_action").performScrollTo().performClick()
        compose.onNodeWithText("기억할 생각이나 적용할 아이디어").performTextInput("버릴 초안")
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("계속 작성").performClick()
        assertSame(initial, reader())
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("변경 버리고 나가기").performClick()
        assertSame(initial, reader())
        compose.onNodeWithTag("reader_memo_action").performScrollTo().performClick()
        compose.onNodeWithText("기억할 생각이나 적용할 아이디어").performTextInput("저장할 메모")
        compose.onNodeWithText("메모 저장").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("메모 수정").fetchSemanticsNodes().isNotEmpty() }
        assertSame(initial, reader())
        assertSame("Save must not reload the article HTML", loaded, shadowOf(reader()).lastLoadDataWithBaseURL)
        compose.onNodeWithText("원문을 현재 목록에서 찾을 수 없어요. 저장한 메모와 북마크는 유지됩니다").assertDoesNotExist()
    }

    @Test
    fun cacheMetadataChangeDoesNotReloadButActualContentChangeDoes() {
        val vm = waitForNotes()
        val article = vm.articles.value.single { it.title == articleTitle }
        val displayed = mutableStateOf(article)
        compose.runOnUiThread {
            compose.activity.setContent { AxTheme { ReaderWebView(displayed.value, onError = { }) } }
        }
        compose.waitForIdle()
        val initial = reader()
        val loaded = shadowOf(initial).lastLoadDataWithBaseURL
        assertNotNull(loaded)
        compose.runOnIdle { displayed.value = article.copy(cachedAt = (article.cachedAt ?: 0L) + 1) }
        compose.waitForIdle()
        assertSame(initial, reader())
        assertSame("Non-rendered cache metadata must not trigger a blank HTML reload", loaded, shadowOf(reader()).lastLoadDataWithBaseURL)
        compose.runOnIdle { displayed.value = article.copy(bodyHtml = "<p>갱신한 본문</p>", bodyText = "갱신한 본문") }
        compose.waitForIdle()
        assertSame(initial, reader())
        assertNotSame(loaded, shadowOf(reader()).lastLoadDataWithBaseURL)
        assertTrue(shadowOf(reader()).lastLoadDataWithBaseURL.data.contains("갱신한 본문"))
    }

    @Test
    fun leavingReaderDestroysItsViewAndAnotherArticleDoesNotReuseIt() {
        val vm = waitForNotes()
        openArticle()
        val initial = reader()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.runOnIdle { assertTrue(webViews(compose.activity.window.decorView).isEmpty()) }
        assertTrue(shadowOf(initial).wasDestroyCalled())
        val another = vm.articles.value.first { it.title != articleTitle }
        compose.onNodeWithTag("notes_list").performScrollToNode(hasText(another.title))
        compose.onNodeWithText(another.title).performClick()
        compose.waitForIdle()
        val next = reader()
        assertNotSame(initial, next)
        assertTrue(shadowOf(next).lastLoadDataWithBaseURL.data.contains(another.title))
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.waitForIdle()
        assertTrue(shadowOf(next).wasDestroyCalled())
    }

    @Test
    fun recreationReleasesOldActivityViewAndRestoresReaderRoute() {
        waitForNotes()
        openArticle()
        val initial = reader()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("reader_pane").assertExists()
        compose.waitUntil(10000) { compose.runOnIdle { webViews(compose.activity.window.decorView).isNotEmpty() } }
        assertTrue(shadowOf(initial).wasDestroyCalled())
        assertNotSame(initial, reader())
        assertTrue(shadowOf(reader()).lastLoadDataWithBaseURL.data.contains(articleTitle))
    }

    @Test
    fun themeRepaintsSameViewAndFontScaleReflowsWithoutReloadingHtml() {
        val vm = waitForNotes()
        val article = vm.articles.value.single { it.title == articleTitle }
        val dark = mutableStateOf(false)
        val fontScale = mutableStateOf(1f)
        compose.runOnUiThread {
            compose.activity.setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale.value)) {
                    AxTheme(darkTheme = dark.value) { ReaderWebView(article, onError = { }) }
                }
            }
        }
        compose.waitForIdle()
        val initial = reader()
        compose.runOnIdle { dark.value = true }
        compose.waitForIdle()
        assertSame(initial, reader())
        val darkLoad = shadowOf(initial).lastLoadDataWithBaseURL
        assertEquals(buildReaderHtml(article, AxDarkColors), darkLoad.data)
        compose.runOnIdle { fontScale.value = 2f }
        compose.waitForIdle()
        assertSame(initial, reader())
        assertEquals(200, initial.settings.textZoom)
        assertSame(darkLoad, shadowOf(initial).lastLoadDataWithBaseURL)
    }

    private fun waitForNotes(): AxViewModel {
        val vm = compose.runOnIdle { ViewModelProvider(compose.activity)[AxViewModel::class.java] }
        compose.waitUntil(15000) { compose.runOnIdle { !vm.busy.value && vm.articles.value.any { it.title == articleTitle } } }
        return vm
    }
    private fun openArticle() {
        compose.onNodeWithTag("notes_list").performScrollToNode(hasText(articleTitle))
        compose.onNodeWithText(articleTitle).performClick()
        compose.waitUntil(10000) { compose.runOnIdle { webViews(compose.activity.window.decorView).isNotEmpty() } }
    }
    private fun reader(): WebView = compose.runOnIdle {
        val views = webViews(compose.activity.window.decorView)
        assertEquals("Exactly one reader WebView must remain attached", 1, views.size)
        views.single()
    }
    private fun webViews(view: View): List<WebView> = when (view) {
        is WebView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { webViews(view.getChildAt(it)) }
        else -> emptyList()
    }
    private fun resetFactory() {
        ViewModelProvider.AndroidViewModelFactory::class.java.getDeclaredField("_instance")
            .apply { isAccessible = true }.set(null, null)
    }
}
