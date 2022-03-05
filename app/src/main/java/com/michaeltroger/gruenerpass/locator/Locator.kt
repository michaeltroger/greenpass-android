package com.michaeltroger.gruenerpass.locator

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.room.Room
import com.michaeltroger.gruenerpass.DocumentNameRepo
import com.michaeltroger.gruenerpass.DocumentNameRepoImpl
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.db.AppDatabase
import com.michaeltroger.gruenerpass.model.PdfHandler
import com.michaeltroger.gruenerpass.model.PdfHandlerImpl
import com.michaeltroger.gruenerpass.settings.SettingsFragment

object Locator {

    fun database(context: Context): AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "greenpass"
    ).build()

    fun pdfHandler(context: Context): PdfHandler = PdfHandlerImpl(context.applicationContext)

    fun documentNameRepo(context: Context): DocumentNameRepo = DocumentNameRepoImpl(context.applicationContext)

    fun biometricPromptInfo(context: Context): BiometricPrompt.PromptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.authenticate))
        .setConfirmationRequired(false)
        .setAllowedAuthenticators(SettingsFragment.AUTHENTICATORS)
        .build()
}