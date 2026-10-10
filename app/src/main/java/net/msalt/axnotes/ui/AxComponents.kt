package net.msalt.axnotes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/** Domain primitives. Material owns focus, pressed/ripple and disabled input behavior. */
@Composable
internal fun AxCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick, modifier = modifier.fillMaxWidth().semantics { this.selected = selected },
        shape = MaterialTheme.shapes.large, colors = axCardColors(selected),
        border = axCardBorder(selected), content = content
    )
}

/** Non-clickable container for cards with separate body and secondary actions. */
@Composable
internal fun AxCardFrame(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().semantics { this.selected = selected },
        shape = MaterialTheme.shapes.large, colors = axCardColors(selected),
        border = axCardBorder(selected), content = content
    )
}

@Composable
private fun axCardColors(selected: Boolean) = CardDefaults.cardColors(
    containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
)

@Composable
private fun axCardBorder(selected: Boolean) = BorderStroke(
    if (selected) AxComponentTokens.selectedOutlineWidth else AxComponentTokens.outlineWidth,
    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
)

@Composable
internal fun AxButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    loading: Boolean = false, content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick, enabled = enabled && !loading,
        modifier = modifier.heightIn(min = AxSize.minTouch).semantics {
            if (loading) { stateDescription = "처리 중"; liveRegion = LiveRegionMode.Polite }
        },
        shape = RoundedCornerShape(AxSize.buttonRadius),
        contentPadding = PaddingValues(horizontal = AxSpacing.xxl, vertical = AxSpacing.md),
        colors = ButtonDefaults.buttonColors()
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(AxSize.supportingIcon), color = LocalContentColor.current, strokeWidth = AxComponentTokens.selectedOutlineWidth)
            Spacer(Modifier.width(AxComponentTokens.controlGap))
        }
        content()
    }
}

@Composable
internal fun AxOutlinedButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.heightIn(min = AxSize.minTouch), enabled = enabled,
        shape = RoundedCornerShape(AxSize.buttonRadius),
        contentPadding = PaddingValues(horizontal = AxSpacing.xxl, vertical = AxSpacing.md), content = content
    )
}

@Composable
internal fun AxTextButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    destructive: Boolean = false, content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick, modifier = modifier.heightIn(min = AxSize.minTouch), enabled = enabled,
        shape = RoundedCornerShape(AxSize.buttonRadius),
        colors = ButtonDefaults.textButtonColors(contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
        content = content
    )
}

@Composable
internal fun AxFilterChip(
    selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, leadingIcon: (@Composable () -> Unit)? = null
) {
    FilterChip(
        selected = selected, onClick = onClick, label = label,
        modifier = modifier.sizeIn(minWidth = AxSize.minTouch, minHeight = AxSize.minTouch), enabled = enabled,
        shape = RoundedCornerShape(AxSize.filterRadius), leadingIcon = leadingIcon,
        trailingIcon = if (selected) { { Icon(Icons.Default.Check, null, Modifier.size(AxSize.supportingIcon)) } } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@Composable
internal fun AxTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, readOnly: Boolean = false, label: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null, isError: Boolean = false,
    singleLine: Boolean = false, minLines: Int = 1, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        modifier = modifier.heightIn(min = AxSize.fieldMinHeight), enabled = enabled,
        textStyle = MaterialTheme.typography.bodyLarge, label = label,
        leadingIcon = leadingIcon, trailingIcon = trailingIcon ?: if (isError) { { Icon(Icons.Default.Warning, "입력을 확인하세요") } } else null,
        supportingText = supportingText, readOnly = readOnly,
        isError = isError, singleLine = singleLine, minLines = minLines, maxLines = maxLines,
        shape = RoundedCornerShape(AxSize.inputRadius)
    )
}

internal enum class AxNoticeTone { Info, Warning, Error }

@Composable
internal fun AxNotice(text: String, modifier: Modifier = Modifier, tone: AxNoticeTone = AxNoticeTone.Info) {
    val scheme = MaterialTheme.colorScheme
    val (container, foreground) = when (tone) {
        AxNoticeTone.Info -> scheme.secondaryContainer to scheme.onSecondaryContainer
        AxNoticeTone.Warning -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        AxNoticeTone.Error -> scheme.errorContainer to scheme.onErrorContainer
    }
    Surface(color = container, contentColor = foreground, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(AxSpacing.lg), horizontalArrangement = Arrangement.spacedBy(AxSpacing.md), verticalAlignment = Alignment.Top) {
            Icon(if (tone == AxNoticeTone.Info) Icons.Default.Info else Icons.Default.Warning, null, Modifier.size(AxSize.icon))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
internal fun AxEmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = AxComponentTokens.sectionGap), verticalArrangement = Arrangement.spacedBy(AxSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
