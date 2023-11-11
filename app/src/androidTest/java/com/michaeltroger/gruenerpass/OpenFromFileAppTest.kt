package com.michaeltroger.gruenerpass

import com.michaeltroger.gruenerpass.robots.AndroidFileAppRobot
import com.michaeltroger.gruenerpass.utils.FailingTestWatcher
import org.junit.Rule
import org.junit.Test

class OpenFromFileAppTest {

    @get:Rule
    val failingTestWatcher = FailingTestWatcher()

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
