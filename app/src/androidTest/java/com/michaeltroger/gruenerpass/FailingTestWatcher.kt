package com.michaeltroger.gruenerpass;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;


class FailingTestWatcher : TestWatcher() {

    override fun failed(e: Throwable, description: Description) {
        ScreenshotUtil.recordErrorScreenshot(description.methodName)
    }
}
