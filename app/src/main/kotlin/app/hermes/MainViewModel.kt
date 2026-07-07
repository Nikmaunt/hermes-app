package app.hermes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hermes.data.AppSettings
import app.hermes.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Holds the settings the activity needs before it can draw: theme (light/dark) and the
 * onboarding gate. Emits null until the first DataStore read completes, so the activity
 * can hold the splash background instead of flashing the wrong theme or start screen.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    repository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
