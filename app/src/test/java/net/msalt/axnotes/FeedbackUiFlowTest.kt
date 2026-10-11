package net.msalt.axnotes

import android.content.Context
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.msalt.axnotes.data.Article
import net.msalt.axnotes.ui.AxTheme
import net.msalt.axnotes.ui.AxViewModel
import net.msalt.axnotes.ui.NotesRefreshBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w411dp-h891dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class FeedbackUiFlowTest {
    @get:Rule(order = 0)
    val work = object : ExternalResource() {
        override fun before() {
            // Lifecycle 2.9.0 caches the first Application here, but Robolectric creates
            // a new Application for each test. Reset only that factory's test-global
            // cache before MainActivity starts so database/personal state cannot leak.
            resetViewModelFactoryApplication()
            org.robolectric.RuntimeEnvironment.setFontScale(1f)
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                .putBoolean("sample", true)
                .putBoolean("liveFeedDefaultV3", true)
                .commit()
            WorkManagerTestInitHelper.initializeTestWorkManager(
                context,
                Configuration.Builder().setExecutor(SynchronousExecutor()).build()
            )
        }

        override fun after() {
            WorkManagerTestInitHelper.closeWorkDatabase()
            resetViewModelFactoryApplication()
            org.robolectric.RuntimeEnvironment.setFontScale(1f)
        }
    }

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val sampleTitle = "AX Notes 앱도 만들어볼까?"
    private val seriesTitle = "자동화 시작"
    private val projectTitle = "AX 앱 만들기"
    private val firstTitle = "첫 번째 단계"
    private val secondTitle = "두 번째 단계"
    private val projectFirstTitle = "프로젝트 첫 단계"
    private val projectSecondTitle = "프로젝트 다음 단계"
    private val unrelatedTitle = "자동화 시작 · 제목만 닮은 글"
    private val authorName = "회귀 확인 작성자"

    @Test
    fun compactNavigationIsIconOnlyAccessibleAnd56DpWithoutSystemInset() {
        waitForInitialFeed()
        compose.onNodeWithTag("bottom_navigation").assertExists()
        compose.onNodeWithTag("navigation_rail").assertDoesNotExist()

        listOf("notes" to "Notes", "library" to "내 보관함", "search" to "검색", "settings" to "설정").forEach { (route, label) ->
            // Destination bounds measure the 56dp content, excluding the system bar inset.
            compose.onNodeWithTag("nav_$route")
                .assertContentDescriptionEquals(label)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
                .assertHasClickAction()
                .assertHeightIsEqualTo(56.dp)
                .assertWidthIsAtLeast(48.dp)
            compose.onAllNodes(
                hasText(label) and hasAnyAncestor(hasTestTag("bottom_navigation")),
                useUnmergedTree = true
            ).assertCountEquals(0)
        }

        compose.onNodeWithTag("nav_notes").assertIsSelected()
        compose.onNodeWithContentDescription("내 보관함").performClick()
        compose.onNodeWithTag("nav_library").assertIsSelected()
        compose.onNodeWithTag("nav_notes").assertIsNotSelected()
        compose.onNodeWithText("나만의 읽기 공간").assertIsDisplayed()
        compose.onNodeWithContentDescription("Notes").performClick()
        compose.onNodeWithTag("nav_notes").assertIsSelected()
        compose.onNodeWithTag("notes_list").assertExists()
    }

    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun editorialMastheadGrowsAtTwoHundredPercentWithoutClippingOrShrinkingText() {
        val baseline = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText("AX.", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(baseline) }
        val baselineHeight = baseline.single().size.height
        // Change the actual Android font configuration, including the platform
        // font resolver, rather than only replacing Compose's LocalDensity.
        compose.runOnIdle { org.robolectric.RuntimeEnvironment.setFontScale(2f) }
        compose.activityRule.scenario.recreate()
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.waitUntil(5000) {
            layouts.clear()
            compose.onNodeWithText("AX.", useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
            layouts.singleOrNull()?.layoutInput?.density?.fontScale == 2f
        }
        val layout = layouts.single()
        compose.onNodeWithText("AX.", useUnmergedTree = true).assertIsDisplayed()
        // A single line trims outer leading; compare actual glyph layout growth
        // rather than requiring the nominal inter-line height on its outer bounds.
        assertTrue("System text must enlarge the masthead: ${layout.size.height} <= $baselineHeight", layout.size.height > baselineHeight)
        assertFalse("Editorial masthead must have room for scaled leading", layout.didOverflowHeight)
        assertEquals(1, layout.lineCount)
        assertEquals(3, layout.getLineEnd(0, visibleEnd = true))
        // Text in a wrap-content Row can retain a wider paragraph constraint even
        // when every glyph fits. Check the actual rendered line, not that constraint.
        assertTrue("Every masthead glyph must fit its bounds", layout.getLineRight(0) <= layout.size.width + 1f)
        listOf("notes", "library", "search", "settings").forEach { route ->
            compose.onNodeWithTag("nav_$route").assertIsDisplayed().assertHeightIsEqualTo(56.dp)
        }
    }

    @Test
    fun editorialSearchShortcutAndMarginMemoUseExistingProtectedFlows() {
        waitForInitialFeed()
        compose.onNodeWithText("실험을 읽는 시간").assertIsDisplayed()
        compose.onNodeWithTag("notes_search").assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("글과 메모에서 검색").assertExists()
        compose.onNodeWithContentDescription("뒤로").performClick()
        openArticle(sampleTitle)
        compose.onNodeWithText("연결된 메모 0").performScrollTo().assertHasClickAction()
        compose.onNodeWithText("메모 남기기").performScrollTo().performClick()
        compose.onNodeWithTag("memo_editor").assertExists()
        compose.onNodeWithText("기억할 생각이나 적용할 아이디어").performTextInput("편집 디자인에서도 지킬 생각")
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("저장하지 않은 메모가 있어요").assertExists()
        compose.onNodeWithText("계속 작성").performClick()
        compose.onNodeWithText("메모 저장").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("reader_pane").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("연결된 메모 1").performScrollTo().performClick()
        compose.onNodeWithText("편집 디자인에서도 지킬 생각").assertExists()
    }

    @Test
    fun bundledFontLicenseCanBeReadOfflineAndDismissed() {
        waitForInitialFeed()
        compose.onNodeWithContentDescription("설정").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("글꼴 라이선스"))
        compose.onNodeWithText("글꼴 라이선스").performClick()
        compose.onNodeWithText("나눔명조 · SIL OFL 1.1").assertExists()
        compose.onNodeWithText("Copyright (c) 2010, NHN Corporation", substring = true).assertExists()
        compose.onNodeWithText("닫기").performClick()
        compose.onNodeWithText("나눔명조 · SIL OFL 1.1").assertDoesNotExist()
        compose.onNodeWithText("설정 및 소개").assertExists()
    }

    @Test
    fun notesExposeRefreshAsAnAccessibilityActionWithoutRefreshOrTestControls() {
        waitForInitialFeed()
        val actions = compose.onNodeWithTag("pull_refresh").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        assertEquals(listOf("새로고침"), actions.map { it.label })
        assertNoRefreshButtonOrInternalWording()

        compose.onNodeWithContentDescription("설정").performClick()
        compose.onNodeWithText("설정 및 소개").assertExists()
        assertNoRefreshButtonOrInternalWording()
        compose.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText("콘텐츠 캐시만 삭제"))
        assertNoRefreshButtonOrInternalWording()
    }

    @Test
    fun accessibilityRefreshRunsOnceAndRejectsAnotherActionWhileRefreshing() {
        val refreshing = mutableStateOf(false)
        var refreshCalls = 0
        showRefreshFixture({ refreshing.value }) {
            refreshCalls++
            refreshing.value = true
        }

        assertTrue(invokeRefreshAction())
        compose.runOnIdle { assertEquals(1, refreshCalls) }
        assertFalse(invokeRefreshAction())
        compose.runOnIdle { assertEquals(1, refreshCalls) }
    }

    @Test
    fun downwardSwipeRefreshesExactlyOnceAndIsIgnoredWhileRefreshing() {
        val refreshing = mutableStateOf(false)
        var refreshCalls = 0
        showRefreshFixture({ refreshing.value }) {
            refreshCalls++
            refreshing.value = true
        }

        compose.onNodeWithTag("pull_refresh").performTouchInput {
            swipeDown(startY = height * 0.1f, endY = height * 0.85f, durationMillis = 300)
        }
        compose.runOnIdle { assertEquals(1, refreshCalls) }
        compose.onNodeWithTag("pull_refresh").performTouchInput {
            swipeDown(startY = height * 0.1f, endY = height * 0.85f, durationMillis = 300)
        }
        compose.runOnIdle { assertEquals(1, refreshCalls) }
    }

    @Test
    fun slowDragRefreshesOnlyOnReleaseAndOnlyOnce() {
        val refreshing = mutableStateOf(false)
        var refreshCalls = 0
        showRefreshFixture({ refreshing.value }) {
            refreshCalls++
            refreshing.value = true
        }

        compose.onNodeWithTag("pull_refresh").performTouchInput {
            down(Offset(centerX, height * 0.1f))
            repeat(12) { index ->
                moveTo(Offset(centerX, height * (0.1f + (index + 1) * 0.06f)), delayMillis = 60)
            }
        }
        compose.runOnIdle { assertEquals(0, refreshCalls) }
        compose.onNodeWithTag("pull_refresh").performTouchInput { up() }
        compose.runOnIdle { assertEquals(1, refreshCalls) }
    }

    @Test
    fun readerKeepsBookmarkMemoReminderAndShareWithoutPromotionalActions() {
        seedCollections()
        openArticle(firstTitle)
        listOf("북마크", "메모", "읽기 알림", "공유").forEach { label ->
            compose.onNodeWithText(label).assertHasClickAction()
        }
        listOf("원문", "원문 보기", "프로젝트", "작성자", authorName).forEach { label ->
            compose.onNodeWithText(label).assertDoesNotExist()
        }
        compose.onAllNodesWithText("소개", substring = true).assertCountEquals(0)

        compose.onNodeWithText("북마크").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("북마크됨").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("메모").performClick()
        compose.onNodeWithText("글에 남긴 메모").assertExists()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("읽기 알림").performScrollTo().performClick()
        compose.onNodeWithText("나중에 읽기").assertExists()
        compose.onNodeWithText("취소").performClick()
        compose.onNodeWithTag("reader_pane").assertExists()

        compose.runOnIdle {
            assertNull("Opening the reader must not launch an external activity", shadowOf(compose.activity).nextStartedActivity)
        }
        compose.onNodeWithText("공유").performScrollTo().performClick()
        compose.runOnIdle {
            val chooser = shadowOf(compose.activity).nextStartedActivity
            assertEquals(Intent.ACTION_CHOOSER, chooser.action)
            @Suppress("DEPRECATION")
            val share = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
            assertEquals(Intent.ACTION_SEND, share.action)
            assertEquals("text/plain", share.type)
            assertEquals("$firstTitle\nhttps://ax.msalt.net/notes/feedback-first/", share.getStringExtra(Intent.EXTRA_TEXT))
        }
    }

    @Test
    fun savedBodyExplanationCanCloseAndReopenWithoutLeavingReader() {
        seedCollections()
        openArticle(firstTitle)
        repeat(2) {
            compose.onNodeWithText("본문 저장됨").performClick()
            compose.onNodeWithText("저장된 본문").assertExists()
            compose.onNodeWithText("인터넷 없이 다시 읽을 수 있어요", substring = true).assertExists()
            compose.onNodeWithText("북마크·메모·읽기 알림은 본문 저장과 별개", substring = true).assertExists()
            compose.onNodeWithText("확인").performClick()
            compose.onNodeWithText("저장된 본문").assertDoesNotExist()
            compose.onNodeWithTag("reader_pane").assertExists()
        }
    }

    @Test
    fun seriesGroupsUseAuthoredMembershipAndPreserveSelectionAfterArticleReturn() {
        seedCollections()
        compose.onNodeWithTag("collection_전체").assertIsSelected()
        compose.onNodeWithTag("collection_시리즈").performClick().assertIsSelected()
        compose.onNode(hasText(seriesTitle) and hasText("글 2개")).assertExists()
        compose.onNode(hasText("협업 실험") and hasText("글 1개")).assertExists()
        compose.onNodeWithText(unrelatedTitle).assertDoesNotExist()
        compose.onNodeWithText(seriesTitle).performClick()
        assertArticleOrder(firstTitle, secondTitle)
        compose.onNodeWithText(unrelatedTitle).assertDoesNotExist()

        openArticle(firstTitle)
        compose.onNodeWithContentDescription("뒤로").performClick()
        scrollNotesToTop()
        compose.onNodeWithTag("collection_시리즈").assertIsSelected()
        compose.onNodeWithText("시리즈 목록").assertExists()
        compose.onNodeWithText(seriesTitle).assertExists()
        assertArticleOrder(firstTitle, secondTitle)

        scrollNotesToTop()
        compose.onNodeWithText("시리즈 목록").performClick()
        compose.onNode(hasText(seriesTitle) and hasText("글 2개")).assertExists()
        compose.onNodeWithText(seriesTitle).performClick()
        pressSystemBack()
        compose.onNode(hasText(seriesTitle) and hasText("글 2개")).assertExists()
        compose.onNodeWithTag("collection_시리즈").assertIsSelected()
        pressSystemBack()
        compose.onNodeWithTag("collection_전체").assertIsSelected()
    }

    @Test
    fun projectGroupsApplyTheirOwnOrderAndPreserveSelectionAfterArticleReturn() {
        seedCollections()
        compose.onNodeWithTag("collection_프로젝트").performClick().assertIsSelected()
        compose.onNode(hasText(projectTitle) and hasText("글 2개")).assertExists()
        compose.onNode(hasText("문서 도구") and hasText("글 1개")).assertExists()
        compose.onNodeWithText(projectTitle).performClick()
        assertArticleOrder(projectFirstTitle, projectSecondTitle)
        compose.onNodeWithText(unrelatedTitle).assertDoesNotExist()
        openArticle(projectFirstTitle)
        pressSystemBack()
        scrollNotesToTop()
        compose.onNodeWithTag("collection_프로젝트").assertIsSelected()
        compose.onNodeWithText(projectTitle).assertExists()
        compose.onNodeWithText("프로젝트 목록").performClick()
        compose.onNode(hasText(projectTitle) and hasText("글 2개")).assertExists()
        pressSystemBack()
        compose.onNodeWithTag("collection_전체").assertIsSelected()
    }

    @Test
    fun recreationKeepsTheSelectedCollectionAndItsScrolledArticlePosition() {
        val extraArticles = (3..30).map { order ->
            fixtureArticle("feedback-step-$order", "이어지는 단계 $order", "2026-02-01").copy(
                seriesId = "series-a", seriesTitle = seriesTitle, seriesOrder = order
            )
        }
        seedCollections(extraArticles)
        compose.onNodeWithTag("collection_시리즈").performClick()
        compose.onNodeWithText(seriesTitle).performClick()
        val title = "이어지는 단계 20"
        compose.onNodeWithTag("notes_list").performScrollToNode(hasText(title))
        val previousTop = compose.onNodeWithText(title).assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot.top

        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        val restoredTop = compose.onNodeWithText(title).assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot.top
        assertEquals("Recreation must preserve the list scroll offset", previousTop, restoredTop, 1f)

        scrollNotesToTop()
        compose.onNodeWithTag("collection_시리즈").assertIsSelected()
        compose.onNodeWithText(seriesTitle).assertExists()
        compose.onNodeWithText("시리즈 목록").assertExists()
    }

    private fun waitForInitialFeed(): AxViewModel {
        val vm = compose.runOnIdle { ViewModelProvider(compose.activity)[AxViewModel::class.java] }
        assertSame("The displayed ViewModel must use this test's Application graph",
            (compose.activity.application as AxApplication).graph, vm.graph)
        compose.waitUntil(15000) {
            // Query semantics first so PAUSED Robolectric drains the main looper even
            // while the initial feed is still null or its refresh is still busy.
            compose.onAllNodesWithText(sampleTitle).fetchSemanticsNodes().isNotEmpty() &&
                !vm.busy.value && vm.feed.value != null
        }
        return vm
    }

    private fun seedCollections(extraArticles: List<Article> = emptyList()) {
        val vm = waitForInitialFeed()
        val articles = listOf(
            fixtureArticle("feedback-second", secondTitle, "2026-10-09").copy(
                seriesId = "series-a", seriesTitle = seriesTitle, seriesOrder = 2
            ),
            fixtureArticle("feedback-first", firstTitle, "2026-01-01").copy(
                seriesId = "series-a", seriesTitle = seriesTitle, seriesOrder = 1
            ),
            fixtureArticle("feedback-unrelated", unrelatedTitle, "2026-10-08"),
            fixtureArticle("feedback-other", "다른 모음의 글", "2026-10-07").copy(
                seriesId = "series-b", seriesTitle = "협업 실험", seriesOrder = 1
            ),
            fixtureArticle("feedback-project-second", projectSecondTitle, "2026-10-06").copy(
                projectId = "project-a", projectTitle = projectTitle, projectOrder = 2
            ),
            fixtureArticle("feedback-project-first", projectFirstTitle, "2026-01-02").copy(
                projectId = "project-a", projectTitle = projectTitle, projectOrder = 1
            ),
            fixtureArticle("feedback-other-project", "문서 도구 진행", "2026-10-05").copy(
                projectId = "project-b", projectTitle = "문서 도구", projectOrder = 1
            )
        ) + extraArticles
        compose.runOnIdle {
            // Seed the same graph whose identity and initial feed were verified above.
            val graph = vm.graph
            // Wait for the bundled feed first; seed transactionally off the UI dispatcher.
            runBlocking(Dispatchers.IO) {
                graph.contentDb.withTransaction {
                    val dao = graph.contentDb.dao()
                    val initialFeed = checkNotNull(dao.feed()) {
                        "The initial feed must be persisted in the displayed ViewModel's database"
                    }
                    assertEquals(vm.feed.value, initialFeed)
                    dao.clearArticles()
                    dao.put(articles)
                    dao.setFeed(initialFeed.copy(sample = false, authorName = authorName))
                }
            }
        }
        val expectedIds = articles.map { it.id }.toSet()
        compose.waitUntil(10000) {
            // Room invalidation dispatches back onto Robolectric's paused main looper.
            compose.runOnIdle { vm.articles.value.map { it.id }.toSet() == expectedIds }
        }
    }

    private fun fixtureArticle(id: String, title: String, date: String) = Article(
        id = id,
        title = title,
        description = "짧은 글 소개",
        publishedAt = "${date}T00:00:00Z",
        updatedAt = null,
        canonicalUrl = "https://ax.msalt.net/notes/$id/",
        detailUrl = "https://ax.msalt.net/app/v1/notes/$id.json",
        wantedRevision = "fixture-v1",
        projectUrl = "https://ax.msalt.net/projects/legacy-link/",
        cachedRevision = "fixture-v1",
        bodyHtml = "<p>기기에 저장된 본문입니다.</p>",
        bodyText = "기기에 저장된 본문입니다.",
        cachedAt = 1L
    )

    private fun openArticle(title: String) {
        compose.onNodeWithTag("notes_list").performScrollToNode(hasText(title))
        compose.onNodeWithText(title).performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("본문 저장됨").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertArticleOrder(first: String, second: String) {
        compose.onNodeWithTag("notes_list").performScrollToNode(hasText(first))
        val firstBounds = compose.onNodeWithText(first).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val secondBounds = compose.onNodeWithText(second).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("Expected '$first' before '$second'", firstBounds.top < secondBounds.top)
    }

    private fun pressSystemBack() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun scrollNotesToTop() {
        compose.onNodeWithTag("notes_list").performScrollToIndex(0)
    }

    private fun assertNoRefreshButtonOrInternalWording() {
        compose.onNodeWithText("새로고침").assertDoesNotExist()
        compose.onNodeWithContentDescription("새로고침").assertDoesNotExist()
        listOf("샘플", "내부 테스트", "검증용").forEach { wording ->
            compose.onAllNodesWithText(wording, substring = true).assertCountEquals(0)
        }
    }

    private fun showRefreshFixture(isRefreshing: () -> Boolean, onRefresh: () -> Unit) {
        waitForInitialFeed()
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AxTheme {
                    NotesRefreshBox(isRefreshing = isRefreshing(), onRefresh = onRefresh) {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(100) { Text("Refresh fixture item $it") }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("pull_refresh").assertExists()
    }

    private fun invokeRefreshAction(): Boolean {
        val action = compose.onNodeWithTag("pull_refresh").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].single { it.label == "새로고침" }
        return compose.runOnIdle { action.action() }
    }

    private fun resetViewModelFactoryApplication() {
        ViewModelProvider.AndroidViewModelFactory::class.java.getDeclaredField("_instance")
            .apply { isAccessible = true }.set(null, null)
    }
}
