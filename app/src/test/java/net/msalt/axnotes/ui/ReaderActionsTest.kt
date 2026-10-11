package net.msalt.axnotes.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.After
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

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w800dp-h800dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderActionsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var host: ActivityController<ComponentActivity>
    private val clicks = mutableListOf<String>()
    private val availableWidth = mutableStateOf(300)
    @Before fun createHost() { host = Robolectric.buildActivity(ComponentActivity::class.java).setup() }
    @After fun closeHost() { host.pause().stop().destroy() }

    @Test fun noOverflowHasNoDecorationOrExtraButtons() {
        show(width = 780)
        compose.onNodeWithTag("reader_actions_next").assertDoesNotExist()
        compose.onNodeWithTag("reader_actions_previous").assertDoesNotExist()
        compose.onNodeWithTag("share").assertIsDisplayed()
    }

    @Test fun narrowReaderShowsAccessibleOverflowControlAndRevealsShare() {
        show(width = 300)
        assertOverflowControls()
    }

    @Test fun twoHundredPercentTextKeepsScrollingAndAllActionsReachable() {
        show(width = 360, fontScale = 2f)
        assertOverflowControls()
    }

    @Test fun rightToLeftLayoutUsesTheSameLogicalPagingControls() {
        show(width = 300, direction = LayoutDirection.Rtl)
        assertOverflowControls()
    }

    @Test fun touchSwipeUpdatesTheEdgeHintsWithoutPagingButtons() {
        show(width = 300)
        compose.onNodeWithTag("reader_actions").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        val range = compose.onNodeWithTag("reader_actions").fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange]
        assertTrue("A real swipe inside the row must change its scroll position", range.value() > 0f)
        var pages = 0
        while (compose.onAllNodesWithTag("reader_actions_next").fetchSemanticsNodes().isNotEmpty() && pages++ < 8) {
            compose.onNodeWithTag("reader_actions_next").performTouchInput { click() }
            compose.waitForIdle()
        }
        assertTrue("Swipe and paging must reach the end", pages < 8)
        compose.onNodeWithTag("share").assertIsDisplayed()
        compose.onNodeWithTag("reader_actions_previous").assertIsDisplayed()
    }

    @Test fun wideningTheWindowRemovesTheGutterAndNarrowingRestoresIt() {
        show(width = 300)
        compose.onNodeWithTag("reader_actions_next").assertIsDisplayed()
        compose.runOnIdle { availableWidth.value = 780 }
        compose.onNodeWithTag("reader_actions_next").assertDoesNotExist()
        compose.onNodeWithTag("reader_actions_previous").assertDoesNotExist()
        compose.onNodeWithTag("share").assertIsDisplayed()
        compose.runOnIdle { availableWidth.value = 300 }
        compose.onNodeWithTag("reader_actions_next").assertIsDisplayed()
    }

    private fun assertOverflowControls() {
        compose.onNodeWithTag("reader_actions_previous").assertDoesNotExist()
        compose.onNodeWithTag("reader_actions_next").assertIsDisplayed().assertHasClickAction()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertContentDescriptionEquals("뒤쪽 작업 더 보기")
        var pages = 0
        while (compose.onAllNodesWithTag("reader_actions_next").fetchSemanticsNodes().isNotEmpty() && pages++ < 8) {
            compose.onNodeWithTag("reader_actions_next").performTouchInput { click() }
            compose.waitForIdle()
        }
        assertTrue("Paging must reach the end", pages < 8)
        compose.onNodeWithTag("reader_actions_next").assertDoesNotExist()
        compose.onNodeWithTag("share").assertIsDisplayed().performTouchInput { click() }
        assertTrue("The actual Share target must receive the touch", clicks.contains("share"))
        compose.onNodeWithTag("reader_actions_previous").assertIsDisplayed()
            .assertContentDescriptionEquals("처음 작업 보기")
        compose.onNodeWithTag("reader_actions_previous").performTouchInput { click() }
        compose.waitForIdle()
        compose.onNodeWithTag("bookmark").assertIsDisplayed()
        compose.onNodeWithTag("reader_actions_previous").assertDoesNotExist()
    }

    private fun show(width: Int, fontScale: Float = 1f, direction: LayoutDirection = LayoutDirection.Ltr) {
        availableWidth.value = width
        host.get().setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale), LocalLayoutDirection provides direction) {
                AxTheme {
                    Box(Modifier.width(availableWidth.value.dp)) {
                        ReaderActions {
                            listOf("bookmark" to "북마크", "reminder" to "읽기 알림", "memo" to "메모 남기기", "share" to "공유").forEach { (tag, label) ->
                                AxTextButton(onClick = { clicks += tag }, modifier = Modifier.testTag(tag)) {
                                    Icon(Icons.Default.Share, null, Modifier.size(AxSize.supportingIcon))
                                    Spacer(Modifier.width(AxSpacing.xs))
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
