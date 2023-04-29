package com.michaeltroger.gruenerpass.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.michaeltroger.gruenerpass.R

private const val TIMEOUT = 10000L

class MainActivityRobot {

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun verifyEmptyState() = apply {
        uiDevice.waitForIdle()
        onView(withId(R.id.add)).check(matches(isDisplayed()))
    }

    fun verifyPdfDocumentLoaded() = apply {
        val delete = By.desc("Delete")
        uiDevice.wait(Until.hasObject(delete), TIMEOUT)
    }

    fun clickOnAddDocument(): AndroidFileAppRobot {
        uiDevice.findObject(By.desc("Add")).click()
        return AndroidFileAppRobot()
    }
}
