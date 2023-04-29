package com.michaeltroger.gruenerpass.utils

import org.junit.rules.TestWatcher
import org.junit.runner.Description

class FailingTestWatcher : TestWatcher() {

    override fun failed(e: Throwable, description: Description) {
        ScreenshotUtil.recordErrorScreenshot(description.methodName)
    }
}
