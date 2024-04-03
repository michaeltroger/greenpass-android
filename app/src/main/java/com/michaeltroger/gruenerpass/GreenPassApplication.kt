package com.michaeltroger.gruenerpass

import android.app.Application
import com.michaeltroger.gruenerpass.migration.AppMigrator

class GreenPassApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppMigrator(applicationContext).performMigration()
    }
}
