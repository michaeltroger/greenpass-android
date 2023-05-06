package com.michaeltroger.gruenerpass.logging

import android.util.Log
import com.michaeltroger.gruenerpass.BuildConfig

private const val TAG = "greenpass"
private val log: Boolean = BuildConfig.DEBUG

class LoggerImpl : Logger {
    override fun logDebug(value: String?) {
        if (log) {
            Log.d(TAG, value.toString())
        }
    }
    override fun logError(value: String?) {
        if (log) {
            Log.e(TAG, value.toString())
        }
    }
}
