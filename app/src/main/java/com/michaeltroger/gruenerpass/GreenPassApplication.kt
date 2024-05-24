package com.michaeltroger.gruenerpass

import android.app.Application
import com.michaeltroger.gruenerpass.cache.BitmapCache
import com.michaeltroger.gruenerpass.migration.AppMigrator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GreenPassApplication : Application() {

    @Inject
    lateinit var appMigrator: AppMigrator

    override fun onCreate() {
        super.onCreate()
        appMigrator.performMigration()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        BitmapCache.memoryCache.evictAll()
    }
}
