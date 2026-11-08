package app.lucent.firefly.data.auth.api.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module
@Configuration
@ComponentScan("app.lucent.firefly.data.auth.api.usecase")
class AuthUseCaseModule
