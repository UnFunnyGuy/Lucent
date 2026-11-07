package app.lucent.firefly.core.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigatorTest {

    private object Login : NavKey
    private object Home : NavKey
    private object Detail : NavKey

    private object AuthGraph : NavGraph {
        override val startDestination: NavKey = Login
    }

    private object HomeGraph : NavGraph {
        override val startDestination: NavKey = Home
    }

    @Test
    fun setGraphReplacesBackStackWithStartDestination() {
        val navigator = Navigator()
        navigator.setGraph(HomeGraph)
        navigator.navigate(Detail)

        navigator.setGraph(AuthGraph)

        assertEquals(listOf<NavKey>(Login), navigator.backStack.toList())
        assertEquals(AuthGraph, navigator.currentGraph)
    }

    @Test
    fun settingSameGraphKeepsBackStack() {
        val navigator = Navigator()
        navigator.setGraph(HomeGraph)
        navigator.navigate(Detail)

        navigator.setGraph(HomeGraph)

        assertEquals(listOf(Home, Detail), navigator.backStack.toList())
    }

    @Test
    fun goBackNeverEmptiesBackStack() {
        val navigator = Navigator()
        navigator.setGraph(HomeGraph)
        navigator.navigate(Detail)

        navigator.goBack()
        navigator.goBack()

        assertEquals(listOf<NavKey>(Home), navigator.backStack.toList())
    }
}
