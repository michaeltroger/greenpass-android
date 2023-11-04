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
            .selectPdf(fileName = "demo.pdf")
            .verifyDocumentLoaded(docName = "demo")

        ScreenshotUtil.recordScreenshot("normal_state")
    }

    @Test
    fun multipleDocuments() {
        MainActivityRobot()
            .selectAddDocument()
            .goToPdfFolder()
            .selectPdf(fileName = "demo.pdf")
            .verifyDocumentLoaded(docName = "demo")
            .selectAddDocument()
            .goToPdfFolder()
            .selectPdf(fileName = "demo1.pdf")
            .verifyDocumentLoaded(docName = "demo1")

        ScreenshotUtil.recordScreenshot("multiple_documents")
    }

    @Test
    fun qrCode() {
        MainActivityRobot()
            .selectAddDocument()
            .goToPdfFolder()
            .selectPdf(fileName = "qr.pdf")
            .verifyDocumentLoaded(docName = "qr", expectQr = true)

        ScreenshotUtil.recordScreenshot("qr_code")
    }

    @Test
    fun passwordProtected() {
        MainActivityRobot()
            .selectAddDocument()
            .goToPdfFolder()
            .selectPasswordProtectedPdf(fileName = "password.pdf")
            .verifyPasswordDialogShown()
            .enterPasswordAndConfirm(password = "test")
            .verifyDocumentLoaded(docName = "password")

        ScreenshotUtil.recordScreenshot("password_protected")
    }
}
