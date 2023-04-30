package com.michaeltroger.gruenerpass.robots

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.utils.click
import com.michaeltroger.gruenerpass.utils.verifyIsDisplayed
import com.michaeltroger.gruenerpass.utils.waitUntilIdle
import com.michaeltroger.gruenerpass.utils.waitUntilNoException
import org.hamcrest.CoreMatchers.`is`

class MainActivityRobot {

    fun verifyEmptyState() = apply {
        waitUntilIdle()
        waitUntilNoException {
            onView(withId(R.id.add)).verifyIsDisplayed()
        }
    }

    fun verifyDocumentLoaded(docName: String, expectQr: Boolean = false) = apply {
        waitUntilNoException {
            onView(withId(R.id.certificates))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
            onView(withId(R.id.certificate))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
            onView(
                withTagValue(`is`(if (expectQr) "qr_loaded" else "pdf_loaded"))
            ).verifyIsDisplayed()

            onView(withId(R.id.deleteIcon)).verifyIsDisplayed()
            onView(withText(docName)).verifyIsDisplayed()
        }
    }

    fun selectAddDocument(): AndroidFileAppRobot {
        waitUntilNoException {
            onView(withId(R.id.add)).click()
        }
        return AndroidFileAppRobot()
    }
}
