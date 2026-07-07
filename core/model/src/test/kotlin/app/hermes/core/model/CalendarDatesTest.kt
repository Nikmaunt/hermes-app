package app.hermes.core.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test

class CalendarDatesTest {

    @Test
    fun `same calendar day at different instants normalizes to one date-only value`() {
        val warsaw = ZoneId.of("Europe/Warsaw")
        val date = LocalDate.of(2026, 7, 15)
        val expected = CalendarDates.toDateMillis(date)

        val morning = date.atTime(8, 0).atZone(warsaw).toInstant().toEpochMilli()
        val evening = date.atTime(23, 30).atZone(warsaw).toInstant().toEpochMilli()

        assertThat(CalendarDates.toDateMillis(morning, warsaw)).isEqualTo(expected)
        assertThat(CalendarDates.toDateMillis(evening, warsaw)).isEqualTo(expected)
    }

    @Test
    fun `date-only value round-trips through LocalDate`() {
        val date = LocalDate.of(2026, 1, 1)
        assertThat(CalendarDates.toLocalDate(CalendarDates.toDateMillis(date))).isEqualTo(date)
    }

    @Test
    fun `daysBetween is calendar-correct`() {
        val a = CalendarDates.toDateMillis(LocalDate.of(2026, 7, 1))
        val b = CalendarDates.toDateMillis(LocalDate.of(2026, 7, 31))
        assertThat(CalendarDates.daysBetween(a, b)).isEqualTo(30)
    }

    @Test
    fun `zone decides which calendar day a late-night instant belongs to`() {
        // 23:30 in Warsaw on the 15th is still the 14th in UTC — the zone must decide.
        val warsaw = ZoneId.of("Europe/Warsaw")
        val lateNight = LocalDate.of(2026, 7, 15).atTime(23, 30).atZone(warsaw).toInstant().toEpochMilli()
        assertThat(CalendarDates.toDateMillis(lateNight, warsaw))
            .isEqualTo(CalendarDates.toDateMillis(LocalDate.of(2026, 7, 15)))
        assertThat(CalendarDates.toDateMillis(lateNight, ZoneId.of("UTC")))
            .isEqualTo(CalendarDates.toDateMillis(LocalDate.of(2026, 7, 15)))
    }
}
