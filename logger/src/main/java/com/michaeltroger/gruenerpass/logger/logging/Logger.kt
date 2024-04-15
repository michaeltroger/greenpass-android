package com.michaeltroger.gruenerpass.logger.logging

import android.util.Log
import com.michaeltroger.gruenerpass.logger.BuildConfig
import javax.inject.Inject

private const val TAG = "greenpass"
private val log: Boolean = BuildConfig.DEBUG

public interface Logger {
    public fun logDebug(value: Any?)
    public fun logError(value: Any?)
}

internal class LoggerImpl @Inject constructor() : Logger {
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
