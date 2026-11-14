package app.lucent.firefly.feature.capture.impl.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import app.lucent.firefly.core.navigation.EntryProviderInstaller
import app.lucent.firefly.core.navigation.Navigator
import app.lucent.firefly.core.navigation.scene.BottomSheetSceneStrategy
import app.lucent.firefly.feature.capture.api.CaptureNavKey
import app.lucent.firefly.feature.capture.impl.presentation.CaptureRoute
import org.koin.core.annotation.Singleton

@Singleton
class CaptureEntryProviderInstaller(
    private val navigator: Navigator,
) : EntryProviderInstaller {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun install(scope: EntryProviderScope<NavKey>) {
        scope.entry<CaptureNavKey>(metadata = BottomSheetSceneStrategy.bottomSheet(skipPartiallyExpanded = true)) {
            CaptureRoute(onDismiss = navigator::goBack)
        }
    }
}
