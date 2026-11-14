package app.lucent.firefly.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.lucent.firefly.core.navigation.EntryProviderInstaller
import app.lucent.firefly.core.navigation.Navigator
import app.lucent.firefly.core.navigation.scene.BottomSheetSceneStrategy

@Composable
fun LucentNavDisplay(
    navigator: Navigator,
    entryProviderInstallers: List<EntryProviderInstaller>,
    modifier: Modifier = Modifier,
) {
    val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }

    NavDisplay(
        backStack = navigator.backStack,
        modifier = modifier,
        onBack = navigator::goBack,
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        sceneStrategies = listOf(bottomSheetStrategy),
        entryProvider =
            entryProvider {
                entryProviderInstallers.forEach { installer -> installer.install(this) }
            },
    )
}
