package com.michaeltroger.gruenerpass

import android.annotation.SuppressLint
import android.app.Application
import com.michaeltroger.gruenerpass.update.AppMigrator
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader

class GreenPassApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppMigrator(applicationContext).performMigration()
        PDFBoxResourceLoader.init(this)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: GreenPassApplication
            private set
    }
}