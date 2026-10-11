@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package net.msalt.axnotes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.window.DialogProperties
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

/** DatePicker uses UTC dates; reminders keep local wall-clock date/time. */
internal fun reminderDatePickerValue(dueAt: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun reminderWithDate(dueAt: Long, selectedDateUtc: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
    val current = Instant.ofEpochMilli(dueAt).atZone(zone)
    val date = Instant.ofEpochMilli(selectedDateUtc).atZone(ZoneOffset.UTC).toLocalDate()
    return date.atTime(current.toLocalTime().withSecond(0).withNano(0)).atZone(zone).toInstant().toEpochMilli()
}

internal fun reminderWithTime(dueAt: Long, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate().atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

@Composable
internal fun AxReminderDialog(dueAt: Long, onDueAtChange: (Long) -> Unit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    val summaryScroll = rememberScrollState()
    var invalidPast by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(dueAt) { invalidPast = false }
    if (picker == null) AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("나중에 읽기") },
        text = {
            Column(Modifier.verticalScroll(summaryScroll), verticalArrangement = Arrangement.spacedBy(AxSpacing.lg)) {
                Text("다시 읽고 싶은 날짜와 시각을 정하세요", style = MaterialTheme.typography.bodyMedium)
                AxOutlinedButton(onClick = { picker = "date" }, modifier = Modifier.fillMaxWidth().testTag("reminder_date")) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AxSpacing.xs)) {
                        Text("날짜", style = MaterialTheme.typography.labelMedium)
                        Text(DateFormat.getDateInstance(DateFormat.FULL).format(Date(dueAt)), style = MaterialTheme.typography.titleMedium)
                    }
                }
                AxOutlinedButton(onClick = { picker = "time" }, modifier = Modifier.fillMaxWidth().testTag("reminder_time")) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AxSpacing.xs)) {
                        Text("시각", style = MaterialTheme.typography.labelMedium)
                        Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(dueAt)), style = MaterialTheme.typography.titleMedium)
                    }
                }
                // Keep a complete confirmation summary visible after either picker closes.
                Text("예약할 시각 · ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(dueAt))}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("reminder_summary").semantics { liveRegion = LiveRegionMode.Polite })
                if (invalidPast) Text("미래 시각을 선택하세요", color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Text("정확한 시각을 보장하지 않아요. 절전·OS 제한으로 늦어지거나 전달되지 않을 수 있습니다", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { AxButton(onClick = { if (dueAt <= System.currentTimeMillis()) invalidPast = true else onConfirm() }, modifier = Modifier.testTag("reminder_confirm")) { Text("예약") } },
        dismissButton = { AxOutlinedButton(onClick = onDismiss) { Text("취소") } }
    )
    if (picker == "date") {
        val today = remember { LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = reminderDatePickerValue(dueAt),
            initialDisplayMode = DisplayMode.Picker,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= today
            }
        )
        DatePickerDialog(onDismissRequest = { picker = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            modifier = Modifier.fillMaxHeight().systemBarsPadding().padding(vertical = AxSpacing.sm),
            // Add breathing room inside the surface, below both actions. Material's
            // default 8dp footer is too tight against these full-height AX buttons.
            confirmButton = {
                Box(Modifier.testTag("reminder_date_apply_inset").padding(bottom = AxSpacing.lg)) {
                    AxButton(onClick = {
                        state.selectedDateMillis?.let { onDueAtChange(reminderWithDate(dueAt, it)) }; picker = null
                    }, enabled = state.selectedDateMillis != null,
                        modifier = Modifier.testTag("reminder_date_apply")) { Text("날짜 적용") }
                }
            },
            dismissButton = {
                Box(Modifier.testTag("reminder_date_cancel_inset").padding(bottom = AxSpacing.lg)) {
                    AxOutlinedButton(onClick = { picker = null }, modifier = Modifier.testTag("reminder_date_cancel")) { Text("취소") }
                }
            }) {
            DatePicker(state = state, showModeToggle = false,
                modifier = Modifier.testTag("reminder_calendar").verticalScroll(rememberScrollState()),
                title = { Text("날짜 선택", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(AxSpacing.lg)) }, headline = null)
        }
    }
    if (picker == "time") {
        val local = remember { Instant.ofEpochMilli(dueAt).atZone(ZoneId.systemDefault()) }
        val hourState = rememberLazyListState(initialFirstVisibleItemIndex = local.hour)
        val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = local.minute)
        val hour by remember { derivedStateOf { hourState.centeredValue(local.hour) } }
        val minute by remember { derivedStateOf { minuteState.centeredValue(local.minute) } }
        // Each wheel keeps its own scroll state until Apply. Closing this branch discards
        // both drafts, so Cancel/Back and reopening always use the last applied reminder.
        AlertDialog(onDismissRequest = { picker = null }, title = { Text("시각 선택") },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            modifier = Modifier.systemBarsPadding().padding(vertical = AxSpacing.sm)
                .widthIn(max = AxSize.dialogMaxWidth).fillMaxWidth().padding(horizontal = AxSpacing.lg),
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AxSpacing.lg)) {
                    Text("시와 분을 위아래로 밀어 선택하세요 · 24시간", style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AxSpacing.lg)) {
                        ReminderTimeWheel("시", 0..23, hour, hourState, "reminder_hour", Modifier.weight(1f))
                        ReminderTimeWheel("분", 0..59, minute, minuteState, "reminder_minute", Modifier.weight(1f))
                    }
                }
            },
            confirmButton = { AxButton(onClick = { onDueAtChange(reminderWithTime(dueAt, hour, minute)); picker = null },
                enabled = !hourState.isScrollInProgress && !minuteState.isScrollInProgress) { Text("시각 적용") } },
            dismissButton = { AxOutlinedButton(onClick = { picker = null }) { Text("취소") } })
    }
}

