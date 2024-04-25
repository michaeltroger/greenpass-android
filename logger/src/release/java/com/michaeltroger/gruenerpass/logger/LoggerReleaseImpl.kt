package com.michaeltroger.gruenerpass.logger

import javax.inject.Inject

internal class LoggerReleaseImpl @Inject constructor() : Logger {
    override fun logDebug(value: Any?) {
        // do nothing in release build
    }
    override fun logError(value: Any?) {
        // do nothing in release build
    }
}
