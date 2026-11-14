package app.lucent.firefly.di

import app.lucent.firefly.core.navigation.EntryProviderInstaller
import org.koin.core.annotation.Singleton

@Singleton
class EntryProviderInstallers(
    val installers: List<EntryProviderInstaller>,
)
