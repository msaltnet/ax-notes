package net.msalt.axnotes

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class DeviceUiFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun waitForNotes() { compose.waitUntil(15000) { compose.onAllNodesWithText("AX Notes 앱도 만들어볼까?").fetchSemanticsNodes().isNotEmpty() } }
    @Test fun repeatedNavigationReturnsToStableNotesAndLibrary() {
        waitForNotes()
        repeat(3) {
            compose.onNodeWithText("내 보관함").performClick()
            compose.onNodeWithText("나만의 읽기 공간").assertExists()
            compose.onNodeWithText("Notes").performClick()
            compose.onNodeWithText("AX Notes 앱도 만들어볼까?").assertExists()
        }
    }
    @Test fun dirtyMemoBackCanContinueOrDiscard() {
        waitForNotes()
        compose.onNodeWithText("AX Notes 앱도 만들어볼까?").performClick()
        compose.onNodeWithText("메모").performClick()
        compose.onNodeWithText("새 메모").performClick()
        compose.onNodeWithText("기억할 생각이나 적용할 아이디어").performTextInput("잃으면 안 되는 생각")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("저장하지 않은 메모가 있어요").assertExists()
        compose.onNodeWithText("계속 작성").performClick()
        compose.onNodeWithText("잃으면 안 되는 생각").assertExists()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("변경 버리고 나가기").performClick()
        compose.onNodeWithText("글에 남긴 메모").assertExists()
    }
    @Test fun searchHasEmptyGuidanceAndLiteralNoResultState() {
        waitForNotes()
        compose.onNodeWithContentDescription("검색").performClick()
        compose.onNodeWithText("기억나는 단어를 입력하세요").assertExists()
        compose.onNodeWithText("글과 메모에서 검색").performTextInput("qzx%_'123987")
        compose.waitUntil(10000) { compose.onAllNodesWithText("검색 결과가 없어요").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("검색어 지우기").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("기억나는 단어를 입력하세요").fetchSemanticsNodes().isNotEmpty() }
    }
}
