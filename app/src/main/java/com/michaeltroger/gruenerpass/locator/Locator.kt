package com.michaeltroger.gruenerpass.locator

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.room.Room
import com.michaeltroger.gruenerpass.file.DocumentNameRepo
import com.michaeltroger.gruenerpass.file.DocumentNameRepoImpl
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.db.AppDatabase
import com.michaeltroger.gruenerpass.file.FileRepo
import com.michaeltroger.gruenerpass.file.FileRepoImpl
import com.michaeltroger.gruenerpass.logging.Logger
import com.michaeltroger.gruenerpass.logging.LoggerImpl
import com.michaeltroger.gruenerpass.model.PdfDecryptor
import com.michaeltroger.gruenerpass.model.PdfDecryptorImpl
import com.michaeltroger.gruenerpass.settings.SettingsFragment

object Locator {

    fun database(context: Context): AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "greenpass"
    ).build()

    fun pdfDecryptor(): PdfDecryptor = PdfDecryptorImpl()

    fun documentNameRepo(context: Context): DocumentNameRepo = DocumentNameRepoImpl(context.applicationContext)

    fun biometricPromptInfo(context: Context): BiometricPrompt.PromptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.authenticate))
        .setConfirmationRequired(false)
        .setAllowedAuthenticators(SettingsFragment.AUTHENTICATORS)
        .build()

    fun logger(): Logger = LoggerImpl()

    fun fileRepo(context: Context): FileRepo = FileRepoImpl(
        context.applicationContext,
        documentNameRepo(context.applicationContext)
    )
}
