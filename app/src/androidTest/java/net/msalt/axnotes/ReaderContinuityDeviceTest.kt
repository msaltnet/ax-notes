package net.msalt.axnotes

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.WebView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import java.io.File

/** Runs on the disposable internal-build emulator with bundled public fixture articles. */
@RunWith(AndroidJUnit4::class)
class ReaderContinuityDeviceTest {
    @get:Rule(order = 0)
    val fixture = object : ExternalResource() {
        override fun before() {
            recoverDisposableEmulatorDialogIfRequested()
            InstrumentationRegistry.getInstrumentation().targetContext
                .getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                .putBoolean("sample", true).putBoolean("liveFeedDefaultV3", true).commit()
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun memoRoundTripKeepsRenderedReaderAndScrollPosition() {
        val title = "AX Notes 앱도 만들어볼까?"
        compose.waitUntil(60000) { compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("notes_list").performScrollToNode(hasText(title))
        compose.onNodeWithText(title).performClick()
        capture("reader-entry")
        waitForRenderedReader()
        val original = reader()
        compose.runOnIdle { original.scrollTo(0, 200) }
        val scrollY = compose.runOnIdle { original.scrollY }
        assertTrue("Use a real scrolled document for the round trip", scrollY > 0)
        capture("reader-before-memo")
        if (InstrumentationRegistry.getArguments().getString("skipOverflowCheck") != "true") checkOverflowControls()
        compose.onNodeWithTag("reader_memo_action").performScrollTo().performTouchInput { click() }
        capture("memo-editor")
        compose.onNodeWithContentDescription("뒤로").performClick()
        waitForRenderedReader()
        capture("reader-after-memo")
        assertSame("Returning from memo must reuse the rendered document", original, reader())
        assertEquals("Memo return must keep the current reading position", scrollY, compose.runOnIdle { original.scrollY })
        compose.onNodeWithText("원문을 현재 목록에서 찾을 수 없어요. 저장한 메모와 북마크는 유지됩니다").assertDoesNotExist()
        compose.onNodeWithTag("reader_memo_action").performScrollTo().performTouchInput { click() }
        compose.onNodeWithText("기억할 생각이나 적용할 아이디어").performTextReplacement("실행 중인 읽기 화면을 유지하는 메모")
        capture("memo-editor-with-input")
        compose.onNodeWithText("메모 저장").performScrollTo().performClick()
        compose.waitUntil(60000) { compose.onAllNodesWithTag("reader_memo_action").fetchSemanticsNodes().isNotEmpty() }
        capture("reader-after-save")
        assertSame("Saving a memo must also reuse the rendered document", original, reader())
        assertEquals(scrollY, compose.runOnIdle { original.scrollY })
    }

    // Explicit, test-only recovery for the software emulator used in this review.
    // Never enabled by default and never dismisses an app ANR or another dialog.
    private fun recoverDisposableEmulatorDialogIfRequested() {
        if (InstrumentationRegistry.getArguments().getString("recoverSystemUiAnr") != "true") return
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        capture("emulator-recovery-entry")
        // A newly connected UiAutomation can initially have no active root.
        var root: AccessibilityNodeInfo? = null
        val deadline = SystemClock.uptimeMillis() + 10000
        do {
            root = automation.rootInActiveWindow
            if (root?.findAccessibilityNodeInfosByText("System UI isn't responding")?.any {
                    it.packageName?.toString() == "android" && it.text?.toString() == "System UI isn't responding"
                } == true) break
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        val dialog = root ?: return
        if (dialog.findAccessibilityNodeInfosByText("System UI isn't responding").none {
                it.packageName?.toString() == "android" && it.text?.toString() == "System UI isn't responding"
            }) return
        val wait = dialog.findAccessibilityNodeInfosByText("Wait").singleOrNull {
            it.packageName?.toString() == "android" && it.text?.toString() == "Wait" && it.isClickable
        } ?: error("The exact System UI ANR Wait action was unavailable")
        capture("emulator-system-ui-anr-before-wait")
        check(wait.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        android.util.Log.i("ReaderContinuity", "Selected Wait once for the disposable emulator System UI ANR")
    }

    private fun checkOverflowControls() {
        var pages = 0
        while (compose.onAllNodesWithTag("reader_actions_next").fetchSemanticsNodes().isNotEmpty() && pages++ < 8) {
            compose.onNodeWithTag("reader_actions_next").performTouchInput { click() }
            compose.waitForIdle()
        }
        assertTrue("Native paging must reach the trailing action", pages < 8)
        compose.onNodeWithText("공유").assertIsDisplayed()
        capture("reader-actions-end")
        if (pages > 0) {
            compose.onNodeWithTag("reader_actions_previous").performTouchInput { click() }
            compose.waitForIdle()
            compose.onNodeWithTag("reader_actions_next").assertIsDisplayed()
        }
    }

    private fun waitForRenderedReader() {
        try {
            compose.waitUntil(120000) { compose.runOnIdle {
                webViews(compose.activity.window.decorView).singleOrNull()?.let {
                    it.contentHeight > 100 && it.width > 0 && it.height > 0 && it.isShown
                } == true
            } }
            val committed = AtomicBoolean(false)
            val rendered = reader()
            compose.runOnIdle {
                rendered.postVisualStateCallback(1L, object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) { committed.set(true) }
                })
            }
            compose.waitUntil(60000) { committed.get() }
        } catch (error: Throwable) {
            capture("reader-timeout")
            val details = compose.runOnIdle {
                webViews(compose.activity.window.decorView).joinToString {
                    "progress=${it.progress}, contentHeight=${it.contentHeight}, size=${it.width}x${it.height}, visible=${it.isShown}"
                }
            }
            throw AssertionError("Native reader did not finish rendering: $details", error)
        }
    }

    private fun reader(): WebView = compose.runOnIdle { webViews(compose.activity.window.decorView).single() }
    private fun webViews(view: View): List<WebView> = when (view) {
        is WebView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { webViews(view.getChildAt(it)) }
        else -> emptyList()
    }
    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
        val folder = File(instrumentation.targetContext.filesDir, "reader-continuity-evidence").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }
}
