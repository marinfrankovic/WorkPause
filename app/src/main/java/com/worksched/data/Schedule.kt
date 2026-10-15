package com.worksched.data

// Modified for WorkPause: independent daily hours, overnight windows, and validated location gating.
import java.util.Calendar
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.Serializable

@Serializable
enum class DayMode { SCHEDULE, ALL_ON, ALL_OFF }

@Serializable
data class DaySchedule(
    val enabled: Boolean = true,
    val resumeMinutes: Int = 8 * 60,
    val pauseMinutes: Int = 18 * 60,
    val mode: DayMode = if (enabled) DayMode.SCHEDULE else DayMode.ALL_OFF
) {
    fun effectiveMode(): DayMode = if (enabled) mode else DayMode.ALL_OFF

    fun hasValidTimes(): Boolean =
        resumeMinutes in 0..1439 && pauseMinutes in 0..1439 && resumeMinutes != pauseMinutes

    fun isOvernight(): Boolean = pauseMinutes < resumeMinutes
}

data class ScheduleTransition(val atMillis: Long, val resume: Boolean)

/**
 * Independent work intervals keyed by Calendar weekday (Sunday=1 .. Saturday=7).
 * Legacy shared hours and [days] remain a fallback for saved schedules without overrides.
 */
@Serializable
data class Schedule(
    val enabled: Boolean = true,
    val daySchedules: Map<Int, DaySchedule> = emptyMap(),
    val enableHour: Int = 10,
    val enableMinute: Int = 0,
    val disableHour: Int = 19,
    val disableMinute: Int = 0,
    val days: List<Int> = DEFAULT_DAYS,
    // Optional location gate on the *resume* only. Defaults keep it off, so older saved
    // JSON without these fields decodes to "no location gating" — behaviour unchanged.
    // lat/lng are nullable (not NaN) so the schedule stays valid JSON.
    val locationEnabled: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int = 150,
    // Human-readable address for the saved location (display only; never gates anything).
    // Resolved via the system Geocoder; null when offline / unavailable → UI shows coordinates.
    val locationLabel: String? = null
) {
    fun enableMinutesOfDay(): Int = enableHour * 60 + enableMinute
    fun disableMinutesOfDay(): Int = disableHour * 60 + disableMinute

    fun forDay(day: Int): DaySchedule {
        require(day in ALL_DAYS) { "Invalid weekday: $day" }
        return daySchedules[day] ?: DaySchedule(day in days, enableMinutesOfDay(), disableMinutesOfDay())
    }

    fun withDay(day: Int, value: DaySchedule): Schedule {
        require(day in ALL_DAYS) { "Invalid weekday: $day" }
        return copy(daySchedules = daySchedules + (day to value))
    }

    fun copyDayTo(source: Int, targets: Set<Int>): Schedule {
        require(targets.all { it in ALL_DAYS }) { "Invalid target weekday" }
        return copy(daySchedules = daySchedules + targets.associateWith { forDay(source) })
    }

    fun validationError(): String? {
        if (daySchedules.keys.any { it !in ALL_DAYS }) return "Schedule contains an invalid weekday."
        val invalidDay = ORDER.firstOrNull {
            forDay(it).let { day -> day.effectiveMode() == DayMode.SCHEDULE && !day.hasValidTimes() }
        }
        return invalidDay?.let { "${LONG[it]}: choose different resume and pause times." }
    }

    private fun windowsAround(atMillis: Long, zone: ZoneId): List<Pair<Long, Long>> {
        if (!enabled) return emptyList()
        require(validationError() == null) { requireNotNull(validationError()) }
        val date = Instant.ofEpochMilli(atMillis).atZone(zone).toLocalDate()
        return (-1L..8L).mapNotNull { offset ->
            val startDate = date.plusDays(offset)
            val day = forDay(startDate.dayOfWeek.value % 7 + 1)
            when (day.effectiveMode()) {
                DayMode.ALL_OFF -> return@mapNotNull null
                DayMode.ALL_ON -> return@mapNotNull startDate.atStartOfDay(zone).toInstant().toEpochMilli() to
                    startDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                DayMode.SCHEDULE -> Unit
            }
            val nextDate = startDate.plusDays(1)
            val nextDayOff = forDay(nextDate.dayOfWeek.value % 7 + 1).effectiveMode() == DayMode.ALL_OFF
            val endDate = if (day.isOvernight()) nextDate else startDate
            val start = startDate.atTime(day.resumeMinutes / 60, day.resumeMinutes % 60).atZone(zone).toInstant().toEpochMilli()
            val end = if (day.isOvernight() && nextDayOff) nextDate.atStartOfDay(zone).toInstant().toEpochMilli()
                else endDate.atTime(day.pauseMinutes / 60, day.pauseMinutes % 60).atZone(zone).toInstant().toEpochMilli()
            start to end
        }
    }

    fun isActiveAt(atMillis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Boolean =
        windowsAround(atMillis, zone).any { (start, end) -> atMillis >= start && atMillis < end }

    fun nextTransition(afterMillis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): ScheduleTransition? {
        val boundaries = windowsAround(afterMillis, zone).flatMap { listOf(it.first, it.second) }
            .filter { it > afterMillis }.distinct().sorted()
        return boundaries.firstOrNull { isActiveAt(it - 1, zone) != isActiveAt(it, zone) }
            ?.let { ScheduleTransition(it, isActiveAt(it, zone)) }
    }

    fun activeWindowEndMillis(atMillis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (!isActiveAt(atMillis, zone)) return null
        // A continuously ON week has no OFF transition; bound arrival monitoring to one week.
        return nextTransition(atMillis, zone)?.atMillis
            ?: Instant.ofEpochMilli(atMillis).atZone(zone).plusWeeks(1).toInstant().toEpochMilli()
    }

    /** True only when gating is on AND a usable coordinate is stored. Gating applies only then. */
    fun hasValidLocation(): Boolean =
        locationEnabled && latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0 && radiusMeters > 0

    /** Human-readable day summary, e.g. "Mon–Fri", "Every day", "Mon, Wed, Sat", "No days". */
    fun daysLabel(): String {
        val set = days.toSet()
        if (set.isEmpty()) return "no days"
        if (set == ALL_DAYS.toSet()) return "every day"
        if (set == DEFAULT_DAYS.toSet()) return "Mon–Fri"
        if (set == WEEKEND.toSet()) return "Sat–Sun"
        // Otherwise list short names in week order (Mon-first).
        return ORDER.filter { it in set }.joinToString(", ") { SHORT[it] ?: "?" }
    }

    companion object {
        fun default() = Schedule(daySchedules = ORDER.associateWith { DaySchedule(enabled = it in DEFAULT_DAYS) })

        val DEFAULT_DAYS = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY
        )
        val WEEKEND = listOf(Calendar.SATURDAY, Calendar.SUNDAY)
        val ALL_DAYS = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )

        /** Display order, Monday-first. */
        val ORDER = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )
        val SHORT = mapOf(
            Calendar.MONDAY to "Mon", Calendar.TUESDAY to "Tue", Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu", Calendar.FRIDAY to "Fri", Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )
        val LONG = mapOf(
            Calendar.MONDAY to "Monday", Calendar.TUESDAY to "Tuesday", Calendar.WEDNESDAY to "Wednesday",
            Calendar.THURSDAY to "Thursday", Calendar.FRIDAY to "Friday", Calendar.SATURDAY to "Saturday",
            Calendar.SUNDAY to "Sunday"
        )
    }
}
