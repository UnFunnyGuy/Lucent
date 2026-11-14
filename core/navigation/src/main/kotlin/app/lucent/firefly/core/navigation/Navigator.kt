package app.lucent.firefly.core.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import org.koin.core.annotation.Singleton

@Singleton
class Navigator {
    val backStack: SnapshotStateList<NavKey> = mutableStateListOf()

    var currentGraph: NavGraph? = null
        private set

    fun setGraph(graph: NavGraph) {
        if (graph == currentGraph) return
        currentGraph = graph
        backStack.clear()
        backStack.add(graph.startDestination)
    }

    fun navigate(destination: NavKey) {
        backStack.add(destination)
    }

    fun goBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}