/** Content padding lets the first and last values occupy the same center selection row. */
private fun LazyListState.centeredValue(fallback: Int): Int {
    val layout = layoutInfo
    val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
    return layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index ?: fallback
}

@Composable
private fun ReminderTimeWheel(
    label: String,
    values: IntRange,
    selectedValue: Int,
    state: LazyListState,
    tag: String,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // Scale the row, not the type, so 200% text keeps its full size and touch targets.
    val rowHeight = with(density) { MaterialTheme.typography.bodyLarge.lineHeight.toDp() + 16.dp }
        .coerceAtLeast(AxSize.minTouch)
    fun select(value: Int) {
        scope.launch { state.animateScrollToItem(value.coerceIn(values)) }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = AxSpacing.sm))
        Box(Modifier.fillMaxWidth().height(rowHeight * 3)) {
            Surface(Modifier.align(Alignment.Center).fillMaxWidth().height(rowHeight),
                shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {}
            LazyColumn(state = state, flingBehavior = rememberSnapFlingBehavior(state),
                contentPadding = PaddingValues(vertical = rowHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().testTag(tag).semantics {
                    contentDescription = "$label 선택"
                    stateDescription = "${selectedValue.toString().padStart(2, '0')}$label"
                    progressBarRangeInfo = ProgressBarRangeInfo(selectedValue.toFloat(), values.first.toFloat()..values.last.toFloat(), values.count() - 2)
                    setProgress { value -> select(value.roundToInt()); true }
                    customActions = listOf(
                        CustomAccessibilityAction("이전 $label") { select(selectedValue - 1); true },
                        CustomAccessibilityAction("다음 $label") { select(selectedValue + 1); true }
                    )
                }) {
                items(values.toList(), key = { it }) { value ->
                    Box(Modifier.fillMaxWidth().height(rowHeight)
                        .selectable(selected = value == selectedValue, role = Role.RadioButton, onClick = { select(value) })
                        .testTag("${tag}_$value"), contentAlignment = Alignment.Center) {
                        Text(value.toString().padStart(2, '0'), style = MaterialTheme.typography.bodyLarge,
                            color = if (value == selectedValue) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
