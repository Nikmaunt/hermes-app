package app.hermes.core.model

/**
 * Money is always an integer amount in the currency's minor unit (cents, grosze)
 * plus an ISO-4217 code. There is deliberately NO cross-currency total anywhere:
 * aggregates are computed per currency (see [sumByCurrency]).
 *
 * Absence of an amount is represented by a `null` Money — never by `Money(0, …)`.
 * A zero amount is a real amount; "no amount recorded" is null. Keeping the two
 * distinct is what stops per-currency totals from silently gaining phantom rows.
 */
data class Money(val amountMinor: Long, val currency: String) {
    init {
        require(currency.length == 3) { "currency must be an ISO-4217 alpha-3 code: '$currency'" }
    }
}

/**
 * Sum monies grouped by currency. `null` entries (no amount) are skipped — a null
 * is NOT coerced to zero, so a currency represented only by nulls never appears in
 * the result. Insertion order of first appearance is preserved.
 */
fun Iterable<Money?>.sumByCurrency(): Map<String, Long> {
    val totals = LinkedHashMap<String, Long>()
    for (money in this) {
        if (money == null) continue
        totals[money.currency] = (totals[money.currency] ?: 0L) + money.amountMinor
    }
    return totals
}
