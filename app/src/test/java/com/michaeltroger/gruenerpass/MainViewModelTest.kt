package com.michaeltroger.gruenerpass

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
import com.michaeltroger.gruenerpass.file.FileRepo
import com.michaeltroger.gruenerpass.logging.Logger
import com.michaeltroger.gruenerpass.pdf.PdfDecryptor
import com.michaeltroger.gruenerpass.pdf.PdfRenderer
import com.michaeltroger.gruenerpass.pdf.PdfRendererBuilder
import com.michaeltroger.gruenerpass.settings.PreferenceManager
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import com.michaeltroger.gruenerpass.utils.InstantExecutionRule
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
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
    private val preferenceManager = mockk<PreferenceManager>(relaxed = true)
    private val pdfDecryptor = mockk<PdfDecryptor>(relaxed = true)
    private val pdfRenderer = mockk<PdfRenderer>(relaxed = true)
    private val fileRepo = mockk<FileRepo>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    @Before
    fun startUp() {
        mockPdfRenderer()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `verify initial state`() = runTest {
        val vm = createVM()

        vm.viewState.value should beInstanceOf<ViewState.Loading>()
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
        mockIsPasswordProtectedFile(true)

        val vm = createVM()
        advanceUntilIdle()

        vm.viewEvent.test {
            vm.copyAndSetPendingFile(Uri.EMPTY)

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
            vm.copyAndSetPendingFile(Uri.EMPTY)

            expectNoEvents()
        }

        vm.viewState.value should beInstanceOf<ViewState.Locked>()
    }

    @Test
    fun `verify error while parsing file`() = runTest {
        mockIsPasswordProtectedFile(false)
        mockShouldAuthenticatePreference(false)
        mockCopyPdfToAppSuccess()

        val vm = createVM()
        advanceUntilIdle()

        vm.viewEvent.test {
            vm.copyAndSetPendingFile(Uri.EMPTY)

            awaitItem() shouldBe ViewEvent.CloseAllDialogs
            awaitItem() shouldBe ViewEvent.ErrorParsingFile
        }

        vm.viewState.value should beInstanceOf<ViewState.Empty>()
    }

    @Test
    fun `verify added new certificate`() = runTest {
        mockDbEntries(listOf(mockk()))
        mockIsPasswordProtectedFile(false)
        mockShouldAuthenticatePreference(false)
        mockCopyPdfToAppSuccess()
        mockLoadFileSuccess(true)

        val vm = createVM()
        advanceUntilIdle()

        vm.viewEvent.test {
            vm.copyAndSetPendingFile(Uri.EMPTY)

            awaitItem() shouldBe ViewEvent.CloseAllDialogs
            awaitItem() shouldBe ViewEvent.ScrollToLastCertificate
        }

        vm.viewState.value should beInstanceOf<ViewState.Normal>()
    }

    private fun createVM() =
        MainViewModel(
            app = context,
            db = db,
            fileRepo = fileRepo,
            logger = logger,
            preferenceManager = preferenceManager,
            pdfDecryptor = pdfDecryptor,
        )

    private fun mockShouldAuthenticatePreference(prefValue: Boolean) {
        every {
            preferenceManager.shouldAuthenticate()
        } returns prefValue
    }

    private fun mockIsPasswordProtectedFile(value: Boolean) {
        coEvery {
            pdfDecryptor.isPdfPasswordProtected(any())
        } returns value
    }

    private fun mockCopyPdfToAppSuccess() {
        coEvery {
            fileRepo.copyToApp(any())
        } returns Certificate("any.pdf", "Doc name")
    }

    private fun mockPdfRenderer() {
        mockkObject(PdfRendererBuilder)
        every {
            PdfRendererBuilder.create(any(), any(), any())
        } returns pdfRenderer
    }

    private fun mockLoadFileSuccess(value: Boolean) {
        when (value) {
            true -> {
                coEvery {
                    pdfRenderer.loadFile()
                } just Runs
            }
            false -> {
                coEvery {
                    pdfRenderer.loadFile()
                }.throws(Exception())
            }
        }

    }

    private fun mockDbEntries(certificates: List<Certificate>) {
        coEvery { db.getAll() } returns certificates
    }
}
