package app.lucent.firefly.data.transactions.impl.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import java.time.Clock

@Module
@Configuration
@ComponentScan("app.lucent.firefly.data.transactions.impl")
class TransactionsModule {
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
