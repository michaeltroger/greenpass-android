package com.michaeltroger.gruenerpass.robots

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

private const val RETRIALS = 3
private const val TIMEOUT = 5000L

class AndroidFileAppRobot {

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val hamburgerSelector = By.desc("Show roots")
    private val rootDirSelector = By.textStartsWith("Android SDK")
    private val testDataDirSelector = By.text("testdata")
    private val pdfSelector = By.textEndsWith(".pdf")
    private val greenPassAppSelector = By.text("Green Pass")
    private val shareButtonSelector = By.desc("Share")

    fun openFileManagerApp() = apply {
        val intent: Intent = context.packageManager.getLaunchIntentForPackage("com.android.documentsui")!!
        context.startActivity(intent)
    }

    fun goToPdfFolder() = apply {
        (1..RETRIALS).forEach { _ ->
            try {
                uiDevice.wait(Until.hasObject(hamburgerSelector), TIMEOUT)
                uiDevice.findObject(hamburgerSelector).click()

                uiDevice.wait(Until.hasObject(rootDirSelector), TIMEOUT)
                uiDevice.findObject(rootDirSelector).click()

                uiDevice.wait(Until.hasObject(testDataDirSelector), TIMEOUT)
                uiDevice.findObject(testDataDirSelector).click()

                uiDevice.wait(Until.hasObject(pdfSelector), TIMEOUT)
                if (!uiDevice.hasObject(pdfSelector)) {
                    return@forEach
                }

                return@apply
            } catch (e: NullPointerException) {
                //ignoring
            }
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
        val selector = By.text(fileName)
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
