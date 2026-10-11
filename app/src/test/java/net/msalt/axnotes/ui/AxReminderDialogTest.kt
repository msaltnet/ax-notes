package net.msalt.axnotes.ui

import android.app.Application
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
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
import org.robolectric.shadows.ShadowDialog

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
        // Large-text mode exposes the actual Material date input rather than a cramped grid.
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("01052099")
        compose.onNodeWithText("날짜 적용").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(localMillis(2099, 1, 5, 10, 15), dueAt.value) }
        assertCurrentValues()

        openTime()
        compose.onNodeWithTag("reminder_hour").performTextReplacement("21")
        compose.onNodeWithTag("reminder_minute").performTextReplacement("45")
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
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("01072099")
        cancelPicker("날짜 선택")
        compose.runOnIdle {
            assertEquals(localMillis(2099, 1, 2, 10, 15), dueAt.value)
            assertTrue(changes.isEmpty())
        }
        assertCurrentValues()
        compose.onNodeWithTag("reminder_date").performScrollTo().performClick()
        val input = compose.onNode(hasSetTextAction()).fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertEquals("A canceled date must not survive reopening", "01022099", input.filter(Char::isDigit))
        compose.onNodeWithText("날짜 적용").performClick()
        compose.runOnIdle { assertEquals(localMillis(2099, 1, 2, 10, 15), dueAt.value) }
    }

    @Test
    fun canceledTimeEditsAreDiscardedAndReopeningUsesTheLastAppliedTime() {
        showDialog()
        openTime()
        compose.onNodeWithTag("reminder_hour").performTextReplacement("23")
        compose.onNodeWithTag("reminder_minute").performTextReplacement("09")
        cancelPicker("시각 선택")
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
        assertCurrentValues()
        openTime()
        assertEditableValue("reminder_hour", "10")
        assertEditableValue("reminder_minute", "15")
        compose.onNodeWithTag("reminder_hour").performTextReplacement("12")
        compose.onNodeWithTag("reminder_minute").performTextReplacement("34")
        compose.onNodeWithText("시각 적용").performClick()
        openTime()
        assertEditableValue("reminder_hour", "12")
        assertEditableValue("reminder_minute", "34")
        cancelPicker("시각 선택")

        compose.onNodeWithText("취소").performClick()
        compose.onNodeWithTag("reminder_confirm").assertDoesNotExist()
        compose.onNodeWithText("알림 다시 열기").performClick()
        assertCurrentValues()
        openTime()
        assertEditableValue("reminder_hour", "12")
        assertEditableValue("reminder_minute", "34")
        compose.runOnIdle {
            assertEquals(listOf(localMillis(2099, 1, 2, 12, 34)), changes)
            assertEquals(1, dismissals)
            assertEquals(0, confirmations)
        }
    }

    @Test
    fun invalidHoursMinutesAndEmptyValuesDisableApplyWithoutChangingDueAt() {
        showDialog()
        openTime()
        listOf("24" to "15", "10" to "60", "" to "15", "10" to "").forEach { (hour, minute) ->
            compose.onNodeWithTag("reminder_hour").performTextReplacement(hour)
            compose.onNodeWithTag("reminder_minute").performTextReplacement(minute)
            compose.onNodeWithText("시각 적용").assertIsNotEnabled().performTouchInput { click() }
            compose.onNodeWithTag(if (hour.isEmpty() || hour == "24") "reminder_hour" else "reminder_minute")
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            compose.runOnIdle {
                assertTrue(changes.isEmpty())
                assertEquals(localMillis(2099, 1, 2, 10, 15), dueAt.value)
            }
        }
        compose.onNodeWithTag("reminder_hour").performTextReplacement("00")
        compose.onNodeWithTag("reminder_minute").performTextReplacement("00")
        compose.onNodeWithText("시각 적용").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(localMillis(2099, 1, 2, 0, 0), dueAt.value) }
        assertCurrentValues()
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
    fun pickerWindowsRequestImeResizeOnAndroidOreo() {
        showDialog(fontScale = 2f)
        openTime()
        assertImeResizeRequested()
        compose.onNodeWithTag("reminder_minute").performScrollTo().performClick()
        compose.onNodeWithText("시각 적용").assertIsDisplayed().assertIsEnabled()
        cancelPicker("시각 선택")
        compose.onNodeWithTag("reminder_date").performScrollTo().performClick()
        assertImeResizeRequested()
        val dateWindow = checkNotNull(ShadowDialog.getLatestDialog().window)
        assertEquals("Date host must fill the window so real IME insets can constrain its content",
            WindowManager.LayoutParams.MATCH_PARENT, dateWindow.attributes.height)
        compose.onNode(hasSetTextAction()).performScrollTo().performClick()
        compose.onNodeWithText("날짜 적용").assertIsDisplayed().assertIsEnabled()
        cancelPicker("날짜 선택")
    }

    private fun assertImeResizeRequested() {
        // Robolectric does not render a real keyboard. Verify the actual window
        // policy here; emulator coverage separately checks visible IME insets.
        // performClick dispatches the callback; await the resulting dialog
        // composition before asking Robolectric for the latest shown window.
        compose.waitForIdle()
        val dialog = checkNotNull(ShadowDialog.getLatestDialog())
        assertTrue("Inspect the active picker, not the dismissed summary window", dialog.isShowing)
        val window = checkNotNull(dialog.window)
        assertEquals(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
            window.attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
    }

    @Test
    fun twoHundredPercentDialogKeepsRealScaledValuesAndControlsReadable() = assertLargeTextDialog()

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h800dp-port-mdpi")
    fun narrowPhoneKeepsRealTwoHundredPercentReminderValuesAndControlsReadable() = assertLargeTextDialog()

    private fun assertLargeTextDialog() {
        showDialog(fontScale = 2f)
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
        listOf("reminder_hour", "reminder_minute").forEach { tag ->
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(layouts)) }
            assertTrue(layouts.isNotEmpty())
            layouts.forEach { layout ->
                assertEquals(2f, layout.layoutInput.density.fontScale, 0f)
                assertEquals(AxTypography.bodyLarge.fontSize, layout.layoutInput.style.fontSize)
                assertGlyphsFit(layout, layout.layoutInput.text.text)
            }
        }
        compose.onNodeWithText("시각 적용").assertIsDisplayed().assertIsEnabled()
        cancelPicker("시각 선택")
        val returnedTop = compose.onNodeWithTag("reminder_time").assertIsDisplayed().fetchSemanticsNode().boundsInRoot.top
        assertEquals("Closing the picker preserves the main dialog's scroll position", timeTop, returnedTop, 1f)
        assertCurrentValues()
    }

    private fun showDialog(fontScale: Float = 1f, initialDueAt: Long = localMillis(2099, 1, 2, 10, 15)) {
        dueAt.value = initialDueAt
        RuntimeEnvironment.setFontScale(fontScale)
        host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        compose.runOnUiThread {
            host.get().setContent {
                AxTheme {
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

    private fun assertEditableValue(tag: String, expected: String) {
        compose.onNodeWithTag(tag).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,
            androidx.compose.ui.text.AnnotatedString(expected)))
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
