package net.msalt.axnotes.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import net.msalt.axnotes.R

private val destinations = listOf("notes" to "Notes", "library" to "내 보관함", "search" to "검색", "settings" to "설정")

@Composable
internal fun CompactNavigation(selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val navigationColors = LocalAxNavigationColors.current
    val selectedIndex = destinations.indexOfFirst { it.first == selected }
    // Initial composition/restoration starts at the actual destination, not at Notes.
    // Compose animation clocks honor the system animator duration scale, including 0.
    val indicatorIndex by animateFloatAsState(
        selectedIndex.coerceAtLeast(0).toFloat(),
        spring(dampingRatio = 0.8f, stiffness = 650f), label = "navigationUnderline"
    )
    Surface(color = colors.surface) {
        Column {
            HorizontalDivider()
            Box(Modifier.fillMaxWidth().navigationBarsPadding().height(AxSize.bottomNavigation)) {
                Row(Modifier.fillMaxSize().selectableGroup().testTag("bottom_navigation"), verticalAlignment = Alignment.CenterVertically) {
                    destinations.forEach { (route, label) ->
                        val isSelected = selected == route
                        val interactionSource = remember { MutableInteractionSource() }
                        val pop = remember { Animatable(1f) }
                        var previousSelection by remember { mutableStateOf(isSelected) }
                        LaunchedEffect(isSelected) {
                            val newlySelected = isSelected && !previousSelection
                            previousSelection = isSelected
                            // A new selection cancels the old pulse. Reselecting a tab does not replay it.
                            pop.snapTo(1f)
                            if (newlySelected) {
                                pop.animateTo(1.14f, tween(90))
                                pop.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 700f))
                            }
                        }
                        Box(Modifier.weight(1f).fillMaxHeight().selectable(
                            selected = isSelected, interactionSource = interactionSource,
                            indication = null, role = Role.Tab, onClick = { onSelect(route) }
                        ).semantics { contentDescription = label }.testTag("nav_$route"), contentAlignment = Alignment.Center) {
                            val tint = if (isSelected) navigationColors.selectedIcon else navigationColors.unselectedIcon
                            Box(Modifier.size(AxSize.minTouch).clip(CircleShape)
                                .indication(interactionSource, ripple(bounded = false, radius = AxSize.minTouch / 2, color = colors.primary)),
                                contentAlignment = Alignment.Center) {
                                val iconModifier = Modifier.size(AxSize.icon).graphicsLayer {
                                    scaleX = pop.value
                                    scaleY = pop.value
                                }
                                when (route) {
                                    "notes" -> Icon(painterResource(R.drawable.ic_article), null, iconModifier, tint = tint)
                                    "library" -> Icon(painterResource(R.drawable.ic_library), null, iconModifier, tint = tint)
                                    "search" -> Icon(Icons.Default.Search, null, iconModifier, tint = tint)
                                    else -> Icon(Icons.Default.Settings, null, iconModifier, tint = tint)
                                }
                            }
                        }
                    }
                }
                // Decorative only: no duplicate accessibility node or intercepted touch target.
                Canvas(Modifier.matchParentSize()) {
                    if (selectedIndex >= 0) {
                        val slot = size.width / destinations.size
                        val logicalCenter = (indicatorIndex + 0.5f) * slot
                        val center = if (layoutDirection == LayoutDirection.Rtl) size.width - logicalCenter else logicalCenter
                        drawRoundRect(navigationColors.selectedIcon,
                            topLeft = Offset(center - 9.dp.toPx(), size.height - 8.dp.toPx()),
                            size = Size(18.dp.toPx(), 3.dp.toPx()), cornerRadius = CornerRadius(1.5.dp.toPx()))
                    }
                }
            }
        }
    }
}
