package app.hermes.onboarding

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * M0 stand-in: the ML Kit GenAI (Gemini Nano) probe is wired in M1. Until then on-device
 * AI is reported UNAVAILABLE, so choosing it in onboarding routes to the (non-locking)
 * fallback branch — exactly the path we want exercised early.
 */
class OnDeviceAvailabilityM0 @Inject constructor() : OnDeviceAvailability {
    override suspend fun status(): OnDeviceStatus = OnDeviceStatus.UNAVAILABLE
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {
    @Binds
    abstract fun bindOnDeviceAvailability(impl: OnDeviceAvailabilityM0): OnDeviceAvailability
}
