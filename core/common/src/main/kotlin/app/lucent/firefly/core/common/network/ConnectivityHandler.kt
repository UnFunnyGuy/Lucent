package app.lucent.firefly.core.common.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Exposes device network connectivity as an observable [StateFlow] and synchronous snapshot.
 */
interface ConnectivityHandler {

    /** Hot flow of connectivity status. */
    val isConnected: StateFlow<Boolean>

    /** Synchronous snapshot — returns true if internet capability is present. */
    fun isCurrentlyConnected(): Boolean

    /** Returns true if connection has both INTERNET and VALIDATED capabilities. */
    fun hasValidatedInternet(): Boolean
}
