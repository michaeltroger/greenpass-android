package com.michaeltroger.gruenerpass.logging

import android.util.Log

private const val TAG = "greenpass"

class LoggerImpl : Logger {
    override fun logDebug(value: String) {
        Log.d(TAG, value)
    }
    override fun logError(value: String) {
        Log.e(TAG, value)
    }
}
