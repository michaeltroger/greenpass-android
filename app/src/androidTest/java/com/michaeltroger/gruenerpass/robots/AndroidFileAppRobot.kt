package com.michaeltroger.gruenerpass.robots

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.lang.NullPointerException

private const val RETRIALS = 3
private const val TIMEOUT = 10000L

class AndroidFileAppRobot {

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val pdfFileSelector = By.text("dummy.pdf")
    private val hamburgerSelector = By.desc("Show roots")
    private val rootDirSelector = By.textStartsWith("Android SDK")

    fun goToPdfFolder() = apply {
        (1..RETRIALS).forEach { _ ->
            try {
                if (uiDevice.hasObject(pdfFileSelector)) {
                    return@apply
                }
                uiDevice.wait(Until.hasObject(hamburgerSelector), TIMEOUT)
                uiDevice.findObject(hamburgerSelector).click()

                uiDevice.wait(Until.hasObject(rootDirSelector), TIMEOUT)
                uiDevice.findObject(rootDirSelector).click()
            } catch (e: NullPointerException) {
                //ignoring
            }
        }
    }

    fun selectPdfDocument(): MainActivityRobot {
        uiDevice.wait(Until.hasObject(pdfFileSelector), TIMEOUT)
        uiDevice.findObject(pdfFileSelector).click()

        return MainActivityRobot()
    }
}
