package com.michaeltroger.gruenerpass.logging

import android.util.Log
import com.michaeltroger.gruenerpass.BuildConfig

private const val TAG = "greenpass"
private val log: Boolean = BuildConfig.DEBUG

interface Logger {
    fun logDebug(value: Any?)
    fun logError(value: Any?)
}

class LoggerImpl : Logger {
    override fun logDebug(value: Any?) {
        if (log) {
            Log.d(TAG, value.toString())
        }
    }
    override fun logError(value: Any?) {
        if (log) {
            Log.e(TAG, value.toString())
        }
    }
}
