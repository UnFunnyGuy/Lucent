package app.lucent.firefly.core.common.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun <S : UIState, VM : BaseViewModel<S, *, *>> VM.collectState(): State<S> =
    uiState.collectAsStateWithLifecycle()
