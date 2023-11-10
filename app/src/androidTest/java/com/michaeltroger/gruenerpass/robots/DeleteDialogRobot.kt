package com.michaeltroger.gruenerpass.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.utils.click
import com.michaeltroger.gruenerpass.utils.verifyIsDisplayed

private const val TIMEOUT = 5000L

class DeleteDialogRobot {

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val titleSelector = By.text("Do you really want to delete the document from the app?")

    fun verifyDeleteDialogShown() = apply {
        uiDevice.wait(Until.hasObject(titleSelector), TIMEOUT)
        Espresso.onView(ViewMatchers.withText(R.string.dialog_delete_confirmation_message)).verifyIsDisplayed()
    }

    fun cancelDelete(): MainActivityRobot {
        Espresso.onView(ViewMatchers.withText("CANCEL")).click()
        return MainActivityRobot()
    }

    fun confirmDelete(): MainActivityRobot {
        Espresso.onView(ViewMatchers.withText("OK")).click()
        return MainActivityRobot()
    }
}