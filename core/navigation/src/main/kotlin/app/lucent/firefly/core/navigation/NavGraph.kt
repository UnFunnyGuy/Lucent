package app.lucent.firefly.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * A group of destinations entered through [startDestination]. Switching graphs replaces the
 * whole back stack, so destinations from the previous graph cannot be reached with back.
 */
interface NavGraph {
    val startDestination: NavKey
}
