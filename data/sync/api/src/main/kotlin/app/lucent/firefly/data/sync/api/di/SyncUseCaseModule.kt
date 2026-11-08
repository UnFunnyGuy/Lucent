package app.lucent.firefly.data.sync.api.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module
@Configuration
@ComponentScan("app.lucent.firefly.data.sync.api.usecase")
class SyncUseCaseModule
