package com.michaeltroger.gruenerpass.robots

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until

private const val TIMEOUT = 5000L
private const val testFolder = "testdata"

class AndroidFileAppRobot {

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val hamburgerSelector = By.desc("Show roots")
    private val rootDirSelector = By.textStartsWith("Android SDK")
    private val testDataDirSelector = By.text(testFolder)
    private val pdfSelector = By.textEndsWith(".pdf")
    private val greenPassAppSelector = By.text("Green Pass")
    private val shareButtonSelector = By.desc("Share")
    private val listViewSelector = By.desc("List view")

    fun openFileManagerApp() = apply {
        uiDevice.executeShellCommand("am force-stop com.android.documentsui")
        val intent: Intent = context.packageManager.getLaunchIntentForPackage("com.android.documentsui")!!
        context.startActivity(intent)
    }

    fun goToPdfFolder() = apply {
        val uiScrollable = UiScrollable(UiSelector().scrollable(true))
        try {
            uiDevice.wait(Until.hasObject(hamburgerSelector), TIMEOUT)
            uiDevice.findObject(hamburgerSelector).click()

            uiDevice.wait(Until.hasObject(rootDirSelector), TIMEOUT)
            uiDevice.findObject(rootDirSelector).click()

            uiScrollable.scrollTextIntoView(testFolder)
            uiDevice.wait(Until.hasObject(testDataDirSelector), TIMEOUT)
            uiDevice.findObject(testDataDirSelector).click()

            uiDevice.wait(Until.hasObject(pdfSelector), TIMEOUT)
            uiDevice.wait(Until.hasObject(testDataDirSelector), TIMEOUT)

            uiDevice.findObject(listViewSelector).click()
        } catch (e: NullPointerException) {
            //ignoring
        }
    }

    fun openPdf(fileName: String): MainActivityRobot {
        selectFile(fileName = fileName, longClick = false)
        return MainActivityRobot()
    }

    fun selectPdf(fileName: String) = apply {
        selectFile(fileName = fileName, longClick = true)
    }

    fun openPasswordProtectedPdf(fileName: String): PasswordDialogRobot {
        selectFile(fileName = fileName, longClick = false)
        return PasswordDialogRobot()
    }

    private fun selectFile(fileName: String, longClick: Boolean = false) {
        uiDevice.wait(Until.hasObject(pdfSelector), TIMEOUT)

        val uiScrollable = UiScrollable(UiSelector().scrollable(true))
        val selector = By.text(fileName)

        uiScrollable.scrollTextIntoView(fileName)
        uiDevice.wait(Until.hasObject(selector), TIMEOUT)

        if (longClick) {
            uiDevice.findObject(selector).longClick()
        } else {
            uiDevice.findObject(selector).click()
        }
    }

     fun selectShare() = apply {
        uiDevice.wait(Until.hasObject(shareButtonSelector), TIMEOUT)
        uiDevice.findObject(shareButtonSelector).click()
    }

    fun selectGreenPass(): MainActivityRobot {
        uiDevice.wait(Until.hasObject(greenPassAppSelector), TIMEOUT)
        uiDevice.findObject(greenPassAppSelector).click()
        return MainActivityRobot()
    }
}
