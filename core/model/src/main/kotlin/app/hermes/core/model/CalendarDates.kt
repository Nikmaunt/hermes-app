package app.hermes.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The single normalization point for every calendar (date-only) column in the DB:
 * `documents.renewsOn`, `documents.cancelBy`, `habit_ticks.tickDate`, `briefs.forDate`.
 *
 * Convention (see decision-log): a date-only value is stored as the epoch-millis of
 * that calendar date at 00:00 **UTC**. Every writer MUST go through this object, so
 * the "date-only as UTC-midnight millis" representation cannot drift between writers
 * — that drift is exactly what would break `UNIQUE(habitId, tickDate)` (two ticks in
 * one day) and the 30-day deadline math.
 *
 * Instants (true timestamps like createdAt / dueAt) are NOT calendar dates and do not
 * go through here; they are stored as their own UTC epoch-millis.
 */
object CalendarDates {

    private val UTC: ZoneId = ZoneOffset.UTC

    /** Date-only millis (00:00 UTC) for an explicit calendar date, e.g. a parsed "2026-07-15". */
    fun toDateMillis(date: LocalDate): Long = date.atStartOfDay(UTC).toInstant().toEpochMilli()

    /**
     * Date-only millis for the calendar day an [instantMillis] falls on **in [zone]**.
     * Use this when turning "now" (a device instant) into a tick/deadline date: the
     * day is decided in the user's zone, then canonicalized to UTC midnight.
     */
    fun toDateMillis(instantMillis: Long, zone: ZoneId): Long =
        toDateMillis(Instant.ofEpochMilli(instantMillis).atZone(zone).toLocalDate())

    /** Today's date-only millis in [zone]. */
    fun today(zone: ZoneId): Long = toDateMillis(LocalDate.now(zone))

    /** Inverse of [toDateMillis]: the calendar date a date-only millis encodes. */
    fun toLocalDate(dateMillis: Long): LocalDate = Instant.ofEpochMilli(dateMillis).atZone(UTC).toLocalDate()

    /** Whole days between two date-only millis (b - a), calendar-correct. */
    fun daysBetween(fromDateMillis: Long, toDateMillis: Long): Long =
        toLocalDate(fromDateMillis).toEpochDay().let { from ->
            toLocalDate(toDateMillis).toEpochDay() - from
        }
}
