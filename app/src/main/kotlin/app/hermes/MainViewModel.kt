package app.hermes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hermes.data.AppSettings
import app.hermes.data.ProviderChoice
import app.hermes.data.SettingsRepository
import app.hermes.demo.DemoSeeder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the settings the activity needs before it can draw: theme (light/dark) and the
 * onboarding gate. Emits null until the first DataStore read completes, so the activity
 * can hold the splash background instead of flashing the wrong theme or start screen.
 *
 * It also arms the Demo seed: once onboarding is complete, if the chosen provider is Demo
 * the (idempotent) [DemoSeeder] fills the database with pipeline-produced rows so the
 * screens are not empty in Demo. A non-Demo provider seeds nothing.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    repository: SettingsRepository,
    seeder: DemoSeeder,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = null,
    )

    init {
        viewModelScope.launch {
            // Suspends until onboarding is finished, then seeds once iff the pick was Demo.
            val ready = repository.settings.first { it.onboardingComplete }
            if (ready.provider == ProviderChoice.DEMO) seeder.seedIfEmpty()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
