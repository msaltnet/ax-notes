package net.msalt.axnotes.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** A separate 48dp gutter exposes overflow without covering an action or its drag target. */
@Composable
internal fun ReaderActions(enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var fullWidth by remember { mutableIntStateOf(0) }
    var contentWidth by remember { mutableIntStateOf(0) }
    var viewportWidth by remember { mutableIntStateOf(0) }
    // Compare against the full width, not the smaller viewport after adding the
    // gutter, so it disappears again when a resized window can fit every action.
    val overflow = contentWidth > fullWidth && fullWidth > 0
    val surface = MaterialTheme.colorScheme.background
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        Modifier.fillMaxWidth().onSizeChanged { fullWidth = it.width },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.weight(1f).onSizeChanged { viewportWidth = it.width }.drawWithContent {
                drawContent()
                val fadeWidth = AxSpacing.lg.toPx().coerceAtMost(size.width)
                fun fade(left: Boolean) {
                    val x = if (left) 0f else size.width - fadeWidth
                    val colors = if (left) listOf(surface, surface.copy(alpha = 0f)) else listOf(surface.copy(alpha = 0f), surface)
                    drawRect(Brush.horizontalGradient(colors, x, x + fadeWidth), Offset(x, 0f), Size(fadeWidth, size.height))
                }
                if (scroll.canScrollBackward) fade(left = !rtl)
                if (scroll.canScrollForward) fade(left = rtl)
            }
        ) {
            Row(
                Modifier.fillMaxWidth().testTag("reader_actions").horizontalScroll(scroll, enabled = enabled)
                    .onSizeChanged { contentWidth = it.width }.padding(horizontal = AxSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AxSpacing.xs),
                content = content
            )
        }
        if (overflow) {
            val forward = scroll.canScrollForward
            IconButton(
                enabled = enabled,
                onClick = {
                    val target = if (forward) (scroll.value + (viewportWidth * 0.75f).roundToInt()).coerceAtMost(scroll.maxValue) else 0
                    scope.launch { scroll.animateScrollTo(target) }
                },
                modifier = Modifier.size(AxSize.minTouch).testTag(if (forward) "reader_actions_next" else "reader_actions_previous")
            ) {
                Icon(
                    if (forward) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                    if (forward) "뒤쪽 작업 더 보기" else "처음 작업 보기",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
