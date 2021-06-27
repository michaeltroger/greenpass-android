package com.michaeltroger.gruenerpass

import android.annotation.SuppressLint
import android.app.Application

class GruenerPassApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: GruenerPassApplication
            private set
    }
}