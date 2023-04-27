package com.michaeltroger.gruenerpass

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import java.io.File
import org.junit.Test

private const val BASE_DIR = "/sdcard/Pictures/screenshots"

class UiTest {

    val scenario = ActivityScenario.launch(MainActivity::class.java)
    val uiDevice = UiDevice.getInstance(getInstrumentation())

    @Test
    fun testEmptyState() {
        File(BASE_DIR).mkdir()
        uiDevice.takeScreenshot(File("$BASE_DIR/test.png"))
    }
}
