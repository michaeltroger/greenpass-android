package com.michaeltroger.gruenerpass.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.michaeltroger.gruenerpass.R

private const val TIMEOUT = 5000L

class MainActivityRobot {

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val addIconSelector = By.desc("Add")
    private val deleteIconSelector = By.desc("Delete")

    fun verifyEmptyState() = apply {
        uiDevice.waitForIdle()
        onView(withId(R.id.add)).check(matches(isDisplayed()))
    }

    fun verifyPdfDocumentLoaded(docName: String) = apply {
        uiDevice.wait(Until.hasObject(deleteIconSelector), TIMEOUT)
        onView(withId(R.id.deleteIcon)).check(matches(isDisplayed()))
        onView(withText(docName)).check(matches(isDisplayed()))
    }

    fun selectAddDocument(): AndroidFileAppRobot {
        uiDevice.wait(Until.hasObject(addIconSelector), TIMEOUT)
        uiDevice.findObject(addIconSelector).click()
        return AndroidFileAppRobot()
    }
}
