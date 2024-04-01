package com.michaeltroger.gruenerpass.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.utils.click
import com.michaeltroger.gruenerpass.utils.verifyIsDisplayed
import org.hamcrest.CoreMatchers

private const val TIMEOUT = 5000L

class ChangeDocumentNameDialogRobot {

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val titleSelector = By.text("Change document name")

    fun verifyChangeDocumentNameDialogShown() = apply {
        uiDevice.wait(Until.hasObject(titleSelector), TIMEOUT)
        Espresso.onView(ViewMatchers.withText(R.string.dialog_document_name_title)).verifyIsDisplayed()
    }

    fun changeDocumentNameAndConfirm(newDocumentName: String): MainActivityRobot {
        Espresso.onView(
            CoreMatchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.document_name_text_field)),
                ViewMatchers.withClassName(CoreMatchers.endsWith("EditText"))
            )
        ).perform(ViewActions.replaceText(newDocumentName))

        Espresso.onView(ViewMatchers.withText("OK")).click()
        return MainActivityRobot()
    }
}