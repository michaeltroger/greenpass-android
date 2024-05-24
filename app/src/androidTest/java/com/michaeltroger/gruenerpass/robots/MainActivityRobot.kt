package com.michaeltroger.gruenerpass.robots

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.utils.NullableViewTypeSafeMatcher
import com.michaeltroger.gruenerpass.utils.click
import com.michaeltroger.gruenerpass.utils.verifyIsDisplayed
import com.michaeltroger.gruenerpass.utils.verifyDoesNotExist
import com.michaeltroger.gruenerpass.utils.waitUntilIdle
import com.michaeltroger.gruenerpass.utils.waitUntilNoException
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

private const val filesApp = "com.android.documentsui"

class MainActivityRobot {
    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val addButtonMatcher = withId(R.id.add_button)
    private val menuAddButtonMatcher = withId(R.id.add)
    private val authenticateButtonMatcher = withId(R.id.authenticate)

    fun verifyEmptyState() = apply {
        waitUntilIdle()
        waitUntilNoException {
            onView(addButtonMatcher).verifyIsDisplayed()
        }
    }

    fun verifyLockedState() = apply {
        waitUntilIdle()
        waitUntilNoException {
            onView(authenticateButtonMatcher).verifyIsDisplayed()
        }
    }

    fun verifyDocumentLoaded(docName: String, listLayout: Boolean = false, expectedDocumentCount: Int = 1, expectBarcode: Boolean = false) = apply {
        waitUntilNoException {
            if (!listLayout) {
                onView(withIndex(
                    withTagValue(`is`(if (expectBarcode) "barcode_loaded" else "pdf_loaded")),
                    index = expectedDocumentCount - 1
                )).verifyIsDisplayed()
            }

            onView(withContentDescription(
                androidx.navigation.ui.R.string.nav_app_bar_navigate_up_description
            )).verifyDoesNotExist()

            onView(withIndex(
                withId(R.id.deleteIcon),
                index = expectedDocumentCount - 1
            )).verifyIsDisplayed()
            onView(withText(docName)).verifyIsDisplayed()
        }
    }

    fun clickDeleteDocument(index: Int = 0): DeleteDialogRobot {
        waitUntilNoException {
            onView(withIndex(
                withId(R.id.deleteIcon),
                index = index
            )).click()
        }
        return DeleteDialogRobot()
    }

    fun clickShareDocument(index: Int = 0): ShareDialogRobot {
        waitUntilNoException {
            onView(withIndex(
                withId(R.id.shareIcon),
                index = index
            )).click()
        }
        return ShareDialogRobot()
    }

    fun clickRenameDocument(index: Int = 0): ChangeDocumentNameDialogRobot {
        waitUntilNoException {
            onView(withIndex(
                withId(R.id.name),
                index = index
            )).click()
        }
        return ChangeDocumentNameDialogRobot()
    }

    fun openDetailView(index: Int = 0): DetailViewRobot {
        waitUntilNoException {
            onView(withIndex(
                withId(R.id.certificate_list_item_root),
                index = index
            )).click()
        }
        return DetailViewRobot()
    }

    fun selectFirstDocument(): AndroidFileAppRobot {
        uiDevice.executeShellCommand("pm clear $filesApp")
        waitUntilNoException {
            onView(addButtonMatcher).click()
        }
        return AndroidFileAppRobot()
    }

    fun selectAnotherDocument(): AndroidFileAppRobot {
        uiDevice.executeShellCommand("pm clear $filesApp")
        waitUntilNoException {
            onView(menuAddButtonMatcher).click()
        }
        return AndroidFileAppRobot()
    }

    private fun withIndex(matcher: Matcher<View?>, index: Int): TypeSafeMatcher<View?> {
        return NullableViewTypeSafeMatcher(index, matcher)
    }
}
