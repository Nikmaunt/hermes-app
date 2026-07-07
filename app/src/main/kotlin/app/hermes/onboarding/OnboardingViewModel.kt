package app.hermes.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hermes.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the pure [OnboardingReducer] and performs its two side effects: running the
 * async on-device availability probe when the flow enters checking, and persisting the
 * chosen provider when it reaches [OnboardingState.Ready]. Persisting is the ONLY write —
 * so a cancelled re-entry never mutates settings (D34).
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val availability: OnDeviceAvailability,
) : ViewModel() {

    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.PathSelection)
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun dispatch(event: OnboardingEvent) {
        val next = OnboardingReducer.reduce(_state.value, event)
        _state.value = next
        when (next) {
            OnboardingState.OnDeviceChecking -> viewModelScope.launch {
                dispatch(OnboardingEvent.OnDeviceChecked(availability.status()))
            }

            is OnboardingState.Ready -> viewModelScope.launch {
                settings.completeOnboarding(next.provider)
            }

            else -> Unit
        }
    }
}
