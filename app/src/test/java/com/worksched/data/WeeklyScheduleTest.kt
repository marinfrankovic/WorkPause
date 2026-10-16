package com.worksched.data

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class WeeklyScheduleTest {
    private val zone = ZoneId.of("Europe/Zagreb")
    private fun at(date: String): Long = ZonedDateTime.parse(date + "[Europe/Zagreb]").toInstant().toEpochMilli()
    private fun emptyWeek(): Schedule = Schedule.default().copy(
        daySchedules = Schedule.ORDER.associateWith { DaySchedule(enabled = false) }
    )

    @Test fun `each day has independent times`() {
        val week = emptyWeek()
            .withDay(Calendar.MONDAY, DaySchedule(true, 8 * 60, 17 * 60))
            .withDay(Calendar.TUESDAY, DaySchedule(true, 10 * 60, 19 * 60))
        assertTrue(week.isActiveAt(at("2026-10-19T09:00:00+02:00"), zone))
        assertFalse(week.isActiveAt(at("2026-10-20T09:00:00+02:00"), zone))
        assertTrue(week.isActiveAt(at("2026-10-20T18:00:00+02:00"), zone))
        assertEquals(8 * 60, week.forDay(Calendar.MONDAY).resumeMinutes)
    }

    @Test fun `copy replaces selected targets but leaves other days unchanged`() {
        val original = Schedule.default().withDay(Calendar.MONDAY, DaySchedule(true, 9 * 60, 16 * 60))
        val copied = original.copyDayTo(Calendar.MONDAY, setOf(Calendar.TUESDAY, Calendar.SATURDAY))
        assertEquals(copied.forDay(Calendar.MONDAY), copied.forDay(Calendar.TUESDAY))
        assertEquals(copied.forDay(Calendar.MONDAY), copied.forDay(Calendar.SATURDAY))
        assertEquals(original.forDay(Calendar.WEDNESDAY), copied.forDay(Calendar.WEDNESDAY))
        assertFalse(original.forDay(Calendar.SATURDAY).enabled)
    }

    @Test fun `copy includes disabled state and empty copy changes nothing`() {
        val original = Schedule.default()
        assertFalse(original.copyDayTo(Calendar.SUNDAY, setOf(Calendar.MONDAY)).forDay(Calendar.MONDAY).enabled)
        assertEquals(original, original.copyDayTo(Calendar.MONDAY, emptySet()))
    }

    @Test fun `overnight hours end on the following day`() {
        val week = emptyWeek()
            .withDay(Calendar.FRIDAY, DaySchedule(true, 22 * 60, 6 * 60))
            .withDay(Calendar.SATURDAY, DaySchedule(true, 12 * 60, 18 * 60))
        assertTrue(week.isActiveAt(at("2026-10-24T05:59:00+02:00"), zone))
        assertFalse(week.isActiveAt(at("2026-10-24T06:00:00+02:00"), zone))
        assertEquals(at("2026-10-24T06:00:00+02:00"),
            week.nextTransition(at("2026-10-23T23:00:00+02:00"), zone)?.atMillis)
    }

    @Test fun `overlapping windows do not pause an active workday`() {
        val week = emptyWeek()
            .withDay(Calendar.MONDAY, DaySchedule(true, 22 * 60, 8 * 60))
            .withDay(Calendar.TUESDAY, DaySchedule(true, 7 * 60, 17 * 60))
        val next = week.nextTransition(at("2026-10-20T07:30:00+02:00"), zone)
        assertEquals(at("2026-10-20T17:00:00+02:00"), next?.atMillis)
        assertEquals(false, next?.resume)
    }

    @Test fun `master off cancels active hours and transitions`() {
        val week = Schedule.default().copy(enabled = false)
        assertFalse(week.isActiveAt(at("2026-10-19T10:00:00+02:00"), zone))
        assertNull(week.nextTransition(at("2026-10-19T10:00:00+02:00"), zone))
    }

    @Test fun `WorkPause off takes priority over all day ON`() {
        val week = emptyWeek().withDay(Calendar.MONDAY, DaySchedule(mode = DayMode.ALL_ON))
            .copy(enabled = false)
        assertFalse(week.isActiveAt(at("2026-10-19T10:00:00+02:00"), zone))
        assertNull(week.nextTransition(at("2026-10-19T10:00:00+02:00"), zone))
        assertNull(week.activeWindowEndMillis(at("2026-10-19T10:00:00+02:00"), zone))
    }

    @Test fun `editing and copying days while WorkPause is off do not enable it`() {
        val off = Schedule.default().copy(enabled = false)
        val edited = off.withDay(Calendar.MONDAY, DaySchedule(true, 9 * 60, 17 * 60))
            .copyDayTo(Calendar.MONDAY, setOf(Calendar.TUESDAY))
        assertFalse(edited.enabled)
        assertEquals(edited.forDay(Calendar.MONDAY), edited.forDay(Calendar.TUESDAY))
        assertEquals(9 * 60, edited.forDay(Calendar.TUESDAY).resumeMinutes)
        assertFalse(edited.isActiveAt(at("2026-10-19T10:00:00+02:00"), zone))
    }

    @Test fun `disabled days have no hours or transitions`() {
        val week = emptyWeek()
        assertFalse(week.isActiveAt(at("2026-10-19T10:00:00+02:00"), zone))
        assertNull(week.nextTransition(at("2026-10-19T10:00:00+02:00"), zone))
    }

    @Test fun `equal times and out of range minutes are rejected for active days`() {
        assertNotNull(Schedule.default().withDay(Calendar.MONDAY, DaySchedule(true, 480, 480)).validationError())
        assertNotNull(Schedule.default().withDay(Calendar.MONDAY, DaySchedule(true, -1, 1080)).validationError())
        assertNotNull(Schedule.default().withDay(Calendar.MONDAY, DaySchedule(true, 480, 1440)).validationError())
        assertNull(Schedule.default().withDay(Calendar.MONDAY, DaySchedule(false, 480, 480)).validationError())
    }

    @Test fun `legacy shared hours migrate without changing enabled weekdays`() {
        val legacy = Json.decodeFromString<Schedule>(
            """{"days":[2,4],"enableHour":9,"enableMinute":30,"disableHour":17,"disableMinute":15}"""
        )
        assertEquals(DaySchedule(true, 570, 1035), legacy.forDay(Calendar.MONDAY))
        assertEquals(DaySchedule(false, 570, 1035), legacy.forDay(Calendar.TUESDAY))
        val changed = legacy.withDay(Calendar.WEDNESDAY, DaySchedule(true, 600, 1200))
        assertEquals(legacy.forDay(Calendar.MONDAY), changed.forDay(Calendar.MONDAY))
    }

    @Test fun `daily schedules survive serialization`() {
        val week = Schedule.default().withDay(Calendar.WEDNESDAY, DaySchedule(true, 630, 1245))
        assertEquals(week, Json.decodeFromString<Schedule>(Json.encodeToString(week)))
    }

    @Test fun `next transition crosses the end of the week`() {
        val week = emptyWeek().withDay(Calendar.MONDAY, DaySchedule(true, 480, 1080))
        val next = week.nextTransition(at("2026-10-25T20:00:00+01:00"), zone)
        assertEquals(at("2026-10-26T08:00:00+01:00"), next?.atMillis)
        assertEquals(true, next?.resume)
    }

    @Test fun `spring clock change resolves nonexistent local time forward`() {
        val week = emptyWeek().withDay(Calendar.SUNDAY, DaySchedule(true, 150, 240))
        val next = week.nextTransition(at("2026-03-29T01:00:00+01:00"), zone)
        assertEquals(at("2026-03-29T03:30:00+02:00"), next?.atMillis)
        assertTrue(week.isActiveAt(at("2026-03-29T03:45:00+02:00"), zone))
    }

    @Test fun `all day on covers midnight inclusively until next midnight`() {
        val week = emptyWeek().withDay(Calendar.MONDAY, DaySchedule(mode = DayMode.ALL_ON))
        assertFalse(week.isActiveAt(at("2026-10-18T23:59:59+02:00"), zone))
        assertTrue(week.isActiveAt(at("2026-10-19T00:00:00+02:00"), zone))
        assertTrue(week.isActiveAt(at("2026-10-19T23:59:59+02:00"), zone))
        assertFalse(week.isActiveAt(at("2026-10-20T00:00:00+02:00"), zone))
    }

    @Test fun `all day off truncates previous overnight hours at midnight`() {
        val week = emptyWeek().withDay(Calendar.FRIDAY, DaySchedule(true, 22 * 60, 6 * 60))
        assertTrue(week.isActiveAt(at("2026-10-23T23:59:59+02:00"), zone))
        assertFalse(week.isActiveAt(at("2026-10-24T00:00:00+02:00"), zone))
        assertFalse(week.isActiveAt(at("2026-10-24T05:00:00+02:00"), zone))
        assertEquals(at("2026-10-24T00:00:00+02:00"),
            week.nextTransition(at("2026-10-23T23:00:00+02:00"), zone)?.atMillis)
    }

    @Test fun `all day off after all day on pauses at midnight`() {
        val week = emptyWeek().withDay(Calendar.SUNDAY, DaySchedule(mode = DayMode.ALL_ON))
        val next = week.nextTransition(at("2026-10-18T22:00:00+02:00"), zone)
        assertEquals(at("2026-10-19T00:00:00+02:00"), next?.atMillis)
        assertEquals(false, next?.resume)
    }

    @Test fun `all day on merges with previous overnight hours`() {
        val week = emptyWeek()
            .withDay(Calendar.MONDAY, DaySchedule(true, 22 * 60, 6 * 60))
            .withDay(Calendar.TUESDAY, DaySchedule(mode = DayMode.ALL_ON))
        val next = week.nextTransition(at("2026-10-19T23:00:00+02:00"), zone)
        assertEquals(at("2026-10-21T00:00:00+02:00"), next?.atMillis)
        assertEquals(false, next?.resume)
    }

    @Test fun `copy includes every daily mode and preserves remembered hours`() {
        DayMode.entries.forEach { mode ->
            val original = Schedule.default()
                .withDay(Calendar.MONDAY, DaySchedule(true, 600, 1200, mode))
            val copied = original.copyDayTo(Calendar.MONDAY, setOf(Calendar.SATURDAY))
            assertEquals(original.forDay(Calendar.MONDAY), copied.forDay(Calendar.SATURDAY))
            assertEquals(mode, copied.forDay(Calendar.SATURDAY).effectiveMode())
            assertEquals(600, copied.forDay(Calendar.SATURDAY).resumeMinutes)
        }
    }

    @Test fun `all daily modes survive serialization`() {
        val week = Schedule.default()
            .withDay(Calendar.MONDAY, DaySchedule(mode = DayMode.ALL_ON))
            .withDay(Calendar.TUESDAY, DaySchedule(mode = DayMode.ALL_OFF))
        val restored = Json.decodeFromString<Schedule>(Json.encodeToString(week))
        assertEquals(week, restored)
        assertEquals(DayMode.ALL_ON, restored.forDay(Calendar.MONDAY).effectiveMode())
        assertEquals(DayMode.ALL_OFF, restored.forDay(Calendar.TUESDAY).effectiveMode())
        assertEquals(DayMode.SCHEDULE, restored.forDay(Calendar.WEDNESDAY).effectiveMode())
    }

    @Test fun `continuously on week has no transition and bounded arrival monitoring`() {
        val week = Schedule.default().copy(
            daySchedules = Schedule.ORDER.associateWith { DaySchedule(mode = DayMode.ALL_ON) }
        )
        val now = at("2026-10-19T12:00:00+02:00")
        assertTrue(week.isActiveAt(now, zone))
        assertNull(week.nextTransition(now, zone))
        assertEquals(at("2026-10-26T12:00:00+01:00"), week.activeWindowEndMillis(now, zone))
    }

    @Test fun `all day on uses calendar midnight across short and long DST days`() {
        val week = emptyWeek().withDay(Calendar.SUNDAY, DaySchedule(mode = DayMode.ALL_ON))
        val spring = at("2026-03-29T00:00:00+01:00")
        val autumn = at("2026-10-25T00:00:00+02:00")
        assertEquals(spring + 23 * 60 * 60 * 1000L, week.activeWindowEndMillis(spring, zone))
        assertEquals(autumn + 25 * 60 * 60 * 1000L, week.activeWindowEndMillis(autumn, zone))
    }

    @Test fun `remembered equal hours do not block all day modes`() {
        DayMode.entries.filter { it != DayMode.SCHEDULE }.forEach { mode ->
            assertNull(Schedule.default().withDay(Calendar.MONDAY, DaySchedule(true, 480, 480, mode)).validationError())
        }
        assertNotNull(Schedule.default().withDay(Calendar.MONDAY, DaySchedule(true, 480, 480)).validationError())
    }

    @Test fun `legacy disabled days become all day off`() {
        val legacy = Json.decodeFromString<Schedule>("""{"days":[2],"enableHour":9,"disableHour":17}""")
        assertEquals(DayMode.SCHEDULE, legacy.forDay(Calendar.MONDAY).effectiveMode())
        assertEquals(DayMode.ALL_OFF, legacy.forDay(Calendar.TUESDAY).effectiveMode())
    }
}
