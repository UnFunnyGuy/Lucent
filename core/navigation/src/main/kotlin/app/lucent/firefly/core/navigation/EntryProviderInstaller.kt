package app.lucent.firefly.core.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

fun interface EntryProviderInstaller {
    fun install(scope: EntryProviderScope<NavKey>)
}
