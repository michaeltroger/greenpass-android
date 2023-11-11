package com.michaeltroger.gruenerpass

import androidx.test.core.app.ActivityScenario
import com.michaeltroger.gruenerpass.robots.MainActivityRobot
import com.michaeltroger.gruenerpass.utils.FailingTestWatcher
import org.junit.Rule
import org.junit.Test

class UiTest {

    private val scenario = ActivityScenario.launch(MainActivity::class.java)

    @get:Rule
    val failingTestWatcher = FailingTestWatcher()

    @Test
    fun passwordProtected() {
        MainActivityRobot()
            .selectFirstDocument()
            .goToPdfFolder()
            .openPasswordProtectedPdf(fileName = "password.pdf")
            .verifyPasswordDialogShown()
            .enterPasswordAndConfirm(password = "test")
            .verifyDocumentLoaded(docName = "password")
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
}
