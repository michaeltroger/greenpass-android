package com.michaeltroger.gruenerpass.utils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector

fun waitUntilIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        .waitForIdle()
}

fun waitUntilNoException(timeoutMs: Long = 10000, function: () -> Any?) {
    var startTimeMs = System.currentTimeMillis()
    val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    while (true) {
        try {
            function()
            return
        } catch (e: Throwable) {
            // if exception due to ANR then reset timer
            if (e.toString().contains("RootViewWithoutFocusException")) {
                val waitButton = uiDevice.findObject(UiSelector().textContains("Wait"))
                if (waitButton.exists()) {
                    waitButton.click()
                }
                startTimeMs = System.currentTimeMillis()
            }
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

private class TimeoutException(private val e: Throwable) : Exception() {
    override fun toString(): String {
        return "Timeout out, original error: $e"
    }
}

