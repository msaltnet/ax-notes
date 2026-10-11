@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package net.msalt.axnotes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
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
            initialDisplayMode = if (LocalDensity.current.fontScale >= 1.5f) DisplayMode.Input else DisplayMode.Picker,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= today
            }
        )
        DatePickerDialog(onDismissRequest = { picker = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            // Material adds wrapContentHeight internally. Keep its outer window full-height
            // so API 26 reports IME insets before centering the wrapped picker surface.
            modifier = Modifier.fillMaxHeight().systemBarsPadding().imePadding().padding(vertical = AxSpacing.sm),
            confirmButton = { AxButton(onClick = {
                state.selectedDateMillis?.let { onDueAtChange(reminderWithDate(dueAt, it)) }; picker = null
            }, enabled = state.selectedDateMillis != null) { Text("날짜 적용") } },
            dismissButton = { AxOutlinedButton(onClick = { picker = null }) { Text("취소") } }) {
            DatePicker(state = state, modifier = Modifier.verticalScroll(rememberScrollState()), title = { Text("날짜 선택", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(AxSpacing.lg)) }, headline = null)
        }
    }
    if (picker == "time") {
        val local = remember { Instant.ofEpochMilli(dueAt).atZone(ZoneId.systemDefault()) }
        var hour by rememberSaveable { mutableStateOf(local.hour.toString().padStart(2, '0')) }
        var minute by rememberSaveable { mutableStateOf(local.minute.toString().padStart(2, '0')) }
        val validHour = hour.toIntOrNull()?.takeIf { it in 0..23 }
        val validMinute = minute.toIntOrNull()?.takeIf { it in 0..59 }
        // Match DatePickerDialog's stable window sizing. Platform wrap-content negotiation
        // can oscillate with editable fields; explicit responsive bounds keep input stable.
        // A non-floating inset-aware window resizes above the IME on API 26+;
        // only the fields scroll, leaving Apply/Cancel reachable at large text sizes.
        AlertDialog(onDismissRequest = { picker = null }, title = { Text("시각 선택") },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            modifier = Modifier.systemBarsPadding().imePadding().padding(vertical = AxSpacing.sm)
                .widthIn(max = AxSize.dialogMaxWidth).fillMaxWidth().padding(horizontal = AxSpacing.xxl),
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AxSpacing.lg)) {
                Text("24시간 기준으로 입력하세요", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(hour, { if (it.length <= 2 && it.all(Char::isDigit)) hour = it },
                    modifier = Modifier.fillMaxWidth().testTag("reminder_hour"), textStyle = MaterialTheme.typography.bodyLarge,
                    label = { Text("시 (0–23)") }, singleLine = true, isError = validHour == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(minute, { if (it.length <= 2 && it.all(Char::isDigit)) minute = it },
                    modifier = Modifier.fillMaxWidth().testTag("reminder_minute"), textStyle = MaterialTheme.typography.bodyLarge,
                    label = { Text("분 (0–59)") }, singleLine = true, isError = validMinute == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            } },
            confirmButton = { AxButton(onClick = { onDueAtChange(reminderWithTime(dueAt, validHour!!, validMinute!!)); picker = null }, enabled = validHour != null && validMinute != null) { Text("시각 적용") } },
            dismissButton = { AxOutlinedButton(onClick = { picker = null }) { Text("취소") } })
    }
}
