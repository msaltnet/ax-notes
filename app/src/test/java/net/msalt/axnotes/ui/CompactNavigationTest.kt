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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.luminance
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
    private var active = Color.Unspecified
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

    @Test fun lightSelectionAddsUnderlineWithoutTile() = verifyIconAndUnderlineSelection(dark = false)

    @Test fun darkSelectionAddsUnderlineWithoutTile() = verifyIconAndUnderlineSelection(dark = true)

    private fun verifyIconAndUnderlineSelection(dark: Boolean) {
        showNavigation(dark)
        listOf("notes", "library", "search", "settings").forEach { route ->
            val other = if (route == "notes") "library" else "notes"
            compose.runOnIdle { selected.value = other }
            val node = compose.onNodeWithTag("nav_$route")
            val before = captureNode(node).asImageBitmap().toPixelMap()
            // External state avoids transient press feedback in persistent-style assertions.
            compose.runOnIdle { selected.value = route }
            val after = captureNode(node).asImageBitmap().toPixelMap()
            assertIconAndUnderlinePixels(before, after)
            // Save actual native-rendered pixels as local visual-review evidence.
            val screenshot = captureNode(compose.onNodeWithTag("bottom_navigation"))
            val file = java.io.File("build/navigation-screenshots/${if (dark) "dark" else "light"}-$route.png")
            file.parentFile?.mkdirs()
            file.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
            compose.runOnIdle { selected.value = other }
            val restored = captureNode(node).asImageBitmap().toPixelMap()
            for (y in 0 until before.height) for (x in 0 until before.width) {
                assertEquals("Deselection restores inactive pixels", before[x, y], restored[x, y])
            }
        }
    }

    private fun assertIconAndUnderlinePixels(before: PixelMap, after: PixelMap) {
        assertTrue("Selected and unselected icons need 3:1 luminance contrast", contrast(active, inactive) >= 3f)
        assertTrue("Selected icon must contrast with surface", contrast(active, surface) >= 3f)
        assertTrue("Unselected icon must remain readable", contrast(inactive, surface) >= 3f)
        val halfIcon = 12 // AxSize.icon is 24dp; this host is mdpi.
        var changed = 0
        var activePixels = 0
        var inactivePixels = 0
        var underlinePixels = 0
        for (y in 0 until after.height) for (x in 0 until after.width) {
            val inIcon = x >= after.width / 2 - halfIcon && x < after.width / 2 + halfIcon &&
                y >= after.height / 2 - halfIcon && y < after.height / 2 + halfIcon
            val inUnderline = x >= after.width / 2 - 9 && x < after.width / 2 + 9 &&
                y >= after.height - 8 && y < after.height - 5
            if (!inIcon && !inUnderline) {
                assertEquals("No selected tile at ($x, $y)", surface, after[x, y])
                assertEquals(before[x, y], after[x, y])
            }
            if (before[x, y] != after[x, y]) changed++
            if (after[x, y] == active) activePixels++
            if (inUnderline && after[x, y] == active) underlinePixels++
            if (before[x, y] == inactive) inactivePixels++
        }
        assertTrue("Selection must visibly change the icon", changed > 0)
        assertTrue("Selected icon uses navigation selected token", activePixels > underlinePixels)
        assertTrue("Selected destination has a visible non-color underline", underlinePixels >= 30)
        assertTrue("Unselected icon uses navigation unselected token", inactivePixels > 0)
    }

    // Draw the real native-rendered window after Compose idle,
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
        assertIconAndUnderlinePixels(before, after.asImageBitmap().toPixelMap())
        // Negative control outside the icon: a forbidden tile/dot pixel must fail.
        after.setPixel(0, 0, android.graphics.Color.MAGENTA)
        assertThrows(AssertionError::class.java) {
            assertIconAndUnderlinePixels(before, after.asImageBitmap().toPixelMap())
        }
    }

    @Test fun initialRestoredSelectionStartsAtDestinationWithoutIntroAnimation() {
        selected.value = "settings"
        showNavigation()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("nav_settings").assertIsSelected()
        val pixels = captureNode(compose.onNodeWithTag("nav_settings")).asImageBitmap().toPixelMap()
        assertEquals(active, pixels[pixels.width / 2, pixels.height - 7])
        compose.mainClock.autoAdvance = true
    }

    @Test fun interruptedTransitionsSettleOnNewestDestination() {
        showNavigation()
        compose.mainClock.autoAdvance = false
        listOf("settings", "library", "search", "notes", "settings").forEach { route ->
            compose.runOnIdle { selected.value = route }
            compose.mainClock.advanceTimeBy(48)
        }
        compose.mainClock.advanceTimeBy(2000)
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("nav_settings").assertIsSelected()
        listOf("notes", "library", "search", "settings").forEach { route ->
            val pixels = captureNode(compose.onNodeWithTag("nav_$route")).asImageBitmap().toPixelMap()
            assertEquals("Only final destination has underline", if (route == "settings") active else surface,
                pixels[pixels.width / 2, pixels.height - 7])
        }
    }

    @Test fun rtlLargeFontKeepsUnderlineOnSelectedTabAndTouchSize() {
        showNavigation(dark = true, rtl = true, fontScale = 2f)
        listOf("settings", "search", "library", "notes").forEach { route ->
            compose.runOnIdle { selected.value = route }
            val node = compose.onNodeWithTag("nav_$route")
            node.assertIsSelected().assertHeightIsAtLeast(AxSize.minTouch)
                .assertWidthIsAtLeast(AxSize.minTouch)
            val pixels = captureNode(node).asImageBitmap().toPixelMap()
            assertEquals(active, pixels[pixels.width / 2, pixels.height - 7])
        }
    }

    private fun contrast(first: Color, second: Color): Float {
        val a = first.luminance()
        val b = second.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }

    private fun showNavigation(dark: Boolean = false, rtl: Boolean = false, fontScale: Float = 1f) {
        host.get().setContent {
            CompositionLocalProvider(LocalLayoutDirection provides (if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr),
                LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
            AxTheme(darkTheme = dark) {
                surface = MaterialTheme.colorScheme.surface
                active = LocalAxNavigationColors.current.selectedIcon
                inactive = LocalAxNavigationColors.current.unselectedIcon
                CompactNavigation(selected.value) { route ->
                    clicks += route
                    selected.value = route
                }
            }
            }
        }
        compose.waitForIdle()
    }
}
