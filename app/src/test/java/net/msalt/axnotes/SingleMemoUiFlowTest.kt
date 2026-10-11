package net.msalt.axnotes

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.msalt.axnotes.data.Memo
import net.msalt.axnotes.ui.AxViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w411dp-h891dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class SingleMemoUiFlowTest {
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

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()
    private val articleTitle = "AX Notes 앱도 만들어볼까?"
    private val bodyLabel = "기억할 생각이나 적용할 아이디어"
    private val legacyTitle = "  예전 제목\r\ne\u0301  "

    @Test
    fun singleTopMemoActionCreatesThenReopensOneBodyOnlyMemo() {
        val vm = waitForNotes()
        val articleId = vm.articles.value.single { it.title == articleTitle }.id
        openArticle()
        val actions = compose.onAllNodes(
            hasClickAction() and hasAnyAncestor(hasTestTag("reader_actions"))
        ).fetchSemanticsNodes().map { node ->
            node.config[SemanticsProperties.Text].joinToString("") { it.text }
        }
        assertEquals(listOf("북마크", "읽기 알림", "메모 남기기", "공유"), actions)
        compose.onAllNodesWithTag("reader_memo_action").assertCountEquals(1)
        compose.onNodeWithTag("reader_memo_action")
            .assert(hasAnyAncestor(hasTestTag("reader_actions")))
        compose.onNodeWithText("메모 0").assertDoesNotExist()
        compose.onNodeWithText("새 메모").assertDoesNotExist()
        openMemoAction("메모 남기기")
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(1)
        compose.onNodeWithText("제목 (선택)").assertDoesNotExist()
        compose.onNodeWithTag("legacy_memo_title").assertDoesNotExist()
        compose.onNodeWithText("메모 저장").assertIsNotEnabled()
        val body = "\n  \n첫 줄을 제목으로 표시\n다음 줄은 본문"
        compose.onNodeWithText(bodyLabel).performTextInput(body)
        saveAndWaitForReader("메모 수정")
        val first = storedMemos(vm, articleId).single()
        assertEquals("", first.title)
        assertEquals(body, first.body)

        openMemoAction("메모 수정")
        compose.onNodeWithText(bodyLabel).assertTextContains(body)
        compose.onNodeWithText(bodyLabel).performTextReplacement("\n첫 줄을 제목으로 표시\n수정된 본문")
        saveAndWaitForReader("메모 수정")
        val updated = storedMemos(vm, articleId).single()
        assertEquals(first.id, updated.id)
        assertEquals(first.createdAt, updated.createdAt)
        assertEquals("", updated.title)
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithTag("notes_list").assertExists()
        compose.onNodeWithTag("nav_library").performClick()
        compose.onNodeWithText("메모", substring = false).performClick()
        compose.onNodeWithTag("library_list").performScrollToNode(hasText("첫 줄을 제목으로 표시"))
        compose.onNodeWithText("첫 줄을 제목으로 표시", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("제목 없는 메모").assertDoesNotExist()
    }

    @Test
    fun existingReadOnlyTitleAndDirtyBodySurviveRecreationCancelAndSave() {
        val vm = waitForNotes()
        val articleId = vm.articles.value.single { it.title == articleTitle }.id
        val original = memo("legacy-single", articleId, legacyTitle, "원래 본문", 123L)
        seed(vm, original)
        openArticle()
        openMemoAction("메모 수정")
        assertReadOnlyTitle(legacyTitle)
        compose.onNodeWithText(bodyLabel).performTextReplacement("아직 저장하지 않은 수정")
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("저장하지 않은 메모가 있어요").assertExists()
        compose.onNodeWithText("계속 작성").performClick()
        assertReadOnlyTitle(legacyTitle)
        compose.onNodeWithText(bodyLabel).assertTextContains("아직 저장하지 않은 수정")
        assertEquals(original, storedMemos(vm, articleId).single())
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("변경 버리고 나가기").performClick()
        compose.onNodeWithTag("reader_pane").assertExists()
        assertEquals(original, storedMemos(vm, articleId).single())
        openMemoAction("메모 수정")
        compose.onNodeWithText(bodyLabel).assertTextContains(original.body)
        compose.onNodeWithText(bodyLabel).performTextReplacement("저장할 수정 본문")
        saveAndWaitForReader("메모 수정")
        assertPreservedIdentity(original, storedMemos(vm, articleId).single(), "저장할 수정 본문")
    }

    @Test
    fun multipleLegacyMemosRemainIndividuallyEditableWithoutAnewMemoEntry() {
        val vm = waitForNotes()
        val articleId = vm.articles.value.single { it.title == articleTitle }.id
        val first = memo("legacy-first", articleId, legacyTitle, "첫 레거시 본문", 123L)
        val second = memo("legacy-second", articleId, "두 번째 제목", "둘째 레거시 본문", 456L)
        seed(vm, first, second)
        openArticle()
        openMemoAction("메모 보기")
        compose.onNodeWithText("글에 남긴 메모").assertExists()
        compose.onNodeWithText("새 메모").assertDoesNotExist()
        compose.onNodeWithText("메모 남기기").assertDoesNotExist()
        compose.onNodeWithText(legacyTitle).performScrollTo().performClick()
        assertReadOnlyTitle(legacyTitle)
        compose.onNodeWithText(bodyLabel).performTextReplacement("첫 메모만 수정")
        compose.onNodeWithText("메모 저장").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("글에 남긴 메모").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("두 번째 제목").performScrollTo().assertIsDisplayed()
        val persisted = storedMemos(vm, articleId).associateBy { it.id }
        assertEquals(setOf(first.id, second.id), persisted.keys)
        assertPreservedIdentity(first, persisted.getValue(first.id), "첫 메모만 수정")
        assertEquals(second, persisted.getValue(second.id))
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithTag("reader_memo_action").assertTextEquals("메모 보기")
    }

    @Test
    fun orphanedMemoCanBeEditedFromLibraryReaderAndSearchWithoutLosingItsTitle() {
        val vm = waitForNotes()
        val original = memo("orphan-memo", "removed-article", legacyTitle, "orphan-marker 원래 본문", 321L)
        seed(vm, original)
        compose.onNodeWithTag("nav_library").performClick()
        compose.onNodeWithText("메모", substring = false).performClick()
        compose.onNodeWithText(legacyTitle).performScrollTo().performClick()
        assertReadOnlyTitle(legacyTitle)
        compose.onNodeWithText(bodyLabel).performTextReplacement("orphan-marker 보관함에서 수정")
        compose.onNodeWithText("메모 저장").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("library_list").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("글 읽기").performScrollTo().performClick()
        compose.onNodeWithText("원문을 현재 목록에서 찾을 수 없어요. 저장한 메모와 북마크는 유지됩니다").assertExists()
        openMemoAction("메모 수정")
        assertReadOnlyTitle(legacyTitle)
        compose.onNodeWithText(bodyLabel).assertTextContains("orphan-marker 보관함에서 수정")
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithTag("reader_pane").assertExists()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithTag("library_list").assertExists()
        compose.onNodeWithTag("nav_search").performClick()
        compose.onNodeWithText("글과 메모에서 검색").performTextInput("orphan-marker")
        compose.waitUntil(10000) { compose.onAllNodesWithText(legacyTitle).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(legacyTitle).performScrollTo().performClick()
        assertReadOnlyTitle(legacyTitle)
        compose.onNodeWithText(bodyLabel).performTextReplacement("orphan-marker 검색에서 수정")
        compose.onNodeWithText("메모 저장").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("search_results").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("글과 메모에서 검색").assertTextContains("orphan-marker")
        assertPreservedIdentity(original, storedMemos(vm, original.articleId).single(), "orphan-marker 검색에서 수정")
    }

    @Test
    fun anotherMemoArrivingWhileNewEditorIsOpenDoesNotOverwriteItOrLoseDraft() {
        val vm = waitForNotes()
        val articleId = vm.articles.value.single { it.title == articleTitle }.id
        openArticle()
        openMemoAction("메모 남기기")
        compose.onNodeWithText(bodyLabel).performTextInput("아직 저장되지 않은 초안")
        val incoming = memo("arrived-while-editing", articleId, "기존 기록", "이미 저장된 본문", 789L)
        seed(vm, incoming)
        compose.onNodeWithText("메모 저장").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("이 글에 메모가 이미 있습니다. 기존 메모를 수정하세요")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("memo_editor").assertExists()
        compose.onNodeWithText(bodyLabel).assertTextContains("아직 저장되지 않은 초안")
        assertEquals(listOf(incoming), storedMemos(vm, articleId))
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("변경 버리고 나가기").performClick()
        openMemoAction("메모 수정")
        compose.onNodeWithText(bodyLabel).assertTextContains(incoming.body)
        assertReadOnlyTitle(incoming.title)
    }

    @Test
    fun twoHundredPercentTextKeepsLegacyTitleBodyAndSaveReachable() {
        val vm = waitForNotes()
        val articleId = vm.articles.value.single { it.title == articleTitle }.id
        val original = memo("large-text-legacy", articleId, "작은 화면에서도 보존할 예전 제목", "수정 전", 456L)
        seed(vm, original)
        compose.runOnIdle { RuntimeEnvironment.setFontScale(2f) }
        compose.activityRule.scenario.recreate()
        openArticle()
        openMemoAction("메모 수정")
        compose.onNodeWithTag("legacy_memo_title").performScrollTo().assertIsDisplayed()
        assertReadOnlyTitle(original.title)
        compose.onNodeWithText(bodyLabel).performScrollTo().performTextReplacement("큰 글자에서도 저장")
        compose.onNodeWithText("메모 저장").performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("reader_pane").fetchSemanticsNodes().isNotEmpty() }
        assertPreservedIdentity(original, storedMemos(vm, articleId).single(), "큰 글자에서도 저장")
    }

    private fun waitForNotes(): AxViewModel {
        val vm = compose.runOnIdle { ViewModelProvider(compose.activity)[AxViewModel::class.java] }
        compose.waitUntil(15000) {
            compose.onNodeWithTag("notes_list").fetchSemanticsNode()
            !vm.busy.value && vm.articles.value.any { it.title == articleTitle }
        }
        return vm
    }

    private fun openArticle() {
        compose.onNodeWithTag("notes_list").performScrollToNode(hasText(articleTitle))
        compose.onNodeWithText(articleTitle).performClick()
        compose.onNodeWithTag("reader_pane").assertExists()
    }

    private fun openMemoAction(label: String) {
        compose.onNodeWithTag("reader_memo_action").assertTextEquals(label).performScrollTo().performClick()
    }

    private fun saveAndWaitForReader(label: String) {
        compose.onNodeWithText("메모 저장").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("reader_memo_action").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertReadOnlyTitle(title: String) {
        compose.onNodeWithTag("legacy_memo_title").assertTextEquals(title)
            .assert(hasSetTextAction().not())
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(1)
        compose.onNodeWithText("제목 (선택)").assertDoesNotExist()
    }

    private fun seed(vm: AxViewModel, vararg memos: Memo) {
        compose.runOnIdle { runBlocking(Dispatchers.IO) { memos.forEach { vm.graph.personal.dao.putMemo(it) } } }
        compose.waitUntil(10000) {
            compose.runOnIdle { memos.all { incoming -> vm.memos.value.any { it.id == incoming.id } } }
        }
    }

    private fun storedMemos(vm: AxViewModel, articleId: String) =
        runBlocking(Dispatchers.IO) { vm.graph.personal.dao.memosFor(articleId) }

    private fun memo(id: String, articleId: String, title: String, body: String, timestamp: Long) =
        Memo(id, articleId, articleTitle, "https://ax.msalt.net/notes/$articleId/", title, body, timestamp, timestamp)

    private fun assertPreservedIdentity(before: Memo, after: Memo, body: String) {
        assertEquals(before.id, after.id)
        assertEquals(before.articleId, after.articleId)
        assertArrayEquals(before.title.toByteArray(Charsets.UTF_8), after.title.toByteArray(Charsets.UTF_8))
        assertEquals(before.createdAt, after.createdAt)
        assertEquals(body, after.body)
    }

    private fun resetFactory() {
        ViewModelProvider.AndroidViewModelFactory::class.java.getDeclaredField("_instance")
            .apply { isAccessible = true }.set(null, null)
    }
}
