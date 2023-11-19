package com.michaeltroger.gruenerpass.locator

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricPrompt
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.michaeltroger.gruenerpass.GreenPassApplication
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.db.AppDatabase
import com.michaeltroger.gruenerpass.dialogs.CertificateDialogs
import com.michaeltroger.gruenerpass.dialogs.CertificateDialogsImpl
import com.michaeltroger.gruenerpass.file.DocumentNameRepo
import com.michaeltroger.gruenerpass.file.DocumentNameRepoImpl
import com.michaeltroger.gruenerpass.file.FileRepo
import com.michaeltroger.gruenerpass.file.FileRepoImpl
import com.michaeltroger.gruenerpass.logging.Logger
import com.michaeltroger.gruenerpass.logging.LoggerImpl
import com.michaeltroger.gruenerpass.pdf.PdfDecryptor
import com.michaeltroger.gruenerpass.pdf.PdfDecryptorImpl
import com.michaeltroger.gruenerpass.qr.QrRenderer
import com.michaeltroger.gruenerpass.qr.QrRendererImpl
import com.michaeltroger.gruenerpass.settings.EncryptedPreferenceDataStore
import com.michaeltroger.gruenerpass.settings.PreferenceObserver
import com.michaeltroger.gruenerpass.settings.PreferenceObserverImpl
import com.michaeltroger.gruenerpass.settings.SettingsFragment
import com.michaeltroger.gruenerpass.sharing.PdfSharing
import com.michaeltroger.gruenerpass.sharing.PdfSharingImpl

object Locator {

    val context: Context by lazy {
        GreenPassApplication.instance
    }
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

    fun qrRenderer(): QrRenderer = QrRendererImpl()

    fun preferenceManager(context: Context): PreferenceObserver = PreferenceObserverImpl(context)

    fun pdfSharing(): PdfSharing = PdfSharingImpl()

    fun certificateDialogs(): CertificateDialogs = CertificateDialogsImpl()

    val encryptedSharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secret_shared_prefs",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    val encryptedPreferenceDataStore: EncryptedPreferenceDataStore by lazy {
        EncryptedPreferenceDataStore(
            encryptedSharedPreferences
        )
    }
}
