package com.michaeltroger.gruenerpass

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
import com.michaeltroger.gruenerpass.file.FileRepo
import com.michaeltroger.gruenerpass.pdfimporter.PdfImportResult
import com.michaeltroger.gruenerpass.pdfimporter.PdfImporter
import com.michaeltroger.gruenerpass.pdfimporter.PendingCertificate
import com.michaeltroger.gruenerpass.settings.PreferenceObserver
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import com.michaeltroger.gruenerpass.utils.InstantExecutionRule
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MainViewModelTest {

    @get:Rule
    val instantExecutionRule = InstantExecutionRule()

    private val context = getApplicationContext<Application>()
    private val db = mockk<CertificateDao>(relaxed = true)
    private val preferenceObserver = mockk<PreferenceObserver>(relaxed = true)
    private val pdfImporter = mockk<PdfImporter>(relaxed = true)
    private val fileRepo = mockk<FileRepo>(relaxed = true)

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `verify initial state`() = runTest {
        val vm = createVM()

        vm.viewState.value should beInstanceOf<ViewState.Initial>()
    }

    @Test
    fun `verify empty state`() = runTest {
        val vm = createVM()
        advanceUntilIdle()

        vm.viewState.value should beInstanceOf<ViewState.Empty>()
    }

    @Test
    fun `verify normal state`() = runTest {
        mockDbEntries(listOf(mockk()))

        val vm = createVM()
        advanceUntilIdle()

        vm.viewState.value should beInstanceOf<ViewState.Normal>()
    }

    @Test
    fun `verify locked state`() = runTest {
        mockShouldAuthenticatePreference(true)

        val vm = createVM()
        advanceUntilIdle()

        vm.viewState.value should beInstanceOf<ViewState.Locked>()
    }

    @Test
    fun `verify app gets unlocked`() = runTest {
        mockShouldAuthenticatePreference(true)

        val vm = createVM()
        advanceUntilIdle()
        vm.onAuthenticationSuccess()
        advanceUntilIdle()

        vm.viewState.value should beInstanceOf<ViewState.Empty>()
    }

    @Test
    fun `verify app gets locked again`() = runTest {
        mockShouldAuthenticatePreference(true)

        val vm = createVM()
        advanceUntilIdle()
        vm.onAuthenticationSuccess()
        advanceUntilIdle()
        vm.onInteractionTimeout()
        advanceUntilIdle()

        vm.viewState.value should beInstanceOf<ViewState.Locked>()
    }

    @Test
    fun `verify enter password dialog shown`() = runTest {
        mockDbEntries(listOf(mockk()))
        mockShouldAuthenticatePreference(false)
        mockPdfImporter(PdfImportResult.PasswordRequired)

        val vm = createVM()
        advanceUntilIdle()

        vm.viewEvent.test {
            vm.setPendingFile(Uri.EMPTY)

            awaitItem() shouldBe ViewEvent.CloseAllDialogs
            awaitItem() shouldBe ViewEvent.ShowPasswordDialog
        }

        vm.viewState.value should beInstanceOf<ViewState.Normal>()
    }

    @Test
    fun `verify don't open file when locked`() = runTest {
        mockShouldAuthenticatePreference(true)

        val vm = createVM()
        advanceUntilIdle()

        vm.viewEvent.test {
            vm.setPendingFile(Uri.EMPTY)

            expectNoEvents()
        }

        vm.viewState.value should beInstanceOf<ViewState.Locked>()
    }

    @Test
    fun `verify added new certificate`() = runTest {
        mockDbEntries(listOf(mockk()))
        mockPdfImporter()
        mockShouldAuthenticatePreference(false)

        val vm = createVM()
        advanceUntilIdle()

        vm.viewEvent.test {
            vm.setPendingFile(Uri.EMPTY)

            awaitItem() shouldBe ViewEvent.CloseAllDialogs
            awaitItem() shouldBe ViewEvent.ScrollToLastCertificate()
        }

        vm.viewState.value should beInstanceOf<ViewState.Normal>()
    }

    private fun createVM() =
        MainViewModel(
            app = context,
            db = db,
            fileRepo = fileRepo,
            preferenceObserver = preferenceObserver,
            pdfImporter = pdfImporter,
        )

    private fun mockShouldAuthenticatePreference(prefValue: Boolean) {
        every {
            preferenceObserver.shouldAuthenticate()
        } returns prefValue
    }

    private fun mockPdfImporter(
        value: PdfImportResult = PdfImportResult.Success(
            PendingCertificate(fileName = "any.pdf", documentName = "Doc name")
        )
    ) {
        coEvery {
            pdfImporter.importPdf()
        } returns value
    }

    private fun mockDbEntries(certificates: List<Certificate>) {
        coEvery { db.getAll() } returns certificates
    }
}
