package com.michaeltroger.gruenerpass

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.michaeltroger.gruenerpass.robots.DetailViewRobot
import com.michaeltroger.gruenerpass.robots.MainActivityRobot
import com.michaeltroger.gruenerpass.utils.FailingTestWatcher
import com.michaeltroger.gruenerpass.utils.ScreenshotUtil
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ListScreenshotTest {

    @get:Rule
    val failingTestWatcher = FailingTestWatcher()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(context.getString(R.string.key_preference_show_list_layout), true)
        }
    }

    @Test
    fun emptyState() {
        ActivityScenario.launch(MainActivity::class.java)
        MainActivityRobot()
            .verifyEmptyState()

        ScreenshotUtil.recordScreenshot("list_layout_empty")
    }

    @Test
    fun normal() {
        ActivityScenario.launch(MainActivity::class.java)
        MainActivityRobot()
            .selectFirstDocument()
            .goToPdfFolder()
            .openPdf(fileName = "demo.pdf")

        DetailViewRobot()
            .verifyDocumentLoaded(docName = "demo")
            .goBack()
            .verifyDocumentLoaded(docName = "demo", listLayout = true)

        ScreenshotUtil.recordScreenshot("list_layout_normal")
    }

    @Test
    fun detail() {
        ActivityScenario.launch(MainActivity::class.java)
        MainActivityRobot()
            .selectFirstDocument()
            .goToPdfFolder()
            .openPdf(fileName = "demo.pdf")

        DetailViewRobot()
            .verifyDocumentLoaded(docName = "demo")

        ScreenshotUtil.recordScreenshot("detail_screen")
    }
}
