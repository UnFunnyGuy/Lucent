package app.lucent.firefly.core.common.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Base ViewModel for Unidirectional Data Flow (UDF).
 *
 * @param State Immutable state of the UI.
 * @param Event One-time UI events/effects (Navigation, Snackbar).
 * @param Action User intents/actions.
 */
abstract class BaseViewModel<State : UIState, Event : UIEvent, Action : UIAction> : ViewModel() {

    abstract fun initialState(): State

    protected abstract fun onActionEvent(action: Action)

    private val _uiState by lazy { MutableStateFlow(initialState()) }
    val uiState: StateFlow<State> by lazy { _uiState.asStateFlow() }

    private val _uiEvent = Channel<Event>(capacity = Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    protected val currentState: State
        get() = uiState.value

    val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
        onException(context, throwable)
    }

    protected fun update(reducer: State.() -> State) {
        _uiState.update(reducer)
    }

    protected fun sendEvent(event: Event, delayMillis: Long? = null) {
        viewModelScope.launch {
            if (delayMillis != null && delayMillis > 0) {
                delay(delayMillis)
            }
            _uiEvent.send(event)
        }
    }

    fun onAction(action: Action) {
        onActionEvent(action)
    }

    fun viewmodelLaunch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        return viewModelScope.launch(
            context = context + exceptionHandler,
            start = start,
            block = block,
        )
    }

    open fun onException(coroutineContext: CoroutineContext, throwable: Throwable) = Unit
}
