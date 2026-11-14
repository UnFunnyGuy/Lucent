package app.lucent.firefly.navigation

import androidx.navigation3.runtime.NavKey
import app.lucent.firefly.core.navigation.NavGraph
import app.lucent.firefly.feature.dashboard.api.DashboardNavKey
import app.lucent.firefly.feature.onboarding.api.OnboardingNavKey

sealed interface LucentNavGraph : NavGraph {

    data object Auth : LucentNavGraph {
        override val startDestination: NavKey = OnboardingNavKey
    }

    data object Home : LucentNavGraph {
        override val startDestination: NavKey = DashboardNavKey
    }
}
