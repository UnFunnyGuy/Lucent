package app.lucent.firefly.feature.onboarding.impl.presentation

import app.lucent.firefly.feature.onboarding.impl.presentation.viewmodel.OnboardingViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.lucent.firefly.core.common.base.SingleEventEffect
import app.lucent.firefly.core.common.base.collectState
import app.lucent.firefly.core.designsystem.theme.LucentTheme
import app.lucent.firefly.core.ui.component.ScreenWrapper
import app.lucent.firefly.feature.onboarding.impl.presentation.viewmodel.OnboardingUiAction
import app.lucent.firefly.feature.onboarding.impl.presentation.viewmodel.OnboardingUiEvent
import app.lucent.firefly.feature.onboarding.impl.presentation.viewmodel.OnboardingUiState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful entry route for the Onboarding destination.
 */
@Composable
fun OnboardingRoute(
    viewModel: OnboardingViewModel = koinViewModel(),
    onNavigateToDashboard: () -> Unit,
) {
    val state by viewModel.collectState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    SingleEventEffect(viewModel.uiEvent) { event ->
        when (event) {
            is OnboardingUiEvent.NavigateToDashboard -> onNavigateToDashboard()
            is OnboardingUiEvent.ShowSnackbar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    OnboardingScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * Stateless screen layout for connecting to a Firefly III instance.
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onAction: (OnboardingUiAction) -> Unit,
    snackbarHostState: SnackbarHostState? = null,
) {
    val focusManager = LocalFocusManager.current

    ScreenWrapper(
        snackbarHostState = snackbarHostState,
        scrollable = true,
        applyImePadding = true,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Welcome to Lucent",
            style = MaterialTheme.typography.headlineMedium,
            color = LucentTheme.colors.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connect to your Firefly III server",
            style = MaterialTheme.typography.bodyMedium,
            color = LucentTheme.colors.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Server URL input
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = { onAction(OnboardingUiAction.UpdateServerUrl(it)) },
            label = { Text("Server URL") },
            placeholder = { Text("https://firefly.example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Self-signed certificate toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.allowSelfSignedCert,
                onCheckedChange = { onAction(OnboardingUiAction.UpdateAllowSelfSignedCert(it)) },
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Trust self-signed / homelab TLS certificate",
                style = MaterialTheme.typography.bodySmall,
                color = LucentTheme.colors.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Personal Access Token input
        OutlinedTextField(
            value = state.token,
            onValueChange = { onAction(OnboardingUiAction.UpdateToken(it)) },
            label = { Text("Personal Access Token (PAT)") },
            placeholder = { Text("Paste your Firefly III token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onAction(OnboardingUiAction.Connect)
                },
            ),
        )


        // Error message display
        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.errorMessage,
                color = LucentTheme.colors.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = LucentTheme.colors.primary,
            )
        } else {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onAction(OnboardingUiAction.Connect)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Connect")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    LucentTheme {
        OnboardingScreen(
            state = OnboardingUiState(
                serverUrl = "https://demo.firefly-iii.org",
            ),
            onAction = {},
        )
    }
}
