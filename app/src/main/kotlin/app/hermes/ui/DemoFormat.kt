package app.hermes.ui

import app.hermes.core.model.CalendarDates
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Display formatting for the demo read screens. Kept in one place so dates and money read
 * the same everywhere. Dates are formatted in English for M0 (a locale-aware format is a
 * later concern); money is `major.minor CODE`, per-currency, never a cross-currency total
 * (D13).
 */
object DemoFormat {

    private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)

    /** A followup's due: a timed value shows the clock in its zone; a date-only value the day. */
    fun dueAt(millis: Long, dateOnly: Boolean, zoneId: String?): String =
        if (dateOnly) dateOnly(millis) else DATE_TIME.format(Instant.ofEpochMilli(millis).atZone(zone(zoneId)))

    /** A date-only (UTC-midnight) millis rendered as its calendar day. */
    fun dateOnly(millis: Long): String = DATE.format(CalendarDates.toLocalDate(millis))

    /** A true instant rendered in [zoneId] (system zone when null). */
    fun instant(millis: Long, zoneId: String? = null): String =
        DATE_TIME.format(Instant.ofEpochMilli(millis).atZone(zone(zoneId)))

    fun money(amountMinor: Long, currency: String): String =
        "%.2f %s".format(Locale.ENGLISH, amountMinor / MINOR_PER_MAJOR, currency)

    private fun zone(zoneId: String?): ZoneId = zoneId?.let(ZoneId::of) ?: ZoneId.systemDefault()

    private const val MINOR_PER_MAJOR = 100.0
}
