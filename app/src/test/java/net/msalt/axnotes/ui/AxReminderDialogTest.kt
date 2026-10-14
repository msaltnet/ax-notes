package net.msalt.axnotes.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.ViewModelProvider
import java.text.DateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/** Exercises the real date/time dialogs without repository or notification side effects. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "en-rUS-w411dp-h891dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AxReminderDialogTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var host: ActivityController<ComponentActivity>
    private lateinit var oldLocale: Locale
    private lateinit var oldZone: TimeZone
    private val dueAt = mutableStateOf(0L)
    private val visible = mutableStateOf(true)
    private val changes = mutableListOf<Long>()
    private var confirmations = 0
    private var dismissals = 0

    @Before fun resetHostState() {
        resetViewModelFactoryApplication()
        oldLocale = Locale.getDefault()
        oldZone = TimeZone.getDefault()
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
        RuntimeEnvironment.setFontScale(1f)
    }

    @After fun closeHost() {
        if (::host.isInitialized) host.pause().stop().destroy()
        RuntimeEnvironment.setFontScale(1f)
        Locale.setDefault(oldLocale)
        TimeZone.setDefault(oldZone)
        resetViewModelFactoryApplication()
    }

    @Test
    fun applyingDateThenTimeUpdatesBothValuesAndTheFinalSummary() {
        showDialog(fontScale = 2f)
        assertCurrentValues()
        compose.onNodeWithTag("reminder_date").performScrollTo().performClick()
        assertCalendarOnly()
        selectDay("5")
        compose.onNodeWithText("날짜 적용").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(localMillis(2099, 1, 5, 10, 15), dueAt.value) }
        assertCurrentValues()

        openTime()
        selectWheel("reminder_hour", 21)
        selectWheel("reminder_minute", 45)
        compose.onNodeWithText("시각 적용").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(localMillis(2099, 1, 5, 21, 45), dueAt.value)
            assertEquals(listOf(localMillis(2099, 1, 5, 10, 15), dueAt.value), changes)
        }
        assertCurrentValues()
        compose.onNodeWithTag("reminder_summary")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.onNodeWithTag("reminder_confirm").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, confirmations) }
    }

    @Test
    fun canceledDateEditsDoNotLeakIntoAReopenedPicker() {
        showDialog(fontScale = 2f)
        compose.onNodeWithTag("reminder_date").performScrollTo().performClick()
        assertCalendarOnly()
        selectDay("7")
        cancelPicker("날짜 선택")
        compose.runOnIdle {
            assertEquals(localMillis(2099, 1, 2, 10, 15), dueAt.value)
            assertTrue(changes.isEmpty())
        }
        assertCurrentValues()
        compose.onNodeWithTag("reminder_date").performScrollTo().performClick()
        calendarDay("2").assertIsSelected()
        compose.onNodeWithText("날짜 적용").performClick()
        compose.runOnIdle { assertEquals(localMillis(2099, 1, 2, 10, 15), dueAt.value) }
    }

    @Test
    fun canceledTimeEditsAreDiscardedAndReopeningUsesTheLastAppliedTime() {
        showDialog()
        openTime()
        selectWheel("reminder_hour", 23)
        selectWheel("reminder_minute", 9)
        cancelPicker("시각 선택")
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
        assertCurrentValues()
        openTime()
        assertWheelValue("reminder_hour", "10")
        assertWheelValue("reminder_minute", "15")
        selectWheel("reminder_hour", 12)
        selectWheel("reminder_minute", 34)
        compose.onNodeWithText("시각 적용").performClick()
        openTime()
        assertWheelValue("reminder_hour", "12")
        assertWheelValue("reminder_minute", "34")
        cancelPicker("시각 선택")

        compose.onNodeWithText("취소").performClick()
        compose.onNodeWithTag("reminder_confirm").assertDoesNotExist()
        compose.onNodeWithText("알림 다시 열기").performClick()
        assertCurrentValues()
        openTime()
        assertWheelValue("reminder_hour", "12")
        assertWheelValue("reminder_minute", "34")
        compose.runOnIdle {
            assertEquals(listOf(localMillis(2099, 1, 2, 12, 34)), changes)
            assertEquals(1, dismissals)
            assertEquals(0, confirmations)
        }
    }

    @Test
    fun scrollWheelsExposeOnlyValidEndpointsAndAccessibleAdjustments() {
        showDialog()
        openTime()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        selectWheel("reminder_hour", 23)
        selectWheel("reminder_minute", 59)
        assertWheelValue("reminder_hour", "23")
        assertWheelValue("reminder_minute", "59")
        compose.onNodeWithText("시각 적용").performClick()
        compose.runOnIdle { assertEquals(localMillis(2099, 1, 2, 23, 59), dueAt.value) }
        openTime()
        compose.onNodeWithTag("reminder_hour").performSemanticsAction(SemanticsActions.SetProgress) { it(-1f) }
        compose.onNodeWithTag("reminder_minute").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        assertWheelValue("reminder_hour", "00")
        assertWheelValue("reminder_minute", "00")
        compose.onNodeWithText("시각 적용").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(localMillis(2099, 1, 2, 0, 0), dueAt.value) }
        assertCurrentValues()
    }

    @Test
    fun bothWheelsRespondToRealSwipesAndStageTheirCenteredValuesUntilApply() {
        showDialog()
        openTime()
        listOf("reminder_hour", "reminder_minute").forEach { tag ->
            compose.onNodeWithTag(tag).assertIsDisplayed().performTouchInput {
                swipe(start = Offset(center.x, center.y + height / 4),
                    end = Offset(center.x, center.y - height / 4), durationMillis = 600)
            }
        }
        val hour = wheelValue("reminder_hour")
        val minute = wheelValue("reminder_minute")
        assertTrue("The hour wheel must move with a real swipe", hour > 10)
        assertTrue("The minute wheel must move independently", minute > 15)
        listOf("reminder_hour" to hour, "reminder_minute" to minute).forEach { (tag, value) ->
            val wheel = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            val selected = compose.onNodeWithTag("${tag}_$value").assertIsSelected().fetchSemanticsNode().boundsInRoot
            assertEquals("A swiped wheel must snap to the selected center row", wheel.center.y, selected.center.y, 1f)
        }
        compose.runOnIdle {
            assertTrue(changes.isEmpty())
            assertEquals(localMillis(2099, 1, 2, 10, 15), dueAt.value)
        }
        compose.onNodeWithText("시각 적용").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(localMillis(2099, 1, 2, hour, minute), dueAt.value) }
    }

    @Test
    fun pastConfirmationShowsAnErrorAndChangingTheSelectionAllowsRecovery() {
        showDialog(initialDueAt = System.currentTimeMillis() - 60_000)
        compose.onNodeWithTag("reminder_confirm").performClick()
        compose.onNodeWithText("미래 시각을 선택하세요").performScrollTo().assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.runOnIdle { assertEquals(0, confirmations) }
        compose.runOnIdle { dueAt.value = localMillis(2099, 1, 2, 10, 15) }
        compose.onNodeWithText("미래 시각을 선택하세요").assertDoesNotExist()
        assertCurrentValues()
        compose.onNodeWithTag("reminder_confirm").performClick()
        compose.runOnIdle { assertEquals(1, confirmations) }
    }

    @Test
    @Config(sdk = [26])
    fun androidOreoKeepsCalendarAndBothWheelsUsableWithoutKeyboardEntry() {
        showDialog(fontScale = 2f)
        openTime()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithTag("reminder_hour").assertIsDisplayed()
        compose.onNodeWithTag("reminder_minute").assertIsDisplayed()
        compose.onNodeWithText("시각 적용").assertIsDisplayed().assertIsEnabled()
        cancelPicker("시각 선택")
        compose.onNodeWithTag("reminder_date").performScrollTo().performClick()
        assertCalendarOnly()
        selectDay("5")
        compose.onNodeWithText("날짜 적용").assertIsDisplayed().assertIsEnabled()
        cancelPicker("날짜 선택")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-port-mdpi")
    fun sixWeekCalendarScrollsToLastDayAtTwoHundredPercentAndAppliesOnlyThatSelection() {
        val original = localMillis(2099, 5, 13, 10, 15)
        showDialog(fontScale = 2f, initialDueAt = original, darkTheme = true)
        compose.onNodeWithTag("reminder_date").performScrollTo().performClick()
        assertCalendarOnly()
        // May 2099 has six Sunday-first rows. Swipe the actual outer calendar;
        // a semantic click alone can select a day still clipped under the footer.
        compose.onNodeWithTag("reminder_calendar").performTouchInput { swipeUp(durationMillis = 500) }
        val lastDay = compose.onNode(hasText("May 31, 2099", substring = true) and hasClickAction())
        val calendar = compose.onNodeWithTag("reminder_calendar").fetchSemanticsNode().boundsInRoot
        val dayBounds = lastDay.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("The last row must be fully inside the scrolled viewport",
            dayBounds.top >= calendar.top && dayBounds.bottom <= calendar.bottom)
        lastDay.performTouchInput { click() }.assertIsSelected()
        compose.runOnIdle {
            assertEquals(original, dueAt.value)
            assertTrue(changes.isEmpty())
        }
        val apply = compose.onNodeWithTag("reminder_date_apply").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("reminder_date_cancel").assertIsDisplayed().assertIsEnabled()
        assertTrue("The selected last row must not overlap the fixed actions",
            dayBounds.bottom <= apply.fetchSemanticsNode().boundsInRoot.top)
        apply.performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(localMillis(2099, 5, 31, 10, 15), dueAt.value)
            assertEquals(listOf(dueAt.value), changes)
        }
        assertCurrentValues()
    }

    @Test
    fun twoHundredPercentDialogKeepsRealScaledValuesAndControlsReadable() = assertLargeTextDialog()

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h800dp-port-mdpi")
    fun narrowPhoneKeepsRealTwoHundredPercentReminderValuesAndControlsReadable() = assertLargeTextDialog()

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-port-mdpi")
    fun smallDarkPhoneKeepsTwoHundredPercentWheelsAndCalendarUsable() = assertLargeTextDialog(darkTheme = true)

    private fun assertLargeTextDialog(darkTheme: Boolean = false) {
        showDialog(fontScale = 2f, darkTheme = darkTheme)
        val date = DateFormat.getDateInstance(DateFormat.FULL).format(Date(dueAt.value))
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(dueAt.value))
        listOf(date to AxTypography.titleMedium.fontSize, time to AxTypography.titleMedium.fontSize,
            summary() to AxTypography.bodyMedium.fontSize).forEach { (text, size) -> assertScaledTextFits(text, size) }
        listOf("reminder_date", "reminder_time").forEach { tag ->
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
                .assertHasClickAction().assertHeightIsAtLeast(AxSize.minTouch)
        }
        compose.onNodeWithTag("reminder_confirm").assertIsDisplayed().assertHasClickAction()
        val timeTop = compose.onNodeWithTag("reminder_time").performScrollTo().fetchSemanticsNode().boundsInRoot.top
        openTime()
        listOf("reminder_hour" to "10", "reminder_minute" to "15").forEach { (tag, value) ->
            compose.onNodeWithTag(tag).assertIsDisplayed()
            assertWheelValue(tag, value)
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNode(hasText(value) and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)
                .assertIsDisplayed().performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(layouts)) }
            val layout = layouts.single()
            assertEquals(2f, layout.layoutInput.density.fontScale, 0f)
            assertEquals(AxTypography.bodyLarge.fontSize, layout.layoutInput.style.fontSize)
            assertGlyphsFit(layout, value)
        }
        val hourBounds = compose.onNodeWithTag("reminder_hour").fetchSemanticsNode().boundsInRoot
        val minuteBounds = compose.onNodeWithTag("reminder_minute").fetchSemanticsNode().boundsInRoot
        assertEquals("Both wheels must be visible side by side", hourBounds.top, minuteBounds.top, 1f)
        assertTrue(hourBounds.right <= minuteBounds.left)
        compose.onNodeWithText("시각 적용").assertIsDisplayed().assertIsEnabled()
        cancelPicker("시각 선택")
        val returnedTop = compose.onNodeWithTag("reminder_time").assertIsDisplayed().fetchSemanticsNode().boundsInRoot.top
        assertEquals("Closing the picker preserves the main dialog's scroll position", timeTop, returnedTop, 1f)
        assertCurrentValues()
        compose.onNodeWithTag("reminder_date").performScrollTo().performClick()
        assertCalendarOnly()
        selectDay("15")
        // Material 3 1.3.2 intentionally clears inner day/year Text semantics and
        // exposes the full localized date on its selectable Surface instead.
        // Native-view captures separately verify the calendar's rendered glyphs.
        calendarDay("15").assertIsSelected().assertIsDisplayed()
        compose.onNodeWithTag("reminder_date_apply").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("reminder_date_cancel").assertIsDisplayed()
        listOf("apply", "cancel").forEach { action ->
            val inset = compose.onNodeWithTag("reminder_date_${action}_inset").fetchSemanticsNode().boundsInRoot
            val button = compose.onNodeWithTag("reminder_date_$action").fetchSemanticsNode().boundsInRoot
            assertTrue("Each date action must retain bottom breathing room", inset.bottom - button.bottom >= 16f)
        }
        compose.onNodeWithText("날짜 적용").performClick()
        compose.runOnIdle { assertEquals(localMillis(2099, 1, 15, 10, 15), dueAt.value) }
    }

    private fun showDialog(fontScale: Float = 1f, initialDueAt: Long = localMillis(2099, 1, 2, 10, 15), darkTheme: Boolean = false) {
        dueAt.value = initialDueAt
        RuntimeEnvironment.setFontScale(fontScale)
        host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        compose.runOnUiThread {
            host.get().setContent {
                AxTheme(darkTheme = darkTheme) {
                    AxButton(onClick = { visible.value = true }) { Text("알림 다시 열기") }
                    if (visible.value) AxReminderDialog(
                        dueAt = dueAt.value,
                        onDueAtChange = { changes += it; dueAt.value = it },
                        onDismiss = { dismissals++; visible.value = false },
                        onConfirm = { confirmations++ }
                    )
                }
            }
        }
    }

    private fun openTime() = compose.onNodeWithTag("reminder_time").performScrollTo().performClick()

    private fun cancelPicker(title: String) {
        compose.onNode(hasText("취소") and hasAnyAncestor(isDialog() and hasAnyDescendant(hasText(title))))
            .assertIsDisplayed().performClick()
        compose.onNodeWithText(title).assertDoesNotExist()
    }

    private fun assertCalendarOnly() {
        compose.onNodeWithTag("reminder_calendar").assertExists()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithContentDescription("Switch to text input mode").assertDoesNotExist()
    }

    private fun calendarDay(day: String) = compose.onNode(hasText("January ${day.toInt()}, 2099", substring = true) and hasClickAction())

    private fun selectDay(day: String) = calendarDay(day).performScrollTo().performClick()

    private fun selectWheel(tag: String, value: Int) {
        compose.onNodeWithTag(tag).performScrollToIndex(value)
        assertWheelValue(tag, value.toString().padStart(2, '0'))
    }

    private fun wheelValue(tag: String): Int = compose.onNodeWithTag(tag).fetchSemanticsNode()
        .config[SemanticsProperties.StateDescription].filter(Char::isDigit).toInt()

    private fun assertWheelValue(tag: String, expected: String) {
        val unit = if (tag == "reminder_hour") "시" else "분"
        compose.onNodeWithTag(tag).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "$expected$unit"))
        compose.onNodeWithTag("${tag}_${expected.toInt()}").assertIsSelected()
    }

    private fun assertCurrentValues() {
        compose.onNodeWithTag("reminder_date").performScrollTo()
            .assertTextContains(DateFormat.getDateInstance(DateFormat.FULL).format(Date(dueAt.value)))
        compose.onNodeWithTag("reminder_time").performScrollTo()
            .assertTextContains(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(dueAt.value)))
        compose.onNodeWithTag("reminder_summary").performScrollTo().assertTextEquals(summary())
    }

    private fun assertScaledTextFits(text: String, expectedFontSize: androidx.compose.ui.unit.TextUnit) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(layouts)) }
        val layout = layouts.single()
        assertEquals("Use the actual system 200% setting", 2f, layout.layoutInput.density.fontScale, 0f)
        assertEquals("Do not shrink the typography role to accommodate 200% text", expectedFontSize, layout.layoutInput.style.fontSize)
        assertGlyphsFit(layout, text)
    }

    private fun assertGlyphsFit(layout: TextLayoutResult, text: String) {
        val lineDetails = (0 until layout.lineCount).joinToString { line ->
            "line=$line left=${layout.getLineLeft(line)} right=${layout.getLineRight(line)} end=${layout.getLineEnd(line, visibleEnd = true)}"
        }
        val diagnostic = "$text; size=${layout.size}; constraints=${layout.layoutInput.constraints}; $lineDetails"
        assertFalse("Scaled reminder text must not clip vertically: $diagnostic", layout.didOverflowHeight)
        // A wrap-content Text can retain a wider paragraph constraint than its
        // measured size. Test every rendered line's glyph bounds, rather than
        // didOverflowWidth's constraint comparison (the masthead has the same case).
        (0 until layout.lineCount).forEach { line ->
            assertTrue("All glyphs must fit the left edge: $diagnostic", layout.getLineLeft(line) >= -1f)
            assertTrue("All glyphs must fit the right edge: $diagnostic", layout.getLineRight(line) <= layout.size.width + 1f)
            assertFalse("No line may be ellipsized: $diagnostic", layout.isLineEllipsized(line))
        }
        assertEquals("The complete value must remain rendered: $diagnostic", text.length,
            layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
    }

    private fun summary() = "예약할 시각 · ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(dueAt.value))}"
    private fun localMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        LocalDateTime.of(year, month, day, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun resetViewModelFactoryApplication() {
        ViewModelProvider.AndroidViewModelFactory::class.java.getDeclaredField("_instance")
            .apply { isAccessible = true }.set(null, null)
    }
}
