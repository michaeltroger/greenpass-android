package com.michaeltroger.gruenerpass.robots

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.utils.click
import com.michaeltroger.gruenerpass.utils.verifyIsDisplayed
import com.michaeltroger.gruenerpass.utils.waitUntilIdle
import com.michaeltroger.gruenerpass.utils.waitUntilNoException
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class MainActivityRobot {

    private val addButtonMatcher = withId(R.id.add_button)
    private val menuAddButtonMatcher = withId(R.id.add)

    fun verifyEmptyState() = apply {
        waitUntilIdle()
        waitUntilNoException {
            onView(addButtonMatcher).verifyIsDisplayed()
        }
    }

    fun verifyDocumentLoaded(docName: String, expectedDocumentCount: Int = 1, expectQr: Boolean = false) = apply {
        waitUntilNoException {
            onView(withIndex(
                withTagValue(`is`(if (expectQr) "qr_loaded" else "pdf_loaded")),
                index = expectedDocumentCount - 1
            )).verifyIsDisplayed()

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

    fun selectFirstDocument(): AndroidFileAppRobot {
        waitUntilNoException {
            onView(addButtonMatcher).click()
        }
        return AndroidFileAppRobot()
    }

    fun selectAnotherDocument(): AndroidFileAppRobot {
        waitUntilNoException {
            onView(menuAddButtonMatcher).click()
        }
        return AndroidFileAppRobot()
    }

    private fun withIndex(matcher: Matcher<View?>, index: Int): TypeSafeMatcher<View?> {
        return NullableViewTypeSafeMatcher(index, matcher)
    }
}

private class NullableViewTypeSafeMatcher(
    private val index: Int,
    private val matcher: Matcher<View?>
) : TypeSafeMatcher<View?>() {
    private var currentIndex = 0
    override fun describeTo(description: Description) {
        description.appendText("with index: ")
        description.appendValue(index)
        matcher.describeTo(description)
    }

    override fun matchesSafely(view: View?): Boolean {
        return matcher.matches(view) && currentIndex++ == index
    }
}
