package com.michaeltroger.gruenerpass

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.michaeltroger.gruenerpass.db.CertificateDao
import com.michaeltroger.gruenerpass.model.DocumentNameRepo
import com.michaeltroger.gruenerpass.model.PdfHandler
import com.michaeltroger.gruenerpass.model.PdfRenderer
import com.michaeltroger.gruenerpass.model.PdfRendererBuilder
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.mockk.coEvery
import io.mockk.every
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
    private val documentNameRepo = mockk<DocumentNameRepo>(relaxed = true)
    private val preferenceManager = mockk<SharedPreferences>(relaxed = true)
    private val pdfHandler = mockk<PdfHandler>(relaxed = true)
    private val pdfRenderer = mockk<PdfRenderer>(relaxed = true)

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
    fun `verify normal state`() = runTest {
        val vm = createVM()
        advanceUntilIdle()

        vm.viewState.value should beInstanceOf<ViewState.Normal>()
    }

    @Test
    fun `verify locked state`() = runTest {
        mockPreference(R.string.key_preference_biometric, true)

        val vm = createVM()
        advanceUntilIdle()

        vm.viewState.value should beInstanceOf<ViewState.Locked>()
    }

    @Test
    fun `verify enter password dialog shown`() = runTest {
        mockPreference(R.string.key_preference_biometric, false)
        mockIsPasswordProtectedFile(true)

        val vm = createVM()
        advanceUntilIdle()

        vm.viewEvent.test {
            vm.setUri(Uri.EMPTY)

            awaitItem() shouldBe ViewEvent.CloseAllDialogs
            awaitItem() shouldBe ViewEvent.ShowPasswordDialog
        }

        vm.viewState.value should beInstanceOf<ViewState.Normal>()
    }

    @Test
    fun `verify don't open file when locked`() = runTest {
        mockPreference(R.string.key_preference_biometric, true)

        val vm = createVM()
        advanceUntilIdle()

        vm.viewEvent.test {
            vm.setUri(Uri.EMPTY)

            expectNoEvents()
        }

        vm.viewState.value should beInstanceOf<ViewState.Locked>()
    }

    @Test
    fun `verify error while parsing file`() = runTest {
        mockIsPasswordProtectedFile(false)
        mockPreference(R.string.key_preference_biometric, false)
        mockCopyPdfToCacheSuccess( false)

        val vm = createVM()
        advanceUntilIdle()

        vm.viewEvent.test {
            vm.setUri(Uri.EMPTY)

            awaitItem() shouldBe ViewEvent.CloseAllDialogs
            awaitItem() shouldBe ViewEvent.ErrorParsingFile
        }

        vm.viewState.value should beInstanceOf<ViewState.Normal>()
    }

    @Test
    fun `verify added new certificate`() = runTest {
        mockIsPasswordProtectedFile(false)
        mockPreference(R.string.key_preference_biometric, false)
        mockCopyPdfToCacheSuccess( true)
        mockLoadFileSuccess(true)

        val vm = createVM()
        advanceUntilIdle()

        vm.viewEvent.test {
            vm.setUri(Uri.EMPTY)

            awaitItem() shouldBe ViewEvent.CloseAllDialogs
            awaitItem() shouldBe ViewEvent.ScrollToLastCertificate
        }

        vm.viewState.value should beInstanceOf<ViewState.Normal>()
    }

    private fun createVM() =
        MainViewModel(
            app = context,
            db = db,
            documentNameRepo = documentNameRepo,
            preferenceManager = preferenceManager,
            pdfHandler = pdfHandler
        )

    private fun mockPreference(@StringRes prefKey: Int, prefValue: Boolean) {
        every {
            preferenceManager.getBoolean(context.getString(prefKey), any())
        } returns prefValue
    }

    private fun mockIsPasswordProtectedFile(value: Boolean) {
        coEvery {
            pdfHandler.isPdfPasswordProtected(any())
        } returns value
    }

    private fun mockCopyPdfToCacheSuccess(value: Boolean) {
        coEvery {
            pdfHandler.copyPdfToCache(any(), any())
        } returns value
    }

    private fun mockPdfRenderer() {
        mockkObject(PdfRendererBuilder)
        every {
            PdfRendererBuilder.create(any(), any(), any())
        } returns pdfRenderer
    }

    private fun mockLoadFileSuccess(value: Boolean) {
        coEvery {
            pdfRenderer.loadFile()
        } returns value
    }
}
