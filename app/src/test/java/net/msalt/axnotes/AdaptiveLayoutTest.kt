package net.msalt.axnotes

import net.msalt.axnotes.ui.adaptiveLayout
import org.junit.Assert.*
import org.junit.Test

class AdaptiveLayoutTest {
    @Test fun compactWidthKeepsPhoneNavigation() {
        val layout = adaptiveLayout(599f, 900f, 1f)
        assertFalse(layout.useRail)
        assertFalse(layout.useTwoPanes)
    }
    @Test fun mediumWidthUsesRailAndSinglePane() {
        assertTrue(adaptiveLayout(600f, 900f, 1f).useRail)
        assertFalse(adaptiveLayout(839f, 900f, 1f).useTwoPanes)
    }
    @Test fun expandedWidthHasUsableListAndReadingPanes() {
        val layout = adaptiveLayout(840f, 900f, 1f)
        assertTrue(layout.useTwoPanes)
        assertEquals(320f, layout.listWidthDp, 0f)
        assertTrue(840f - 96f - layout.listWidthDp - 2f >= 400f)
        assertEquals(400f, adaptiveLayout(1600f, 1000f, 1f).listWidthDp, 0f)
    }
    @Test fun landscapePhoneDoesNotUseCrampedDualPane() {
        assertFalse(adaptiveLayout(900f, 479f, 1f).useTwoPanes)
        assertTrue(adaptiveLayout(900f, 480f, 1f).useTwoPanes)
    }
    @Test fun enlargedFontsGetMoreSpaceBeforeSplitting() {
        assertFalse(adaptiveLayout(999f, 900f, 1.5f).useTwoPanes)
        val layout = adaptiveLayout(1000f, 900f, 2f)
        assertTrue(layout.useTwoPanes)
        assertEquals(380f, layout.listWidthDp, 0f)
        assertTrue(1000f - 112f - layout.listWidthDp - 2f >= 500f)
    }
}
