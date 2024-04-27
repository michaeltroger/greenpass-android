package com.michaeltroger.gruenerpass.utils

import android.view.View
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class NullableViewTypeSafeMatcher(
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
