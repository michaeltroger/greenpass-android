package com.michaeltroger.gruenerpass

import android.app.Application
import com.michaeltroger.gruenerpass.update.AppMigrator
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader

class GreenPassApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppMigrator(applicationContext).performMigration()
        PDFBoxResourceLoader.init(this)
    }
}