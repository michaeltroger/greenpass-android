package com.michaeltroger.gruenerpass

import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import com.michaeltroger.gruenerpass.robots.MainActivityRobot
import com.michaeltroger.gruenerpass.utils.FailingTestWatcher
import com.michaeltroger.gruenerpass.utils.ScreenshotUtil
import org.junit.Rule
import org.junit.Test

class ScreenshotTest {

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
            .verifyDocumentLoaded(docName = "qr", expectBarcode = true)

        ScreenshotUtil.recordScreenshot("qr_code")
    }

    @Test
    fun aztecCode() {
        MainActivityRobot()
            .selectFirstDocument()
            .goToPdfFolder()
            .openPdf(fileName = "aztec.pdf")
            .verifyDocumentLoaded(docName = "aztec", expectBarcode = true)

        ScreenshotUtil.recordScreenshot("aztec_code")
    }

    @Test
    fun dataMatrixCode() {
        MainActivityRobot()
            .selectFirstDocument()
            .goToPdfFolder()
            .openPdf(fileName = "datamatrix.pdf")
            .verifyDocumentLoaded(docName = "datamatrix", expectBarcode = true)

        ScreenshotUtil.recordScreenshot("data_matrix")
    }

    @Test
    fun pdf417Code() {
        MainActivityRobot()
            .selectFirstDocument()
            .goToPdfFolder()
            .openPdf(fileName = "pdf417.pdf")
            .verifyDocumentLoaded(docName = "pdf417", expectBarcode = true)

        ScreenshotUtil.recordScreenshot("pdf417")
    }

    @Test
    fun darkMode() {
        enableDarkMode()
        MainActivityRobot().verifyEmptyState()

        ScreenshotUtil.recordScreenshot("dark_mode")
    }

    private fun enableDarkMode() {
        scenario.onActivity {
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
            )
        }
    }
}
