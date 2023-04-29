package com.michaeltroger.gruenerpass

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File

private const val BASE_DIR_SCREENSHOTS = "/sdcard/Pictures/screenshots"
private const val BASE_DIR_ERROR_SCREENSHOTS = "/sdcard/Pictures/error_screenshots"

object ScreenshotUtil {

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun recordScreenshot(name: String) {
        takeScreenshot(directory = BASE_DIR_SCREENSHOTS, fileName = name)
    }

    fun recordErrorScreenshot(name: String) {
        takeScreenshot(directory = BASE_DIR_ERROR_SCREENSHOTS, fileName = name)
    }

    private fun takeScreenshot(directory: String, fileName: String) {
        val dir = File(directory)
        if (!dir.exists()) {
            dir.mkdir()
        }

        uiDevice.takeScreenshot(File("$directory/$fileName.png"))
    }

}
