package app.lucent.firefly.core.network.di

import app.lucent.firefly.core.network.client.FireflyJson
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@Configuration
@ComponentScan("app.lucent.firefly.core.network")
class NetworkModule {
    @Singleton
    fun provideJson(): Json = FireflyJson
}
