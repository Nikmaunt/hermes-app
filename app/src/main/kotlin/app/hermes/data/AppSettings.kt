package app.hermes.data

/** How the app resolves light/dark. Persisted as [wire]; unknown → SYSTEM. */
enum class ThemeMode(val wire: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromWire(wire: String?): ThemeMode = entries.firstOrNull { it.wire == wire } ?: SYSTEM
    }
}

/** The chosen AI backend. Null until onboarding picks one. Persisted as [wire]. */
enum class ProviderChoice(val wire: String) {
    API_KEY("api_key"),
    ON_DEVICE("on_device"),
    DEMO("demo"),
    ;

    companion object {
        fun fromWire(wire: String?): ProviderChoice? = wire?.let { value -> entries.firstOrNull { it.wire == value } }
    }
}

/**
 * The non-secret app settings mirrored from DataStore. Secrets (API key, tokens) are
 * NEVER here — they live in the Keystore-backed store (M1). Only the non-secret part of
 * an endpoint URL and the preferred output language are kept.
 */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val provider: ProviderChoice? = null,
    val onboardingComplete: Boolean = false,
    val baseUrl: String? = null,
    /** Preferred output language (BrainContext.language); null = system locale. UI in M3. */
    val outputLanguage: String? = null,
)
