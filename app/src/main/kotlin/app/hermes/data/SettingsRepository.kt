package app.hermes.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Non-secret app settings on Preferences DataStore. Enums are stored as their wire value
 * and an unknown value degrades to the default on read (same spirit as the DB enum
 * policy). Secrets never touch this store (D35).
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            theme = ThemeMode.fromWire(prefs[Keys.THEME]),
            provider = ProviderChoice.fromWire(prefs[Keys.PROVIDER]),
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
            baseUrl = prefs[Keys.BASE_URL],
            outputLanguage = prefs[Keys.OUTPUT_LANGUAGE],
        )
    }

    suspend fun setTheme(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.wire }
    }

    suspend fun setBaseUrl(url: String?) {
        dataStore.edit { prefs ->
            if (url.isNullOrBlank()) prefs.remove(Keys.BASE_URL) else prefs[Keys.BASE_URL] = url
        }
    }

    suspend fun setOutputLanguage(language: String?) {
        dataStore.edit { prefs ->
            if (language.isNullOrBlank()) prefs.remove(Keys.OUTPUT_LANGUAGE) else prefs[Keys.OUTPUT_LANGUAGE] = language
        }
    }

    /**
     * The single terminal onboarding write: records the chosen provider and flips the
     * flag ON. Idempotent — first run and a later provider change from Settings share this
     * path, and `onboarding_complete` is only ever set true here (never reset), so a
     * cancelled re-entry that never calls this leaves both values intact (D34).
     */
    suspend fun completeOnboarding(provider: ProviderChoice) {
        dataStore.edit {
            it[Keys.PROVIDER] = provider.wire
            it[Keys.ONBOARDING_COMPLETE] = true
        }
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val PROVIDER = stringPreferencesKey("provider")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val BASE_URL = stringPreferencesKey("base_url")
        val OUTPUT_LANGUAGE = stringPreferencesKey("output_language")
    }
}
