package net.msalt.axnotes.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.PixelMap
import kotlin.math.roundToInt
import org.junit.Assert.assertThrows
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import org.junit.After
import org.junit.Assert.assertEquals
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
@Config(application = Application::class, sdk = [28], qualifiers = "w360dp-h800dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompactNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var host: ActivityController<ComponentActivity>
    private val selected = mutableStateOf("notes")
    private val clicks = mutableListOf<String>()
    private var surface = Color.Unspecified
    private var primary = Color.Unspecified
    private var inactive = Color.Unspecified

    @Before fun createHost() {
        host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    }

    @After fun closeHost() {
        host.pause().stop().destroy()
    }

    @Test fun destinationsKeepLabelsSelectionAndFullTouchTargets() {
        showNavigation()
        listOf("notes" to "Notes", "library" to "내 보관함", "search" to "검색", "settings" to "설정").forEach { (route, label) ->
            compose.onNodeWithTag("nav_$route").assertContentDescriptionEquals(label)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
                .assertHasClickAction().assertWidthIsAtLeast(AxSize.minTouch)
                .assertHeightIsAtLeast(AxSize.minTouch)
                // The destination edge remains tappable outside the visual feedback circle.
                .performTouchInput { click(Offset(2f, centerY)) }
            compose.onNodeWithTag("nav_$route").assertIsSelected()
            listOf("notes", "library", "search", "settings").filter { it != route }.forEach {
                compose.onNodeWithTag("nav_$it").assertIsNotSelected()
            }
        }
        compose.onNodeWithTag("nav_settings").performClick().performClick()
        compose.runOnIdle { assertEquals(listOf("notes", "library", "search", "settings", "settings", "settings"), clicks) }
    }

    @Test fun cancelledPressDoesNotNavigate() {
        showNavigation()
        compose.onNodeWithTag("nav_search").performTouchInput { down(center); cancel() }
        compose.onNodeWithTag("nav_notes").assertIsSelected()
        compose.runOnIdle { assertTrue(clicks.isEmpty()) }
    }

    @Test fun lightSelectionChangesOnlyIconPixels() = verifyIconOnlySelection(dark = false)

    @Test fun darkSelectionChangesOnlyIconPixels() = verifyIconOnlySelection(dark = true)

    private fun verifyIconOnlySelection(dark: Boolean) {
        showNavigation(dark)
        val node = compose.onNodeWithTag("nav_search")
        val before = captureNode(node).asImageBitmap().toPixelMap()
        // Change external state without a press, so this checks the persistent selection style.
        compose.runOnIdle { selected.value = "search" }
        val after = captureNode(node).asImageBitmap().toPixelMap()
        assertIconOnlyPixels(before, after)
    }

    private fun assertIconOnlyPixels(before: PixelMap, after: PixelMap) {
        val halfIcon = 12 // AxSize.icon is 24dp; this host is mdpi.
        var changed = 0
        var primaryPixels = 0
        var inactivePixels = 0
        for (y in 0 until after.height) for (x in 0 until after.width) {
            val inIcon = x >= after.width / 2 - halfIcon && x < after.width / 2 + halfIcon &&
                y >= after.height / 2 - halfIcon && y < after.height / 2 + halfIcon
            if (!inIcon) {
                assertEquals("No selected tile at ($x, $y)", surface, after[x, y])
                assertEquals(before[x, y], after[x, y])
            }
            if (before[x, y] != after[x, y]) changed++
            if (after[x, y] == primary) primaryPixels++
            if (before[x, y] == inactive) inactivePixels++
        }
        assertTrue("Selection must visibly change the icon", changed > 0)
        assertTrue("Selected icon uses theme primary", primaryPixels > 0)
        assertTrue("Unselected icon uses theme supporting foreground", inactivePixels > 0)
    }

    // Isolated experiment: draw the real native-rendered window after Compose idle,
    // avoiding Compose 1.8's asynchronous redraw callback under a paused looper.
    private fun captureNode(node: SemanticsNodeInteraction): Bitmap {
        val bounds = node.fetchSemanticsNode().boundsInWindow
        return compose.runOnIdle {
            val view = host.get().window.decorView
            val location = IntArray(2)
            view.getLocationInWindow(location)
            val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(full))
            Bitmap.createBitmap(full, bounds.left.roundToInt() - location[0],
                bounds.top.roundToInt() - location[1], bounds.width.roundToInt(), bounds.height.roundToInt())
        }
    }

    @Test fun forbiddenBackgroundPixelIsRejected() {
        showNavigation()
        val node = compose.onNodeWithTag("nav_search")
        val before = captureNode(node).asImageBitmap().toPixelMap()
        compose.runOnIdle { selected.value = "search" }
        val after = captureNode(node)
        assertIconOnlyPixels(before, after.asImageBitmap().toPixelMap())
        // Negative control outside the icon: a forbidden tile/dot pixel must fail.
        after.setPixel(0, 0, android.graphics.Color.MAGENTA)
        assertThrows(AssertionError::class.java) {
            assertIconOnlyPixels(before, after.asImageBitmap().toPixelMap())
        }
    }

    private fun showNavigation(dark: Boolean = false) {
        host.get().setContent {
            AxTheme(darkTheme = dark) {
                surface = MaterialTheme.colorScheme.surface
                primary = MaterialTheme.colorScheme.primary
                inactive = MaterialTheme.colorScheme.onSurfaceVariant
                CompactNavigation(selected.value) { route ->
                    clicks += route
                    selected.value = route
                }
            }
        }
        compose.waitForIdle()
    }
}
