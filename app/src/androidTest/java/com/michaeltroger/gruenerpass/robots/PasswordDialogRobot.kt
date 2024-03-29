package com.michaeltroger.gruenerpass.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.utils.click
import com.michaeltroger.gruenerpass.utils.verifyIsDisplayed
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.endsWith

private const val TIMEOUT = 5000L

class PasswordDialogRobot {

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val titleSelector = By.text("Decrypt password protected PDF")

    fun verifyPasswordDialogShown() = apply {
        uiDevice.wait(Until.hasObject(titleSelector), TIMEOUT)
        onView(withText(R.string.dialog_password_protection_title)).verifyIsDisplayed()
    }

    fun enterPasswordAndConfirm(password: String): MainActivityRobot {
        onView(allOf(
            isDescendantOfA(withId(R.id.password_text_field)),
            withClassName(endsWith("EditText"))
        )).perform(replaceText(password))

        onView(withText("OK")).click()
        return MainActivityRobot()
    }
}
