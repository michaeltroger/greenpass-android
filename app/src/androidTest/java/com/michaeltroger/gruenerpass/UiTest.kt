package com.michaeltroger.gruenerpass

import androidx.test.core.app.ActivityScenario
import com.michaeltroger.gruenerpass.robots.MainActivityRobot
import com.michaeltroger.gruenerpass.utils.FailingTestWatcher
import com.michaeltroger.gruenerpass.utils.ScreenshotUtil
import org.junit.Rule
import org.junit.Test

class UiTest {

    private val scenario = ActivityScenario.launch(MainActivity::class.java)

    @get:Rule
    val failingTestWatcher = FailingTestWatcher()

    @Test
    fun emptyState() {
        MainActivityRobot().verifyEmptyState()
        ScreenshotUtil.recordScreenshot("empty_state")
    }

    @Test
    fun normalState() {
        MainActivityRobot()
            .selectAddDocument()
            .goToPdfFolder()
            .selectRegularPdf("demo.pdf")
            .verifyPdfDocumentLoaded("demo")

        ScreenshotUtil.recordScreenshot("normal_state")
    }

    @Test
    fun qrCode() {
        MainActivityRobot()
            .selectAddDocument()
            .goToPdfFolder()
            .selectRegularPdf("qr.pdf")
            .verifyPdfDocumentLoaded("qr")

        ScreenshotUtil.recordScreenshot("qr_code")
    }

    @Test
    fun passwordProtected() {
        MainActivityRobot()
            .selectAddDocument()
            .goToPdfFolder()
            .selectPasswordProtectedPdf("password.pdf")
            .verifyPasswordDialogShown()
            .enterPasswordAndConfirm("test")
            .verifyPdfDocumentLoaded("password")

        ScreenshotUtil.recordScreenshot("password_protected")
    }
}
