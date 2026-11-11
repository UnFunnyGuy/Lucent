package app.lucent.firefly.feature.dashboard.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import app.lucent.firefly.core.navigation.EntryProviderInstaller
import app.lucent.firefly.core.navigation.Navigator
import app.lucent.firefly.feature.capture.api.CaptureNavKey
import app.lucent.firefly.feature.dashboard.api.DashboardNavKey
import app.lucent.firefly.feature.dashboard.impl.presentation.DashboardRoute
import org.koin.core.annotation.Singleton

@Singleton
class DashboardEntryProviderInstaller(
    private val navigator: Navigator,
) : EntryProviderInstaller {

    override fun install(scope: EntryProviderScope<NavKey>) {
        scope.entry<DashboardNavKey> {
            DashboardRoute(
                onNavigateToQuickAdd = { navigator.navigate(CaptureNavKey) },
                // Logout clears the session, which switches back to the auth graph.
                onNavigateToLogin = {},
            )
        }
    }
}
