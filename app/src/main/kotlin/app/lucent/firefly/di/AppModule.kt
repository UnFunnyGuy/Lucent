package app.lucent.firefly.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module(includes = [CoroutinesModule::class])
@Configuration
@ComponentScan("app.lucent.firefly.di", "app.lucent.firefly.presentation")
class AppModule
