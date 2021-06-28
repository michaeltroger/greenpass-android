package com.michaeltroger.gruenerpass

import android.annotation.SuppressLint
import android.app.Application
import com.michaeltroger.gruenerpass.update.AppMigrator

class GruenerPassApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppMigrator(applicationContext).performMigration()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: GruenerPassApplication
            private set
    }
}