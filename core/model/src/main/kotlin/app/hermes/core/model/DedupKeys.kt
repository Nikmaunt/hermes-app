package app.hermes.core.model

import java.security.MessageDigest
import java.util.Locale

/**
 * Idempotency keys for extraction/import. Extraction is re-runnable (process killed
 * after the LLM call but before the write → retry) and notifications/Gmail arrive
 * more than once by nature, so a `TEXT id` PK alone does not stop a *second row with
 * the same meaning*. These stable keys give the generating tables a natural dedup
 * hook — see decision-log ("extraction is idempotent per note").
 */
object DedupKeys {

    /**
     * Stable key for an extracted/imported transaction, used behind
     * `UNIQUE(dedupKey)`. A null merchant is normalized to empty — unlike a raw
     * `UNIQUE(source, occurredAt, amountMinor, merchant)` where SQL NULLs compare
     * unequal and would let duplicates slip through.
     */
    fun transaction(source: String, occurredAt: Long, amountMinor: Long, currency: String, merchant: String?): String =
        sha256Hex(
            listOf(
                source.trim().lowercase(Locale.ROOT),
                occurredAt.toString(),
                amountMinor.toString(),
                currency.trim().uppercase(Locale.ROOT),
                (merchant ?: "").trim().lowercase(Locale.ROOT),
            ).joinToString("|"),
        )

    private fun sha256Hex(input: String): String = MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}
