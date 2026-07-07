package app.hermes.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoneyTest {

    @Test
    fun `sumByCurrency keeps currencies separate`() {
        val monies = listOf(
            Money(1000, "PLN"),
            Money(250, "EUR"),
            Money(500, "PLN"),
        )
        assertThat(monies.sumByCurrency()).containsExactly("PLN", 1500L, "EUR", 250L)
    }

    @Test
    fun `null amount is skipped, not treated as zero`() {
        // A currency represented only by nulls must NOT appear in the totals.
        val monies = listOf<Money?>(null, Money(0, "USD"), null)
        val totals = monies.sumByCurrency()
        assertThat(totals).containsExactly("USD", 0L) // the real zero survives…
        assertThat(monies.filterNotNull()).hasSize(1) // …but the two nulls contributed nothing
    }

    @Test
    fun `zero is a real amount distinct from absence`() {
        val zero: Money? = Money(0, "PLN")
        val absent: Money? = null
        assertThat(zero).isNotEqualTo(absent)
        assertThat(listOf(zero, absent).sumByCurrency()).containsExactly("PLN", 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects non ISO-4217 currency`() {
        Money(100, "ZLOTY")
    }
}
