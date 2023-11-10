package com.michaeltroger.gruenerpass

import androidx.test.core.app.ActivityScenario
import com.michaeltroger.gruenerpass.robots.AndroidFileAppRobot
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
            .selectFirstDocument()
            .goToPdfFolder()
            .openPdf(fileName = "demo.pdf")
            .verifyDocumentLoaded(docName = "demo")

        ScreenshotUtil.recordScreenshot("normal_state")
    }

    @Test
    fun multipleDocuments() {
        MainActivityRobot()
            .selectFirstDocument()
            .goToPdfFolder()
            .openPdf(fileName = "demo.pdf")
            .verifyDocumentLoaded(docName = "demo", expectedDocumentCount = 1)
            .selectAnotherDocument()
            .goToPdfFolder()
            .openPdf(fileName = "demo1.pdf")
            .verifyDocumentLoaded(docName = "demo1", expectedDocumentCount = 2)

        ScreenshotUtil.recordScreenshot("multiple_documents")
    }

    @Test
    fun qrCode() {
        MainActivityRobot()
            .selectFirstDocument()
            .goToPdfFolder()
            .openPdf(fileName = "qr.pdf")
            .verifyDocumentLoaded(docName = "qr", expectQr = true)

        ScreenshotUtil.recordScreenshot("qr_code")
    }

    @Test
    fun passwordProtected() {
        MainActivityRobot()
            .selectFirstDocument()
            .goToPdfFolder()
            .openPasswordProtectedPdf(fileName = "password.pdf")
            .verifyPasswordDialogShown()
            .enterPasswordAndConfirm(password = "test")
            .verifyDocumentLoaded(docName = "password")

        ScreenshotUtil.recordScreenshot("password_protected")
    }

    @Test
    fun deleteDocument() {
        MainActivityRobot()
            .selectFirstDocument()
            .goToPdfFolder()
            .openPdf(fileName = "demo.pdf")
            .verifyDocumentLoaded(docName = "demo")
            .clickDeleteDocument()
            .verifyDeleteDialogShown()
            .cancelDelete()
            .verifyDocumentLoaded(docName = "demo")
            .clickDeleteDocument()
            .confirmDelete()
            .verifyEmptyState()
    }

    @Test
    fun shareDocument() {
        MainActivityRobot()
            .selectFirstDocument()
            .goToPdfFolder()
            .openPdf(fileName = "demo.pdf")
            .verifyDocumentLoaded(docName = "demo")
            .clickShareDocument()
            .verifyShareDialogShown()
            .cancelShare()
            .verifyDocumentLoaded(docName = "demo")
    }

    @Test
    fun fileOpenedFromFileManager() {
        AndroidFileAppRobot()
            .openFileManagerApp()
            .goToPdfFolder()
            .openPdf(fileName = "demo.pdf")
            .verifyDocumentLoaded(docName = "demo")
    }

    @Test
    fun fileSharedFromFileManager() {
        AndroidFileAppRobot()
            .openFileManagerApp()
            .goToPdfFolder()
            .selectPdf(fileName = "demo.pdf")
            .selectShare()
            .selectGreenPass()
            .verifyDocumentLoaded(docName = "demo")
    }
}
