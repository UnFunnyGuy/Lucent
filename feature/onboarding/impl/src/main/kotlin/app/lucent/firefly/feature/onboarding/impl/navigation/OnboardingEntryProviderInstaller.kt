package app.lucent.firefly.feature.onboarding.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import app.lucent.firefly.core.navigation.EntryProviderInstaller
import app.lucent.firefly.feature.onboarding.api.OnboardingNavKey
import app.lucent.firefly.feature.onboarding.impl.presentation.OnboardingRoute
import org.koin.core.annotation.Singleton

@Singleton
class OnboardingEntryProviderInstaller : EntryProviderInstaller {
    override fun install(scope: EntryProviderScope<NavKey>) {
        scope.entry<OnboardingNavKey> {
            // Leaving the auth graph is driven by the active session, not by this screen.
            OnboardingRoute(onNavigateToDashboard = {})
        }
    }
}
