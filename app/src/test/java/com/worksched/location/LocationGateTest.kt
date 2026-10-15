package com.worksched.location

import com.worksched.data.Schedule
import com.worksched.data.DaySchedule
import java.util.Calendar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocationGateTest {
    private val schedule = Schedule(
        locationEnabled = true, latitude = 45.8, longitude = 16.0,
        enableHour = 9, disableHour = 17, days = listOf(Calendar.MONDAY)
    )

    private fun time(day: Int, hour: Int, minute: Int = 0): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }

    @Test fun `fix age includes threshold but rejects stale and future samples`() {
        assertTrue(LocationGate.isUsableFix(0, 10f))
        assertTrue(LocationGate.isUsableFix(LocationGate.MAX_FIX_AGE_NANOS, 10f))
        assertFalse(LocationGate.isUsableFix(LocationGate.MAX_FIX_AGE_NANOS + 1, 10f))
        assertFalse(LocationGate.isUsableFix(-1, 10f))
    }

    @Test fun `fix accuracy must be finite and positive`() {
        assertFalse(LocationGate.isUsableFix(0, 0f))
        assertFalse(LocationGate.isUsableFix(0, -1f))
        assertFalse(LocationGate.isUsableFix(0, Float.NaN))
        assertFalse(LocationGate.isUsableFix(0, Float.POSITIVE_INFINITY))
    }

    @Test fun `location configuration rejects invalid coordinates and radius`() {
        assertTrue(schedule.hasValidLocation())
        assertFalse(schedule.copy(latitude = 91.0).hasValidLocation())
        assertFalse(schedule.copy(longitude = -181.0).hasValidLocation())
        assertFalse(schedule.copy(latitude = Double.NaN).hasValidLocation())
        assertFalse(schedule.copy(longitude = Double.POSITIVE_INFINITY).hasValidLocation())
        assertFalse(schedule.copy(radiusMeters = 0).hasValidLocation())
        assertFalse(schedule.copy(locationEnabled = false).hasValidLocation())
    }

    @Test fun `daytime window starts inclusively and ends exclusively`() {
        assertFalse(LocationGate.inResumeWindowNow(schedule, time(Calendar.MONDAY, 8, 59)))
        assertTrue(LocationGate.inResumeWindowNow(schedule, time(Calendar.MONDAY, 9)))
        assertTrue(LocationGate.inResumeWindowNow(schedule, time(Calendar.MONDAY, 16, 59)))
        assertFalse(LocationGate.inResumeWindowNow(schedule, time(Calendar.MONDAY, 17)))
        assertFalse(LocationGate.inResumeWindowNow(schedule, time(Calendar.TUESDAY, 10)))
    }

    @Test fun `overnight window belongs to its starting weekday`() {
        val overnight = schedule.copy(enableHour = 22, disableHour = 6)
            .withDay(Calendar.TUESDAY, DaySchedule(true, 12 * 60, 18 * 60))
        assertTrue(LocationGate.inResumeWindowNow(overnight, time(Calendar.MONDAY, 23)))
        assertTrue(LocationGate.inResumeWindowNow(overnight, time(Calendar.TUESDAY, 5, 59)))
        assertFalse(LocationGate.inResumeWindowNow(overnight, time(Calendar.TUESDAY, 6)))
        assertFalse(LocationGate.inResumeWindowNow(overnight, time(Calendar.MONDAY, 5)))
    }

    @Test fun `overnight window wraps the end of the week`() {
        val saturday = schedule.copy(enableHour = 22, disableHour = 6, days = listOf(Calendar.SATURDAY))
            .withDay(Calendar.SUNDAY, DaySchedule(true, 12 * 60, 18 * 60))
        assertTrue(LocationGate.inResumeWindowNow(saturday, time(Calendar.SUNDAY, 1)))
    }

    @Test fun `empty or disabled schedule never authorizes location resume`() {
        assertFalse(LocationGate.inResumeWindowNow(schedule.copy(days = emptyList()), time(Calendar.MONDAY, 10)))
        assertFalse(LocationGate.inResumeWindowNow(schedule.copy(locationEnabled = false), time(Calendar.MONDAY, 10)))
        assertNotNull(schedule.copy(enableHour = 17).validationError())
    }
}
