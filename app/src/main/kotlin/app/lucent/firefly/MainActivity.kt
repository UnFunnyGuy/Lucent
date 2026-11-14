package app.lucent.firefly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.lucent.firefly.core.common.base.collectState
import app.lucent.firefly.core.designsystem.theme.LucentTheme
import app.lucent.firefly.core.navigation.EntryProviderInstaller
import app.lucent.firefly.core.navigation.Navigator
import app.lucent.firefly.di.EntryProviderInstallers
import app.lucent.firefly.navigation.LucentNavDisplay
import app.lucent.firefly.presentation.viewmodel.MainUiState
import app.lucent.firefly.presentation.viewmodel.MainViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val navigator: Navigator by inject()

    private val entryProviders: EntryProviderInstallers by inject()

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LucentTheme {
                val state by viewModel.collectState()
                LucentApp(
                    state = state,
                    navigator = navigator,
                    entryProviderInstallers = entryProviders.installers,
                )
            }
        }
    }
}

@Composable
private fun LucentApp(
    state: MainUiState,
    navigator: Navigator,
    entryProviderInstallers: List<EntryProviderInstaller>,
) {
    if (state.isSessionResolved) {
        LucentNavDisplay(
            navigator = navigator,
            entryProviderInstallers = entryProviderInstallers,
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = LucentTheme.colors.primary)
        }
    }
}
