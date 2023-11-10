package com.michaeltroger.gruenerpass.robots

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

private const val TIMEOUT = 5000L

class ShareDialogRobot {

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val titleSelector = By.text("Sharing 1 file")
    private val printSelector = By.text("Print")

    fun verifyShareDialogShown() = apply {
        uiDevice.wait(Until.hasObject(titleSelector), TIMEOUT)
        uiDevice.wait(Until.hasObject(printSelector), TIMEOUT)
        uiDevice.findObject(titleSelector)!!
        uiDevice.findObject(printSelector)!!
    }

    fun cancelShare(): MainActivityRobot {
        uiDevice.pressBack()
        return MainActivityRobot()
    }
}