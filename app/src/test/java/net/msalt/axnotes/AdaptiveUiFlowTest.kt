package net.msalt.axnotes

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.flow.MutableStateFlow
import net.msalt.axnotes.ui.AxApp
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w1280dp-h800dp-land-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class AdaptiveUiFlowTest {
    @get:Rule(order = 0) val work = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                .putBoolean("sample", true).putBoolean("liveFeedDefaultV3", true).commit()
            WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().setExecutor(SynchronousExecutor()).build())
        }
        override fun after() { WorkManagerTestInitHelper.closeWorkDatabase() }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val articleTitle = "AX Notes 앱도 만들어볼까?"
    private fun waitForNotes() {
        compose.waitUntil(15000) { compose.onAllNodesWithText(articleTitle).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun openEditor() {
        waitForNotes()
        compose.onNodeWithText(articleTitle).performClick()
        compose.onNodeWithText("메모").performClick()
        compose.onNodeWithText("새 메모").performClick()
        compose.onNodeWithText("기억할 생각이나 적용할 아이디어").performTextInput("태블릿에서도 지킬 생각")
    }

    @Test @Config(qualifiers = "w411dp-h891dp-port-mdpi")
    fun compactWindowRetainsPhoneFlow() {
        waitForNotes()
        compose.onNodeWithTag("bottom_navigation").assertExists()
        compose.onNodeWithTag("navigation_rail").assertDoesNotExist()
        compose.onNodeWithText(articleTitle).performClick()
        compose.onNodeWithTag("reader_pane").assertExists()
        compose.onNodeWithTag("notes_list").assertDoesNotExist()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithTag("notes_list").assertExists()
    }

    @Test @Config(qualifiers = "w800dp-h1280dp-port-mdpi")
    fun mediumWindowUsesRailWithoutSqueezingReader() {
        waitForNotes()
        compose.onNodeWithTag("navigation_rail").assertExists()
        compose.onNodeWithTag("list_detail_workspace").assertDoesNotExist()
        compose.onNodeWithText(articleTitle).performClick()
        compose.onNodeWithTag("reader_pane").assertExists()
        compose.onNodeWithTag("notes_list").assertDoesNotExist()
    }

    @Test fun expandedWindowKeepsListBesideSelectedArticle() {
        waitForNotes()
        compose.onNodeWithTag("list_detail_workspace").assertExists()
        compose.onNodeWithTag("reader_placeholder").assertExists()
        compose.onNodeWithText(articleTitle).performClick()
        compose.onNodeWithTag("reader_pane").assertExists()
        compose.onNodeWithTag("notes_list").assertExists()
        compose.onNodeWithTag("reader_placeholder").assertDoesNotExist()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithTag("reader_placeholder").assertExists()
    }

    @Test fun persistentRailCannotDiscardDirtyMemoWithoutConfirmation() {
        openEditor()
        compose.onNodeWithTag("nav_settings").performClick()
        compose.onNodeWithText("저장하지 않은 메모가 있어요").assertExists()
        compose.onNodeWithText("계속 작성").performClick()
        compose.onNodeWithText("태블릿에서도 지킬 생각").assertExists()
        compose.onNodeWithText(articleTitle).performClick()
        compose.onNodeWithText("저장하지 않은 메모가 있어요").assertExists()
        compose.onNodeWithText("계속 작성").performClick()
        compose.onNodeWithTag("nav_library").performClick()
        compose.onNodeWithText("변경 버리고 나가기").performClick()
        compose.onNodeWithText("나만의 읽기 공간").assertExists()
        compose.onNodeWithTag("memo_editor").assertDoesNotExist()
    }

    @Test fun recreationRetainsDraftAndPendingDiscardDestination() {
        openEditor()
        compose.onNodeWithTag("nav_settings").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("저장하지 않은 메모가 있어요").assertExists()
        compose.onNodeWithText("계속 작성").performClick()
        compose.onNodeWithText("태블릿에서도 지킬 생각").assertExists()
        compose.onNodeWithTag("nav_settings").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("변경 버리고 나가기").performClick()
        compose.onNodeWithText("설정 및 소개").assertExists()
    }

    @Test fun resizingExistingWindowPreservesDraftAndListNavigation() {
        val width = mutableStateOf(1280.dp)
        val target = MutableStateFlow<String?>(null)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.width(width.value).fillMaxHeight()) { AxApp(target) }
                }
            }
        }
        openEditor()
        compose.onNodeWithTag("list_detail_workspace").assertExists()
        compose.runOnIdle { width.value = 700.dp }
        compose.onNodeWithTag("list_detail_workspace").assertDoesNotExist()
        compose.onNodeWithText("태블릿에서도 지킬 생각").assertExists()
        compose.runOnIdle { width.value = 1280.dp }
        compose.onNodeWithTag("list_detail_workspace").assertExists()
        compose.onNodeWithText("태블릿에서도 지킬 생각").assertExists()
        compose.onNodeWithTag("notes_list").assertExists()
    }
    @Test fun largeTextKeepsEditorAndNavigationReachable() {
        val target = MutableStateFlow<String?>(null)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.width(900.dp).fillMaxHeight()) { AxApp(target) }
                    }
                }
            }
        }
        // At 200% text, a 900dp window intentionally uses one readable pane.
        compose.onNodeWithTag("navigation_rail").assertExists()
        compose.onNodeWithTag("list_detail_workspace").assertDoesNotExist()
        waitForNotes()
        compose.onNodeWithTag("notes_list").performScrollToNode(hasText(articleTitle))
        compose.onNodeWithText(articleTitle).performClick()
        compose.onNodeWithText("메모").performClick()
        compose.onNodeWithText("새 메모").performClick()
        compose.onNodeWithText("기억할 생각이나 적용할 아이디어").performScrollTo().performTextInput("큰 글자 메모")
        compose.onNodeWithText("메모 저장").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("nav_settings").performScrollTo().performClick()
        compose.onNodeWithText("저장하지 않은 메모가 있어요").assertExists()
    }

    @Test fun expandedWorkspacePreservesBookmarkMemoSearchAndReminderCancel() {
        waitForNotes()
        compose.onNodeWithText(articleTitle).performClick()
        compose.onNodeWithText("북마크").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("북마크됨").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("읽기 알림").performClick()
        compose.onNodeWithText("나중에 읽기").assertExists()
        compose.onNodeWithText("취소").performClick()
        compose.onNodeWithTag("reader_pane").assertExists()
        compose.onNodeWithText("메모").performClick()
        compose.onNodeWithText("새 메모").performClick()
        compose.onNodeWithText("기억할 생각이나 적용할 아이디어").performTextInput("태블릿 검색 검증용 생각")
        compose.onNodeWithText("메모 저장").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("태블릿 검색 검증용 생각").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("nav_library").performClick()
        compose.onNodeWithText(articleTitle).assertExists()
        compose.onNodeWithText(articleTitle).performClick()
        compose.onNodeWithTag("library_list").assertExists()
        compose.onNodeWithTag("reader_pane").assertExists()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("나만의 읽기 공간").assertExists()
        compose.onNodeWithTag("reader_placeholder").assertExists()
        compose.onNodeWithTag("nav_search").performClick()
        compose.onNodeWithText("글과 메모에서 검색").performTextInput("검색 검증용")
        compose.waitUntil(10000) { compose.onAllNodesWithText("태블릿 검색 검증용 생각").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("search_results").assertExists()
        compose.onNodeWithTag("list_detail_workspace").assertExists()
        compose.onNodeWithText("글 읽기").performScrollTo().performClick()
        compose.onNodeWithTag("search_results").assertExists()
        compose.onNodeWithTag("reader_pane").assertExists()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithTag("reader_placeholder").assertExists()
        compose.onNodeWithText("검색 검증용").assertExists()
    }

    @Test fun retainedSampleCacheIsNotLabelledAsLiveAfterPreferenceChanges() {
        waitForNotes()
        compose.runOnIdle {
            androidx.lifecycle.ViewModelProvider(compose.activity)[net.msalt.axnotes.ui.AxViewModel::class.java]
                .sampleMode.value = false
        }
        compose.onNodeWithText("이전에 저장한 글을 표시하고 있어요.", substring = true).assertExists()
        compose.onNodeWithText("AX Notes 웹 콘텐츠").assertDoesNotExist()
    }

}
