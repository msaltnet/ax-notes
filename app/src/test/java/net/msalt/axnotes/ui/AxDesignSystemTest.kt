package net.msalt.axnotes.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Token contracts for the same schemes, typography and shapes installed by AxTheme. */
class AxDesignSystemTest {
    @Test
    fun lightSemanticTextColorsMeetNormalTextContrast() = assertTextContrast("light", AxLightColors)

    @Test
    fun darkSemanticTextColorsMeetNormalTextContrast() = assertTextContrast("dark", AxDarkColors)

    @Test
    fun everyTypographyRoleHasExplicitScalableSizeAndReadableLeading() {
        val roles = with(AxTypography) {
            mapOf(
                "displayLarge" to displayLarge, "displayMedium" to displayMedium, "displaySmall" to displaySmall,
                "headlineLarge" to headlineLarge, "headlineMedium" to headlineMedium, "headlineSmall" to headlineSmall,
                "titleLarge" to titleLarge, "titleMedium" to titleMedium, "titleSmall" to titleSmall,
                "bodyLarge" to bodyLarge, "bodyMedium" to bodyMedium, "bodySmall" to bodySmall,
                "labelLarge" to labelLarge, "labelMedium" to labelMedium, "labelSmall" to labelSmall
            )
        }
        assertEquals(15, roles.size)
        roles.forEach { (name, style) ->
            assertTrue("$name must scale with the user's font setting", style.fontSize.isSp)
            assertTrue("$name line height must also scale", style.lineHeight.isSp)
            assertTrue("$name must not be smaller than 11sp", style.fontSize.value >= 11f)
            assertTrue("$name requires at least 1.2x leading", style.lineHeight.value >= style.fontSize.value * 1.2f)
            assertEquals("$name uses the system family with Korean fallback", FontFamily.SansSerif, style.fontFamily)
        }
        assertEquals(16.sp, AxTypography.bodyLarge.fontSize)
        assertEquals(24.sp, AxTypography.bodyLarge.lineHeight)
        assertEquals(14.sp, AxTypography.labelLarge.fontSize)
        assertEquals(20.sp, AxTypography.labelLarge.lineHeight)
        assertEquals(FontWeight.SemiBold, AxTypography.titleLarge.fontWeight)
    }

    @Test
    fun spacingShapesAndInteractionTokensStayConsistent() {
        val spacing = listOf(AxSpacing.xs, AxSpacing.sm, AxSpacing.md, AxSpacing.lg, AxSpacing.xl, AxSpacing.xxl, AxSpacing.section)
        assertEquals(listOf(4, 8, 12, 16, 20, 24, 32).map { it.dp }, spacing)
        assertTrue("Every step stays on the 4dp grid", spacing.all { it.value % 4f == 0f })
        assertEquals(48.dp, AxSize.minTouch)
        assertTrue(AxSize.fieldMinHeight >= AxSize.minTouch)
        assertTrue(AxSize.railItemMinHeight >= AxSize.minTouch)
        assertEquals(56.dp, AxSize.bottomNavigation)
        assertEquals(AxSpacing.lg, AxComponentTokens.cardPadding)
        assertEquals(AxSpacing.md, AxComponentTokens.cardGap)
        assertEquals(AxSpacing.sm, AxComponentTokens.controlGap)
        assertEquals(1.dp, AxComponentTokens.outlineWidth)
        assertEquals(2.dp, AxComponentTokens.selectedOutlineWidth)
        assertTrue(AxComponentTokens.selectedOutlineWidth > AxComponentTokens.outlineWidth)
        assertEquals(RoundedCornerShape(AxSize.cardRadius), AxShapes.large)
        assertEquals(RoundedCornerShape(AxSize.dialogRadius), AxShapes.extraLarge)
        assertEquals(RoundedCornerShape(AxSize.inputRadius), AxShapes.medium)
    }

    private fun assertTextContrast(theme: String, scheme: ColorScheme) {
        val pairs = with(scheme) {
            mutableListOf(
                Triple("primary", onPrimary, primary),
                Triple("primaryContainer", onPrimaryContainer, primaryContainer),
                Triple("secondary", onSecondary, secondary),
                Triple("secondaryContainer / selected card / info notice", onSecondaryContainer, secondaryContainer),
                Triple("tertiary", onTertiary, tertiary),
                Triple("tertiaryContainer / warning notice", onTertiaryContainer, tertiaryContainer),
                Triple("error", onError, error),
                Triple("errorContainer / error notice", onErrorContainer, errorContainer),
                Triple("background", onBackground, background),
                Triple("surfaceVariant", onSurfaceVariant, surfaceVariant),
                Triple("inverseSurface", inverseOnSurface, inverseSurface),
                Triple("inversePrimary", inversePrimary, inverseSurface)
            ).apply {
                val surfaces = mapOf(
                    "surface" to surface, "surfaceBright" to surfaceBright, "surfaceDim" to surfaceDim,
                    "surfaceContainerLowest" to surfaceContainerLowest, "surfaceContainerLow" to surfaceContainerLow,
                    "surfaceContainer" to surfaceContainer, "surfaceContainerHigh" to surfaceContainerHigh,
                    "surfaceContainerHighest" to surfaceContainerHighest
                )
                surfaces.forEach { (name, color) ->
                    add(Triple("onSurface / $name", onSurface, color))
                    add(Triple("onSurfaceVariant / $name", onSurfaceVariant, color))
                }
                add(Triple("primary text action / surface", primary, surface))
                add(Triple("destructive text action / surface", error, surface))
            }
        }
        pairs.forEach { (role, foreground, background) ->
            assertEquals("$theme $role foreground must be opaque", 1f, foreground.alpha, 0f)
            assertEquals("$theme $role background must be opaque", 1f, background.alpha, 0f)
            val ratio = (maxOf(foreground.luminance(), background.luminance()) + 0.05f) /
                (minOf(foreground.luminance(), background.luminance()) + 0.05f)
            assertTrue("$theme $role contrast is $ratio, below 4.5:1", ratio >= 4.5f)
        }
    }
}
