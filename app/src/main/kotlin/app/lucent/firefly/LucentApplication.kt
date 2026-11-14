package app.lucent.firefly

import android.app.Application
import app.lucent.firefly.data.sync.api.manager.SyncInitializer
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

@KoinApplication
class LucentApplication : Application() {
    private val syncInitializer: SyncInitializer by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin<LucentApplication> {
            androidContext(this@LucentApplication)
            workManagerFactory()
        }
        syncInitializer.initialize()
    }
}
