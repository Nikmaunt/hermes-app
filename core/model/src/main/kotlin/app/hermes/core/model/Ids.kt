package app.hermes.core.model

import java.util.UUID

/** App-generated stable row ids: a short type prefix + a compact random suffix. */
object Ids {
    fun new(prefix: String): String = prefix + "-" + UUID.randomUUID().toString().replace("-", "").take(SUFFIX_LEN)

    private const val SUFFIX_LEN = 20
}
