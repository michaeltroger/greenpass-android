package com.michaeltroger.gruenerpass.robots

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.utils.NullableViewTypeSafeMatcher
import com.michaeltroger.gruenerpass.utils.verifyIsDisplayed
import com.michaeltroger.gruenerpass.utils.waitUntilNoException
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class DetailViewRobot {

    fun verifyDocumentLoaded(docName: String, expectedDocumentCount: Int = 1, expectBarcode: Boolean = false) = apply {
        waitUntilNoException {
            onView(withContentDescription(
                androidx.navigation.ui.R.string.nav_app_bar_navigate_up_description
            )).verifyIsDisplayed()

            onView(withIndex(
                withTagValue(`is`(if (expectBarcode) "barcode_loaded" else "pdf_loaded")),
                index = expectedDocumentCount - 1
            )).verifyIsDisplayed()

            onView(withIndex(
                withId(R.id.deleteIcon),
                index = expectedDocumentCount - 1
            )).verifyIsDisplayed()
            onView(withText(docName)).verifyIsDisplayed()
        }
    }

    fun goBack() = MainActivityRobot()

    private fun withIndex(matcher: Matcher<View?>, index: Int): TypeSafeMatcher<View?> {
        return NullableViewTypeSafeMatcher(index, matcher)
    }
}