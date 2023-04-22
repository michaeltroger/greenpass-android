package com.michaeltroger.gruenerpass

import android.app.Application
import android.content.SharedPreferences
import com.michaeltroger.gruenerpass.db.CertificateDao
import com.michaeltroger.gruenerpass.model.DocumentNameRepo
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions

@Extensions(ExtendWith(InstantExecutionExtension::class))
class MainViewModelTest {

    private val context = mockk<Application>(relaxed = true)
    private val db = mockk<CertificateDao>()
    private val documentNameRepo = mockk<DocumentNameRepo>()
    private val preferenceManager = mockk<SharedPreferences>(relaxed = true)
    @Test
    fun test() {
        // not doing anything yet, just basic unit test setup
        MainViewModel(
            app = context,
            db = db,
            documentNameRepo = documentNameRepo,
            preferenceManager = preferenceManager,
        )
    }
}
