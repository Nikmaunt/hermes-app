package app.hermes.onboarding

import app.hermes.data.ProviderChoice
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OnboardingStateMachineTest {

    private fun reduce(state: OnboardingState, event: OnboardingEvent) = OnboardingReducer.reduce(state, event)

    @Test
    fun `api key path reaches Ready(API_KEY)`() {
        val end = reduce(OnboardingState.PathSelection, OnboardingEvent.SelectApiKey)
        assertThat(end).isEqualTo(OnboardingState.Ready(ProviderChoice.API_KEY))
        assertThat(end.committedProvider).isEqualTo(ProviderChoice.API_KEY)
    }

    @Test
    fun `demo path reaches Ready(DEMO)`() {
        val end = reduce(OnboardingState.PathSelection, OnboardingEvent.SelectDemo)
        assertThat(end).isEqualTo(OnboardingState.Ready(ProviderChoice.DEMO))
    }

    @Test
    fun `on-device select goes to checking, then available reaches Ready(ON_DEVICE)`() {
        val checking = reduce(OnboardingState.PathSelection, OnboardingEvent.SelectOnDevice)
        assertThat(checking).isEqualTo(OnboardingState.OnDeviceChecking)
        val end = reduce(checking, OnboardingEvent.OnDeviceChecked(OnDeviceStatus.AVAILABLE))
        assertThat(end).isEqualTo(OnboardingState.Ready(ProviderChoice.ON_DEVICE))
    }

    @Test
    fun `on-device unavailable is not terminal and not a dead end`() {
        val checking = reduce(OnboardingState.PathSelection, OnboardingEvent.SelectOnDevice)
        val unavailable = reduce(checking, OnboardingEvent.OnDeviceChecked(OnDeviceStatus.UNAVAILABLE))
        assertThat(unavailable).isEqualTo(OnboardingState.OnDeviceUnavailable)
        assertThat(unavailable.committedProvider).isNull()
        // three exits, none of them a lock
        assertThat(reduce(unavailable, OnboardingEvent.FallbackToApiKey))
            .isEqualTo(OnboardingState.Ready(ProviderChoice.API_KEY))
        assertThat(reduce(unavailable, OnboardingEvent.FallbackToDemo))
            .isEqualTo(OnboardingState.Ready(ProviderChoice.DEMO))
        assertThat(reduce(unavailable, OnboardingEvent.Back)).isEqualTo(OnboardingState.PathSelection)
    }

    @Test
    fun `Ready is terminal`() {
        val ready = OnboardingState.Ready(ProviderChoice.DEMO)
        assertThat(reduce(ready, OnboardingEvent.SelectApiKey)).isEqualTo(ready)
        assertThat(reduce(ready, OnboardingEvent.Back)).isEqualTo(ready)
    }

    @Test
    fun `committedProvider is null until Ready`() {
        assertThat(OnboardingState.PathSelection.committedProvider).isNull()
        assertThat(OnboardingState.OnDeviceChecking.committedProvider).isNull()
        assertThat(OnboardingState.OnDeviceUnavailable.committedProvider).isNull()
    }

    @Test
    fun `re-entry cancelled midway commits nothing and preserves the prior provider`() {
        // Opened again from Settings (the user already has a provider); they poke around
        // the on-device branch and then back all the way out without choosing.
        var state: OnboardingState = OnboardingState.PathSelection
        state = reduce(state, OnboardingEvent.SelectOnDevice)
        state = reduce(state, OnboardingEvent.OnDeviceChecked(OnDeviceStatus.UNAVAILABLE))
        state = reduce(state, OnboardingEvent.Back)
        assertThat(state).isEqualTo(OnboardingState.PathSelection)
        // Back at the selection screen is a no-op in the reducer (nav handles leaving).
        state = reduce(state, OnboardingEvent.Back)
        assertThat(state).isEqualTo(OnboardingState.PathSelection)
        // Nothing terminal was ever reached → nothing to persist → provider untouched.
        assertThat(state.committedProvider).isNull()
    }
}
