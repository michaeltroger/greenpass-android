package com.michaeltroger.gruenerpass.utils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import java.lang.Exception

fun waitUntilIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        .waitForIdle()
}

fun waitUntilNoException(timeoutMs: Long = 5000, function: () -> Any?) {
    val startTimeMs = System.currentTimeMillis()
    while (true) {
        try {
            function()
            return
        } catch (e: Exception) {
            if (System.currentTimeMillis() > startTimeMs + timeoutMs) {
                throw TimeoutException(e)
            }
        }
        waitFor(500)
    }
}

private fun waitFor(ms: Long) {
    val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    uiDevice.findObject(UiSelector().className("doesNotExist")).waitForExists(ms)
}

private class TimeoutException(private val e: Exception) : Exception() {
    override fun toString(): String {
        return "Timeout out, original error: $e"
    }
}

