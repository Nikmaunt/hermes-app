package app.hermes.onboarding

import app.hermes.data.ProviderChoice

/**
 * Onboarding as a state machine, not a linear wizard. Three entries (API key /
 * on-device / Demo) resolve through a small set of states; the on-device path can
 * branch to an "unavailable" state that is NEVER a dead end (decision-log D34).
 *
 * The reducer is pure and total (an unexpected state/event pair is a no-op), so the
 * whole flow is unit-tested on the JVM without Android. Availability of on-device AI is
 * delivered AS AN EVENT ([OnboardingEvent.OnDeviceChecked]) rather than read inside the
 * reducer, keeping it pure and letting the real (async) ML Kit GenAI probe live in the
 * ViewModel (M1).
 */
sealed interface OnboardingState {
    data object PathSelection : OnboardingState

    data object OnDeviceChecking : OnboardingState

    data object OnDeviceUnavailable : OnboardingState

    /** Terminal: the caller persists [provider] + onboardingComplete here. */
    data class Ready(val provider: ProviderChoice) : OnboardingState
}

sealed interface OnboardingEvent {
    data object SelectApiKey : OnboardingEvent

    data object SelectDemo : OnboardingEvent

    data object SelectOnDevice : OnboardingEvent

    data class OnDeviceChecked(val status: OnDeviceStatus) : OnboardingEvent

    data object FallbackToApiKey : OnboardingEvent

    data object FallbackToDemo : OnboardingEvent

    data object Back : OnboardingEvent
}

enum class OnDeviceStatus { AVAILABLE, UNAVAILABLE }

/** The async on-device (ML Kit GenAI) availability probe. M0 reports UNAVAILABLE. */
fun interface OnDeviceAvailability {
    suspend fun status(): OnDeviceStatus
}

object OnboardingReducer {
    fun reduce(state: OnboardingState, event: OnboardingEvent): OnboardingState = when (state) {
        OnboardingState.PathSelection -> when (event) {
            OnboardingEvent.SelectApiKey -> OnboardingState.Ready(ProviderChoice.API_KEY)
            OnboardingEvent.SelectDemo -> OnboardingState.Ready(ProviderChoice.DEMO)
            OnboardingEvent.SelectOnDevice -> OnboardingState.OnDeviceChecking
            else -> state
        }

        OnboardingState.OnDeviceChecking -> when (event) {
            is OnboardingEvent.OnDeviceChecked ->
                if (event.status == OnDeviceStatus.AVAILABLE) {
                    OnboardingState.Ready(ProviderChoice.ON_DEVICE)
                } else {
                    OnboardingState.OnDeviceUnavailable
                }

            OnboardingEvent.Back -> OnboardingState.PathSelection
            else -> state
        }

        OnboardingState.OnDeviceUnavailable -> when (event) {
            OnboardingEvent.FallbackToApiKey -> OnboardingState.Ready(ProviderChoice.API_KEY)
            OnboardingEvent.FallbackToDemo -> OnboardingState.Ready(ProviderChoice.DEMO)
            OnboardingEvent.Back -> OnboardingState.PathSelection
            else -> state
        }

        // Terminal — the flow is done; ignore further events.
        is OnboardingState.Ready -> state
    }
}

/**
 * The provider to persist iff the flow has reached its terminal state; `null` means
 * "nothing to commit". A cancelled re-entry (opened from Settings, backed out before a
 * choice) never reaches [OnboardingState.Ready], so nothing is written and the existing
 * provider + onboardingComplete flag are left untouched (D34).
 */
val OnboardingState.committedProvider: ProviderChoice?
    get() = (this as? OnboardingState.Ready)?.provider
