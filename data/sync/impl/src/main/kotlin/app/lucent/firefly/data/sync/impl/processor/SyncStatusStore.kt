package app.lucent.firefly.data.sync.impl.processor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Singleton

/** Process-wide sync flags that do not belong in the database. */
@Singleton
class SyncStatusStore {
    private val reauthenticationRequired = MutableStateFlow(false)
    val requiresReauthentication: StateFlow<Boolean> = reauthenticationRequired.asStateFlow()

    fun markReauthenticationRequired() {
        reauthenticationRequired.value = true
    }

    fun clearReauthenticationRequired() {
        reauthenticationRequired.value = false
    }
}
