package app.lucent.firefly.core.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.lucent.firefly.core.designsystem.theme.LucentTheme

/**
 * Standard screen layout wrapper used across Lucent feature screens.
 *
 * Encapsulates the Material 3 [Scaffold], status/navigation bar insets, Lucent
 * design tokens (colors & dimensions), and optional scrolling/IME keyboard padding.
 *
 * All optional slots ([topBar], [bottomBar], [floatingActionButton], [snackbarHost])
 * default to `null` to avoid unnecessary empty composition passes.
 *
 * @param modifier Modifier applied to the outer [Scaffold].
 * @param topBar Optional top app bar slot.
 * @param bottomBar Optional bottom navigation / action bar slot.
 * @param floatingActionButton Optional floating action button slot.
 * @param floatingActionButtonPosition Position of the FAB (defaults to [FabPosition.End]).
 * @param snackbarHostState Optional [SnackbarHostState] to automatically render a [SnackbarHost].
 * @param snackbarHost Optional custom snackbar host slot (takes precedence over [snackbarHostState]).
 * @param verticalArrangement Vertical arrangement for the inner [Column] (defaults to [Arrangement.Center]).
 * @param horizontalAlignment Horizontal alignment for the inner [Column] (defaults to [Alignment.CenterHorizontally]).
 * @param applySystemBarsPadding Whether to apply window insets padding to the outer scaffold and inner column.
 * @param scrollable Whether to enable vertical scrolling on the inner [Column].
 * @param scrollState Scroll state used when [scrollable] is true (defaults to [rememberScrollState]).
 * @param applyImePadding Whether to apply [Modifier.imePadding] so content shifts above the on-screen keyboard.
 * @param containerColor Background color for the scaffold (defaults to [LucentTheme.colors.background]).
 * @param contentColor Content color for the scaffold (defaults to [LucentTheme.colors.onBackground]).
 * @param contentWindowInsets Insets applied to the scaffold (defaults to [ScaffoldDefaults.contentWindowInsets]).
 * @param content The screen content composable scoped to [ColumnScope].
 */
@Composable
fun ScreenWrapper(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    snackbarHostState: SnackbarHostState? = null,
    snackbarHost: (@Composable () -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    applySystemBarsPadding: Boolean = true,
    scrollable: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    applyImePadding: Boolean = false,
    containerColor: Color = LucentTheme.colors.background,
    contentColor: Color = LucentTheme.colors.onBackground,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier.then(
            if (applySystemBarsPadding) {
                Modifier.windowInsetsPadding(contentWindowInsets)
            } else {
                Modifier
            }
        ),
        topBar = { topBar?.invoke() },
        bottomBar = { bottomBar?.invoke() },
        snackbarHost = {
            snackbarHost?.invoke() ?: snackbarHostState?.let { SnackbarHost(it) }
        },
        floatingActionButton = { floatingActionButton?.invoke() },
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (applySystemBarsPadding) {
                        Modifier
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                start = LucentTheme.dimensions.paddingMedium,
                                end = LucentTheme.dimensions.paddingMedium,
                                bottom = innerPadding.calculateBottomPadding(),
                            )
                            .padding(vertical = LucentTheme.dimensions.paddingMedium)
                    } else {
                        Modifier
                    }
                )
                .then(if (applyImePadding) Modifier.imePadding() else Modifier)
                .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenWrapperPreview() {
    LucentTheme {
        ScreenWrapper {
            Text(text = "Lucent Screen Content")
        }
    }
}
