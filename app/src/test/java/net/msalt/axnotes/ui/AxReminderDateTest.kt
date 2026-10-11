package net.msalt.axnotes.ui

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** DatePicker's midnight-UTC values must not shift the user's local reminder day. */
class AxReminderDateTest {
    @Test
    fun pickerUsesLocalCalendarDayAcrossPositiveAndNegativeUtcOffsets() {
        val instant = Instant.parse("2026-10-11T00:30:45.123Z").toEpochMilli()
        assertEquals(utcDay("2026-10-11"), reminderDatePickerValue(instant, ZoneId.of("Asia/Seoul")))
        assertEquals(utcDay("2026-10-10"), reminderDatePickerValue(instant, ZoneId.of("America/Los_Angeles")))
        assertEquals(utcDay("2026-10-11"), reminderDatePickerValue(instant, ZoneOffset.UTC))
    }

    @Test
    fun applyingDateKeepsLocalHourAndMinuteInBothHemispheres() {
        listOf("Asia/Seoul", "America/Los_Angeles", "Asia/Kathmandu").forEach { name ->
            val zone = ZoneId.of(name)
            val original = LocalDateTime.of(2026, 10, 10, 23, 47, 59, 123_000_000)
                .atZone(zone).toInstant().toEpochMilli()
            val updated = reminderWithDate(original, utcDay("2026-10-15"), zone)
            assertEquals("$name must preserve wall-clock time and remove seconds",
                LocalDateTime.of(2026, 10, 15, 23, 47), localTime(updated, zone))
            assertEquals(utcDay("2026-10-15"), reminderDatePickerValue(updated, zone))
        }
    }

    @Test
    fun applyingDateRecalculatesDaylightSavingOffsetWithoutChangingWallClockTime() {
        val zone = ZoneId.of("America/New_York")
        val original = Instant.parse("2026-03-07T14:35:00Z").toEpochMilli() // 09:35 EST
        val updated = reminderWithDate(original, utcDay("2026-03-09"), zone)
        assertEquals(Instant.parse("2026-03-09T13:35:00Z").toEpochMilli(), updated) // 09:35 EDT
        assertEquals(LocalDateTime.of(2026, 3, 9, 9, 35), localTime(updated, zone))
    }

    @Test
    fun applyingTimeKeepsLocalDayAtMidnightAndEndOfDay() {
        listOf("Asia/Seoul", "America/Los_Angeles", "Asia/Kathmandu").forEach { name ->
            val zone = ZoneId.of(name)
            val original = LocalDateTime.of(2026, 10, 11, 12, 30, 45)
                .atZone(zone).toInstant().toEpochMilli()
            listOf(0 to 0, 23 to 59).forEach { (hour, minute) ->
                val updated = reminderWithTime(original, hour, minute, zone)
                assertEquals("$name must retain the local date", LocalDateTime.of(2026, 10, 11, hour, minute), localTime(updated, zone))
                assertEquals(utcDay("2026-10-11"), reminderDatePickerValue(updated, zone))
            }
        }
    }

    @Test
    fun invalidHoursAndMinutesCannotCreateAReminderTimestamp() {
        val original = Instant.parse("2026-10-11T12:30:00Z").toEpochMilli()
        listOf(-1 to 30, 24 to 30, 12 to -1, 12 to 60).forEach { (hour, minute) ->
            assertThrows(DateTimeException::class.java) { reminderWithTime(original, hour, minute, ZoneOffset.UTC) }
        }
    }

    private fun utcDay(date: String) = LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    private fun localTime(time: Long, zone: ZoneId) = Instant.ofEpochMilli(time).atZone(zone).toLocalDateTime()
}
