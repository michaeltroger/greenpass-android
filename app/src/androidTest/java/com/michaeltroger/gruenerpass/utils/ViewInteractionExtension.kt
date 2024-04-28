package com.michaeltroger.gruenerpass.utils

import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers.not

fun ViewInteraction.verifyIsDisplayed() {
    check(matches(ViewMatchers.isDisplayed()))
}

fun ViewInteraction.verifyNotDisplayed() {
    check(matches(not(ViewMatchers.isDisplayed())))
}

fun ViewInteraction.click() {
    perform(ViewActions.click())
}
