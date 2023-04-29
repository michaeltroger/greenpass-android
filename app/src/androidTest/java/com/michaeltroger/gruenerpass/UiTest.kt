package com.michaeltroger.gruenerpass

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

private const val TIMEOUT = 10000L

class UiTest {

    private val scenario = ActivityScenario.launch(MainActivity::class.java)
    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val pdfFile = By.text("dummy.pdf")

    @get:Rule
    val failingTestWatcher = FailingTestWatcher()

    @Test
    fun testEmptyState() {
        uiDevice.waitForIdle()
        ScreenshotUtil.recordScreenshot("empty_state")
    }

    @Test
    fun testNormalState() {
        scenario.onActivity {
            it.findViewById<View>(R.id.add).performClick()
        }

        goToDocumentFolder()
        goToDocumentFolder()

        uiDevice.wait(Until.hasObject(pdfFile), TIMEOUT)
        uiDevice.findObject(pdfFile).click()

        val delete = By.desc("Delete")
        uiDevice.wait(Until.hasObject(delete), TIMEOUT)

        ScreenshotUtil.recordScreenshot("normal_state")
    }

    private fun goToDocumentFolder() {
        if (uiDevice.hasObject(pdfFile)) {
            return
        }
        val hamburgerSelector = By.desc("Show roots")
        uiDevice.wait(Until.hasObject(hamburgerSelector), TIMEOUT)
        uiDevice.findObject(hamburgerSelector).click()

        val rootDir = By.textStartsWith("Android SDK")
        uiDevice.wait(Until.hasObject(rootDir), TIMEOUT)
        uiDevice.findObject(rootDir).click()

        val docsDir = By.text("Documents")
        uiDevice.wait(Until.hasObject(docsDir), TIMEOUT)
        uiDevice.findObject(docsDir).click()
    }
}
