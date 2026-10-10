package net.msalt.axnotes.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** AX's reading-oriented adaptation of the official Material 3 Design Kit. */
object AxSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val section = 32.dp
}

object AxSize {
    val icon = 24.dp
    val supportingIcon = 20.dp
    val emptyStateIcon = 40.dp
    val minTouch = 48.dp
    // The system navigation inset is added outside this content height.
    val bottomNavigation = 56.dp
    val cardRadius = 16.dp
    val dialogRadius = 28.dp
    val inputRadius = 12.dp
    val buttonRadius = 12.dp
    val filterRadius = 8.dp
    val fieldMinHeight = 56.dp
    val editorMinHeight = 200.dp
    val listMaxWidth = 760.dp
    val readerMaxWidth = 880.dp
    val railWidth = 96.dp
    val largeTextRailWidth = 112.dp
    val railItemMinHeight = 72.dp
    val placeholderMaxWidth = 480.dp
}

internal val AxLightColors = lightColorScheme(
    primary = Color(0xFF315E50),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5E9DB),
    onPrimaryContainer = Color(0xFF163C2E),
    inversePrimary = Color(0xFFB3D5C3),
    secondary = Color(0xFF506458),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDFE9DF),
    onSecondaryContainer = Color(0xFF243D30),
    tertiary = Color(0xFF776038),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2E4C5),
    onTertiaryContainer = Color(0xFF493714),
    background = Color(0xFFFAF8F0),
    onBackground = Color(0xFF202820),
    surface = Color(0xFFFAF8F0),
    onSurface = Color(0xFF202820),
    surfaceVariant = Color(0xFFE6E9DE),
    onSurfaceVariant = Color(0xFF515C52),
    surfaceTint = Color(0xFF315E50),
    inverseSurface = Color(0xFF2B332C),
    inverseOnSurface = Color(0xFFF3F3E9),
    outline = Color(0xFF747E72),
    outlineVariant = Color(0xFFCDD3C7),
    surfaceBright = Color(0xFFFEFCF6),
    surfaceDim = Color(0xFFDDDCD3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F3EB),
    surfaceContainer = Color(0xFFF0EEE5),
    surfaceContainerHigh = Color(0xFFEAE8DF),
    surfaceContainerHighest = Color(0xFFE4E3DA),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

internal val AxDarkColors = darkColorScheme(
    primary = Color(0xFFB3D5C3),
    onPrimary = Color(0xFF113829),
    primaryContainer = Color(0xFF2D5141),
    onPrimaryContainer = Color(0xFFD5E9DB),
    inversePrimary = Color(0xFF315E50),
    secondary = Color(0xFFBDCFBE),
    onSecondary = Color(0xFF283D30),
    secondaryContainer = Color(0xFF3B5141),
    onSecondaryContainer = Color(0xFFDFE9DF),
    tertiary = Color(0xFFE1C78D),
    onTertiary = Color(0xFF403111),
    tertiaryContainer = Color(0xFF5B4722),
    onTertiaryContainer = Color(0xFFF2E4C5),
    background = Color(0xFF151D19),
    onBackground = Color(0xFFE3E8DF),
    surface = Color(0xFF151D19),
    onSurface = Color(0xFFE3E8DF),
    surfaceVariant = Color(0xFF3F4940),
    onSurfaceVariant = Color(0xFFC3CCC0),
    surfaceTint = Color(0xFFB3D5C3),
    inverseSurface = Color(0xFFE3E8DF),
    inverseOnSurface = Color(0xFF29322B),
    outline = Color(0xFF8D998C),
    outlineVariant = Color(0xFF424D43),
    surfaceBright = Color(0xFF364139),
    surfaceDim = Color(0xFF101713),
    surfaceContainerLowest = Color(0xFF101713),
    surfaceContainerLow = Color(0xFF1A241D),
    surfaceContainer = Color(0xFF1E2922),
    surfaceContainerHigh = Color(0xFF29342C),
    surfaceContainerHighest = Color(0xFF343F37),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

internal val AxTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)

internal val AxShapes = Shapes(
    extraSmall = RoundedCornerShape(AxSpacing.xs),
    small = RoundedCornerShape(AxSpacing.sm),
    medium = RoundedCornerShape(AxSpacing.md),
    large = RoundedCornerShape(AxSize.cardRadius),
    extraLarge = RoundedCornerShape(AxSize.dialogRadius)
)

/** Component-level decisions, separated from the spacing scale. */
object AxComponentTokens {
    val cardPadding = AxSpacing.lg
    val cardGap = AxSpacing.md
    val pageMargin = AxSpacing.xl // AX density choice, not a stock Material token.
    val sectionGap = AxSpacing.xxl
    val controlGap = AxSpacing.sm
    val outlineWidth = 1.dp
    val selectedOutlineWidth = 2.dp
    val placeholderHorizontalPadding = 36.dp
    val placeholderVerticalPadding = 72.dp
}

@Composable
fun AxTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) AxDarkColors else AxLightColors,
        typography = AxTypography,
        shapes = AxShapes,
        content = content
    )
}
