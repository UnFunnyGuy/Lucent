package app.lucent.firefly.presentation.viewmodel

import app.lucent.firefly.core.common.base.BaseViewModel
import app.lucent.firefly.core.navigation.Navigator
import app.lucent.firefly.data.auth.api.usecase.GetActiveSessionUseCase
import app.lucent.firefly.navigation.LucentNavGraph
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MainViewModel(
    private val getActiveSessionUseCase: GetActiveSessionUseCase,
    private val navigator: Navigator,
) : BaseViewModel<MainUiState, MainUiEvent, MainUiAction>() {
    override fun initialState(): MainUiState = MainUiState()

    init {
        observeSession()
    }

    override fun onActionEvent(action: MainUiAction) = Unit

    private fun observeSession() {
        viewmodelLaunch {
            getActiveSessionUseCase()
                .map { session -> if (session != null) LucentNavGraph.Home else LucentNavGraph.Auth }
                .distinctUntilChanged()
                .collect { graph ->
                    navigator.setGraph(graph)
                    update { copy(isSessionResolved = true) }
                }
        }
    }
}
