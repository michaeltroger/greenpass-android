package com.michaeltroger.gruenerpass.logger

import android.util.Log
import javax.inject.Inject

private const val TAG = "greenpass"

internal class LoggerDebugImpl @Inject constructor() : Logger {
    override fun logDebug(value: Any?) {
        Log.d(TAG, value.toString())
    }
    override fun logError(value: Any?) {
        Log.e(TAG, value.toString())
    }
}
