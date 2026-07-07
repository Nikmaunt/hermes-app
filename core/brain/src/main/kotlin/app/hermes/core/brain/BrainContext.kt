package app.hermes.core.brain

/**
 * Ambient context injected into every brain call so the module stays deterministic and
 * unit-testable: nothing inside :core:brain reads the wall clock or the device zone
 * (D22). The caller supplies "now" and the zone; the model resolves relative dates
 * ("tomorrow", "next Friday") against them.
 */
data class BrainContext(
    /** "Now" as UTC epoch-millis; anchors the model's relative-date resolution. */
    val nowMillis: Long,
    /** IANA zone id the user is in (e.g. "Europe/Warsaw"); date-only dues are
     *  canonicalized against it (see CalendarDates, D6). */
    val zoneId: String,
    /**
     * Preferred OUTPUT language for generated prose (summary, brief, chat reply),
     * e.g. "en" / "ru". This is NOT the note's language — the model detects the input
     * language itself; we never language-detect in Kotlin. Null = let the model decide.
     */
    val language: String? = null,
)
