package com.michaeltroger.gruenerpass.utils

import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers

fun ViewInteraction.verifyIsDisplayed() {
    check(matches(ViewMatchers.isDisplayed()))
}

fun ViewInteraction.verifyDoesNotExist() {
    check(ViewAssertions.doesNotExist())
}

fun ViewInteraction.click() {
    perform(ViewActions.click())
}
