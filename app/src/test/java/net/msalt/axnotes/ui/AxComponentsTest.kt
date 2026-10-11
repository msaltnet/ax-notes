package net.msalt.axnotes.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/** Real shared components in an isolated host: no feed, database or MainActivity work. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w360dp-h800dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AxComponentsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var host: ActivityController<ComponentActivity>

    @Before fun createHost() {
        host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    }

    @After fun closeHost() {
        host.pause().stop().destroy()
    }

    @Test
    fun buttonsExposeButtonRolesAndMinimumTouchTargetsAndInvokeActions() {
        val clicks = IntArray(4)
        showComponents {
            AxButton({ clicks[0]++ }, Modifier.testTag("primary")) { Text("저장") }
            AxOutlinedButton({ clicks[1]++ }, Modifier.testTag("outlined")) { Text("날짜 선택") }
            AxTextButton({ clicks[2]++ }, Modifier.testTag("text")) { Text("계속 작성") }
            AxTextButton({ clicks[3]++ }, Modifier.testTag("destructive"), destructive = true) { Text("삭제") }
        }
        listOf("primary", "outlined", "text", "destructive").forEach { tag ->
            compose.onNodeWithTag(tag).assertIsEnabled().assertHasClickAction()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .assertHeightIsAtLeast(AxSize.minTouch).assertWidthIsAtLeast(AxSize.minTouch)
                .performClick()
        }
        compose.runOnIdle { assertEquals(listOf(1, 1, 1, 1), clicks.toList()) }
    }

    @Test
    fun disabledButtonsDoNotInvokeActionsOnTouch() {
        var clicks = 0
        showComponents {
            AxButton({ clicks++ }, Modifier.testTag("primary"), enabled = false) { Text("저장") }
            AxOutlinedButton({ clicks++ }, Modifier.testTag("outlined"), enabled = false) { Text("날짜 선택") }
            AxTextButton({ clicks++ }, Modifier.testTag("text"), enabled = false) { Text("계속 작성") }
        }
        listOf("primary", "outlined", "text").forEach { tag ->
            compose.onNodeWithTag(tag).assertIsNotEnabled().performTouchInput { click() }
        }
        compose.runOnIdle { assertEquals(0, clicks) }
    }

    @Test
    fun loadingAnnouncesProgressAndRejectsRepeatedPressesUntilReady() {
        val loading = mutableStateOf(false)
        var submissions = 0
        showComponents {
            AxButton(
                onClick = { submissions++; loading.value = true },
                modifier = Modifier.testTag("save"), loading = loading.value
            ) { Text("메모 저장") }
        }
        compose.onNodeWithTag("save").assertIsEnabled().performClick()
        compose.onNodeWithTag("save").assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "처리 중"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assertTextContains("메모 저장")
        repeat(3) { compose.onNodeWithTag("save").performTouchInput { click() } }
        compose.runOnIdle {
            assertEquals("An in-flight submission cannot be duplicated", 1, submissions)
            loading.value = false
        }
        compose.onNodeWithTag("save").assertIsEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription)).performClick()
        compose.runOnIdle { assertEquals(2, submissions) }
    }

    @Test
    fun textFieldKeepsItsLabelAndUpdatesEditableValue() {
        val value = mutableStateOf("")
        showComponents {
            AxTextField(value.value, { value.value = it }, Modifier.testTag("memo"), label = { Text("메모 제목") })
        }
        compose.onNodeWithTag("memo").assertIsEnabled().assertHeightIsAtLeast(AxSize.fieldMinHeight)
            .assertTextContains("메모 제목").performTextInput("기억할 생각")
        compose.onNodeWithTag("memo").assertTextContains("기억할 생각").assertTextContains("메모 제목")
        compose.runOnIdle { assertEquals("기억할 생각", value.value) }
    }

    @Test
    fun errorInputExposesLabelErrorSemanticsAndVisibleRecoveryText() {
        showComponents {
            AxTextField(
                "", {}, Modifier.testTag("error"), label = { Text("메모 내용") }, isError = true,
                supportingText = { Text("기억할 내용을 입력해주세요") }
            )
        }
        compose.onNodeWithTag("error").assertTextContains("메모 내용")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose.onNodeWithText("기억할 내용을 입력해주세요", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("입력을 확인하세요", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun readOnlyAndDisabledInputsCannotOfferAnEditingAction() {
        var changes = 0
        showComponents {
            AxTextField("저장된 글의 제목", { changes++ }, Modifier.testTag("readonly"), readOnly = true, label = { Text("연결된 글") })
            AxTextField("저장 중인 메모", { changes++ }, Modifier.testTag("disabled"), enabled = false, label = { Text("메모") })
        }
        compose.onNodeWithTag("readonly").assertIsEnabled().assertTextContains("저장된 글의 제목")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText)).performTouchInput { click() }
        compose.onNodeWithTag("disabled").assertIsNotEnabled().assertTextContains("저장 중인 메모")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText)).performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, changes) }
    }

    @Test
    fun editorialSectionTabsKeepSelectionAndFullTouchTargets() {
        val selected = mutableStateOf(false)
        var calls = 0
        showComponents {
            AxSectionTab(selected.value, { selected.value = !selected.value; calls++ }, "프로젝트", Modifier.testTag("section_tab"))
        }
        compose.onNodeWithTag("section_tab").assertIsNotSelected().assertHasClickAction()
            .assertHeightIsAtLeast(AxSize.minTouch).assertWidthIsAtLeast(AxSize.minTouch)
            .performClick().assertIsSelected().performClick().assertIsNotSelected()
        compose.runOnIdle { assertEquals(2, calls) }
    }

    @Test
    fun cardsAndChipsExposeAndUpdateSelectionWithoutMakingFramesClickable() {
        val cardSelected = mutableStateOf(false)
        val chipSelected = mutableStateOf(false)
        var disabledClicks = 0
        showComponents {
            AxCard({ cardSelected.value = !cardSelected.value }, Modifier.testTag("card"), selected = cardSelected.value) {
                Text("읽을 글", Modifier.padding(AxComponentTokens.cardPadding))
            }
            AxCardFrame(Modifier.testTag("frame"), selected = true) {
                Text("별도 동작이 있는 카드", Modifier.padding(AxComponentTokens.cardPadding))
            }
            AxFilterChip(chipSelected.value, { chipSelected.value = !chipSelected.value }, { Text("시리즈") }, Modifier.testTag("chip"))
            AxFilterChip(false, { disabledClicks++ }, { Text("사용할 수 없음") }, Modifier.testTag("disabled_chip"), enabled = false)
        }
        compose.onNodeWithTag("card").assertIsNotSelected().assertHasClickAction()
            .assertHeightIsAtLeast(AxSize.minTouch).performClick().assertIsSelected()
        compose.onNodeWithTag("card").performClick().assertIsNotSelected()
        compose.onNodeWithTag("frame").assertIsSelected().assertHasNoClickAction()
        compose.onNodeWithTag("chip").assertIsNotSelected().assertHeightIsAtLeast(AxSize.minTouch)
            .assertWidthIsAtLeast(AxSize.minTouch).performClick().assertIsSelected()
        compose.onNodeWithTag("chip").performClick().assertIsNotSelected()
        compose.onNodeWithTag("disabled_chip").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, disabledClicks) }
    }

    @Test
    fun savedReminderCardKeepsItsEntireDateAndDeliveryStatusAtTwoHundredPercentText() {
        // A long localized date followed by the real delivered-but-unopened status.
        // This intentionally needs more than three lines in a 360dp window at 200%.
        val subtitle = "2026년 10월 10일 토요일 오후 11시 45분 00초 협정 세계시 (Coordinated Universal Time) · 알림 전송 · 아직 열지 않음"
        showComponents(fontScale = 2f) {
            SavedCard("다시 읽을 글", subtitle, onClick = {}) {
                AxTextButton({}) { Text("알림 취소") }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(subtitle, useUnmergedTree = true).assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> assertTrue(action(layouts)) }
        assertEquals(1, layouts.size)
        val layout = layouts.single()
        assertTrue("The fixture must cover the former three-line truncation (actual: ${layout.lineCount})", layout.lineCount > 3)
        assertFalse("A saved reminder must never ellipsize its delivery status", layout.hasVisualOverflow)
        assertEquals("The complete delivery status remains visible", subtitle.length,
            layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
        compose.onNodeWithText("알림 취소").assertIsDisplayed().assertHasClickAction()
    }

    @Test fun lightComponentsWrapLongKoreanAtTwoHundredPercentText() = assertLargeKoreanComponents(darkTheme = false)

    @Test fun darkComponentsWrapLongKoreanAtTwoHundredPercentText() = assertLargeKoreanComponents(darkTheme = true)

    @Test
    fun executableCatalogUsesTheSharedComponentStates() {
        listOf(false, true).forEach { darkTheme ->
            compose.runOnUiThread { host.get().setContent { AxDesignSystemCatalog(darkTheme = darkTheme) } }
            compose.onNodeWithTag("design_catalog").assertExists().performScrollToIndex(0)
            compose.onNodeWithTag("catalog_primary").assertIsEnabled().assertHeightIsAtLeast(AxSize.minTouch)
            compose.onNodeWithTag("design_catalog").performScrollToNode(hasTestTag("catalog_card_selected"))
            compose.onNodeWithTag("catalog_card_default").assertIsNotSelected()
            compose.onNodeWithTag("catalog_card_selected").assertIsSelected().assertHasClickAction()
            compose.onNodeWithTag("design_catalog").performScrollToNode(hasText("메모 제목"))
            compose.onNodeWithText("메모 제목").assertExists()
            compose.onNodeWithTag("design_catalog").performScrollToNode(hasText("남긴 메모가 없어요"))
            compose.onNodeWithText("남긴 메모가 없어요").assertIsDisplayed()
            compose.onNodeWithText("글을 열고 기억할 생각을 남겨보세요").performScrollTo().assertIsDisplayed()
        }
    }

    private fun assertLargeKoreanComponents(darkTheme: Boolean) {
        val primary = "기억하고 싶은 생각을 메모로 안전하게 저장하기"
        val loading = "작성한 내용을 저장하고 있으니 잠시만 기다려주세요"
        val disabled = "내용을 모두 입력하면 메모를 저장할 수 있어요"
        val outlined = "나중에 다시 읽을 날짜와 시간을 직접 선택하기"
        val textButton = "작성하던 소중한 메모를 이어서 계속 작성하기"
        val chip = "일하는 방식을 바꾸어 나가는 프로젝트의 긴 이름"
        val cardTitle = "인공지능과 함께 일하면서 새롭게 배운 내용을 모아보는 글"
        val fieldValue = "큰 글자에서도 작성한 내용을 빠짐없이 확인하고 기억할 수 있는 메모입니다"
        val helper = "기억하고 싶은 내용이 모두 들어 있는지 다시 한번 확인해주세요"
        val notices = mapOf(
            AxNoticeTone.Info to "저장한 본문은 인터넷에 연결되어 있지 않아도 편하게 다시 읽을 수 있어요",
            AxNoticeTone.Warning to "본문의 이미지와 외부 링크를 열 때에는 인터넷 연결이 필요할 수 있어요",
            AxNoticeTone.Error to "연결이 돌아오면 새로운 글을 받아보세요 저장된 글과 메모는 그대로 있어요"
        )
        val emptyTitle = "아직 이 글에 남겨 둔 메모가 없어요"
        val emptySubtitle = "글을 읽으며 오래 기억하고 싶은 생각과 직접 적용해볼 아이디어를 남겨보세요"
        showComponents(darkTheme = darkTheme, fontScale = 2f) {
            AxButton({}, Modifier.fillMaxWidth().testTag("large_primary")) { Text(primary) }
            AxButton({}, Modifier.fillMaxWidth().testTag("large_loading"), loading = true) { Text(loading) }
            AxButton({}, Modifier.fillMaxWidth().testTag("large_disabled"), enabled = false) { Text(disabled) }
            AxOutlinedButton({}, Modifier.fillMaxWidth().testTag("large_outlined")) { Text(outlined) }
            AxTextButton({}, Modifier.fillMaxWidth().testTag("large_text")) { Text(textButton) }
            AxFilterChip(true, {}, { Text(chip) }, Modifier.testTag("large_chip"))
            AxCard({}, Modifier.testTag("large_card"), selected = true) {
                Text(cardTitle, Modifier.padding(AxComponentTokens.cardPadding), style = MaterialTheme.typography.titleMedium)
            }
            AxTextField(
                fieldValue, {}, Modifier.fillMaxWidth().testTag("large_input"), label = { Text("기억할 생각") },
                isError = true, supportingText = { Text(helper) }
            )
            AxTextField(fieldValue, {}, Modifier.fillMaxWidth().testTag("large_readonly"), readOnly = true, label = { Text("연결된 글") })
            AxTextField(fieldValue, {}, Modifier.fillMaxWidth().testTag("large_disabled_input"), enabled = false, label = { Text("저장 중인 메모") })
            notices.forEach { (tone, message) -> AxNotice(message, tone = tone) }
            AxEmptyState(emptyTitle, emptySubtitle)
        }
        listOf("large_primary", "large_loading", "large_disabled", "large_outlined", "large_text", "large_chip", "large_card").forEach { tag ->
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(AxSize.minTouch)
        }
        (listOf(primary, loading, disabled, outlined, textButton, chip, cardTitle, helper) + notices.values + listOf(emptyTitle, emptySubtitle)).forEach { text ->
            val node = compose.onNodeWithText(text, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> assertTrue(action(layouts)) }
            assertTrue("A text layout must exist for $text", layouts.isNotEmpty())
            layouts.forEach { layout ->
                assertFalse("200% Korean text must not clip or ellipsize: $text (size=${layout.size}, lines=${layout.lineCount}, widthOverflow=${layout.didOverflowWidth}, heightOverflow=${layout.didOverflowHeight})", layout.hasVisualOverflow)
                assertTrue("Long Korean text must wrap rather than shrink: $text", layout.lineCount > 1)
            }
        }
        listOf("large_input", "large_readonly", "large_disabled_input").forEach { tag ->
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
                .assertTextContains(fieldValue).assertHeightIsAtLeast(AxSize.fieldMinHeight)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> assertTrue(action(layouts)) }
            assertTrue("The input's full value must have a text layout", layouts.isNotEmpty())
            layouts.forEach { layout ->
                assertFalse("200% input text must not clip: $tag", layout.hasVisualOverflow)
                assertTrue("Long input values must wrap: $tag", layout.lineCount > 1)
            }
        }
    }

    private fun showComponents(
        darkTheme: Boolean = false,
        fontScale: Float = 1f,
        content: @Composable ColumnScope.() -> Unit
    ) {
        compose.runOnUiThread {
            host.get().setContent {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                    AxTheme(darkTheme) {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            Column(
                                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AxSpacing.lg),
                                verticalArrangement = Arrangement.spacedBy(AxSpacing.md), content = content
                            )
                        }
                    }
                }
            }
        }
    }
}
