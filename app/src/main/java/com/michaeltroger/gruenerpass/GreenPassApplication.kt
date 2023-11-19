package com.michaeltroger.gruenerpass

import android.app.Application
import com.michaeltroger.gruenerpass.update.AppMigrator

class GreenPassApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppMigrator(applicationContext).performMigration()
    }

    companion object {
        lateinit var instance: GreenPassApplication
            private set
    }
}
