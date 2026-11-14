package app.lucent.firefly.core.common.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Collects values from a [Flow] emitting UI events and delivers them to the [collector]
 * function only when the lifecycle is at least [lifeCycleState] (defaults to STARTED).
 *
 * Ensures one-time events (like navigation or messages) are not consumed while in background.
 */
@Composable
fun <T : UIEvent> ObserveAsEvents(
    eventFlow: Flow<T>,
    lifeCycleState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(eventFlow, lifecycleOwner.lifecycle) {
        lifecycleOwner.repeatOnLifecycle(lifeCycleState) {
            withContext(Dispatchers.Main.immediate) {
                eventFlow.collect(collector)
            }
        }
    }
}
