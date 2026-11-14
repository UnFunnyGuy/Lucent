package app.lucent.firefly.core.navigation.scene

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

@OptIn(ExperimentalMaterial3Api::class)
internal data class BottomSheetScene<T : Any>(
    override val key: T,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val config: BottomSheetConfig,
    private val onBack: () -> Unit,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)

    private lateinit var sheetState: SheetState

    override val content: @Composable () -> Unit = {
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = config.skipPartiallyExpanded)
        ModalBottomSheet(
            onDismissRequest = onBack,
            properties = config.properties,
            sheetState = sheetState,
        ) {
            entry.Content()
        }
    }

    override suspend fun onRemove() {
        sheetState.hide()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal data class BottomSheetConfig(
    val properties: ModalBottomSheetProperties,
    val skipPartiallyExpanded: Boolean,
)

/** Shows the top entry in a [ModalBottomSheet] when it was declared with [bottomSheet] metadata. */
@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull() ?: return null
        val config = lastEntry.metadata[BottomSheetKey] ?: return null
        @Suppress("UNCHECKED_CAST")
        return BottomSheetScene(
            key = lastEntry.contentKey as T,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
            entry = lastEntry,
            config = config,
            onBack = onBack,
        )
    }

    companion object {
        /**
         * @param skipPartiallyExpanded open straight to full height, for sheets that hold a whole
         * task (forms) rather than a quick choice.
         */
        fun bottomSheet(
            properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
            skipPartiallyExpanded: Boolean = false,
        ) = metadata { put(BottomSheetKey, BottomSheetConfig(properties, skipPartiallyExpanded)) }

        private object BottomSheetKey : NavMetadataKey<BottomSheetConfig>
    }
}
