package com.sarath.lucent

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

@KoinApplication
class LucentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin<LucentApplication> {
            androidContext(this@LucentApplication)
            workManagerFactory()
        }
    }
}
