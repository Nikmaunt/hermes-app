package app.hermes.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DedupKeysTest {

    @Test
    fun `identical transactions produce the same key`() {
        val a = DedupKeys.transaction("notification", 1_700_000_000_000, 4599, "PLN", "Biedronka")
        val b = DedupKeys.transaction("notification", 1_700_000_000_000, 4599, "PLN", "Biedronka")
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `null and blank merchant collapse to the same key`() {
        val nullMerchant = DedupKeys.transaction("manual", 1_700_000_000_000, 999, "EUR", null)
        val blankMerchant = DedupKeys.transaction("manual", 1_700_000_000_000, 999, "EUR", "   ")
        assertThat(nullMerchant).isEqualTo(blankMerchant)
    }

    @Test
    fun `different amount yields a different key`() {
        val a = DedupKeys.transaction("gmail", 1_700_000_000_000, 100, "USD", "Netflix")
        val b = DedupKeys.transaction("gmail", 1_700_000_000_000, 200, "USD", "Netflix")
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `merchant and currency are case and whitespace insensitive`() {
        val a = DedupKeys.transaction("manual", 1L, 500, "pln", " Zabka ")
        val b = DedupKeys.transaction("manual", 1L, 500, "PLN", "zabka")
        assertThat(a).isEqualTo(b)
    }
}
