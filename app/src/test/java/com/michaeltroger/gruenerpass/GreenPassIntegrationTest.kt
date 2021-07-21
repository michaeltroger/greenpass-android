package com.michaeltroger.gruenerpass;

import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michaeltroger.gruenerpass.fakes.FakeHandler
import com.michaeltroger.gruenerpass.fakes.FakeRenderer
import com.michaeltroger.gruenerpass.locator.Locator
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GreenPassIntegrationTest {

    @Test
    fun verifyEmptyState() {
        val scenario = launchActivity<MainActivity>()
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            onView(withId(R.id.add)).check(matches(isDisplayed()))

            onView(withId(R.id.tab_layout)).check(matches(not(isDisplayed())))
            onView(withId(R.id.pager)).check(matches(not(isDisplayed())))
            onView(withId(R.id.progress_indicator)).check(matches(not(isDisplayed())))
            onView(withId(R.id.delete)).check(doesNotExist())
        }
    }

    @Test
    fun verifyCertificateState() {
        mockkObject(Locator)
        every { Locator.pdfHandler(any()) } returns FakeHandler(fileInAppCache = true)
        every { Locator.pdfRenderer(any()) } returns FakeRenderer()

        val scenario = launchActivity<MainActivity>()
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            onView(withId(R.id.tab_layout)).check(matches(isDisplayed()))
            onView(withId(R.id.pager)).check(matches(isDisplayed()))
            onView(withId(R.id.delete)).check(matches(isDisplayed()))

            onView(withId(R.id.qrcode)).check(matches(isDisplayed()))
            Assertions.assertThat(it.findViewById<ImageView>(R.id.qrcode).drawable).isNotNull

            onView(withId(R.id.progress_indicator)).check(matches(not(isDisplayed())))
            onView(withId(R.id.add)).check(matches(not(isDisplayed())))

            onView(withText(R.string.tab_title_pdf)).perform(click())

            onView(withId(R.id.certificate)).check(matches(isDisplayed()))
            Assertions.assertThat(it.findViewById<RecyclerView>(R.id.certificate).findViewById<ImageView>(R.id.page).drawable).isNotNull
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }
}
