package com.michaeltroger.gruenerpass

import app.cash.turbine.test
import com.michaeltroger.gruenerpass.extensions.CoroutinesTestExtension
import com.michaeltroger.gruenerpass.fakes.FakeHandler
import com.michaeltroger.gruenerpass.fakes.FakeRenderer
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MainViewModelTest {

    @JvmField
    @RegisterExtension
    val coroutinesTestExtension = CoroutinesTestExtension()

    @Nested
    inner class Bootup {

        @Test
        fun verifyDocumentAndQrCodeRendered() {
            val renderer = FakeRenderer(loadSuccess = true, hasQrCode = true)
            val handler = FakeHandler(fileInAppCache = true)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            assertThat(vm.viewState.value).isEqualTo(ViewState.Certificate)
            assertThat(vm.hasQrCode).isTrue
        }

        @Test
        fun verifyDocumentOnlyRendered() {
            val renderer = FakeRenderer(loadSuccess = true, hasQrCode = false)
            val handler = FakeHandler(fileInAppCache = true)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            assertThat(vm.viewState.value).isEqualTo(ViewState.Certificate)
            assertThat(vm.hasQrCode).isFalse
        }

        @Test
        fun verifyEmptyState() {
            val renderer = FakeRenderer()
            val handler = FakeHandler(fileInAppCache = false)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            assertThat(vm.viewState.value).isEqualTo(ViewState.Empty)
        }

        @Test
        fun verifyErrorDuringLoadingBringsEmptyState() {
            val renderer = FakeRenderer(loadSuccess = false)
            val handler = FakeHandler(fileInAppCache = true)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            assertThat(vm.viewState.value).isEqualTo(ViewState.Empty)
        }

        @Test
        fun verifyInitialState() {
            coroutinesTestExtension.pauseDispatcher()
            val renderer = FakeRenderer()
            val handler = FakeHandler(fileInAppCache = true)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            assertThat(vm.viewState.value).isEqualTo(ViewState.Loading)
        }

        @Test
        fun verifyErrorDuringLoadingShowsError() = runBlockingTest {
            coroutinesTestExtension.pauseDispatcher()
            val renderer = FakeRenderer(loadSuccess = false)
            val handler = FakeHandler(fileInAppCache = true)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            vm.viewEvent.test {
                coroutinesTestExtension.resumeDispatcher()
                assertThat(expectItem()).isEqualTo(ViewEvent.ErrorParsingFile)
            }
        }
    }

    @Nested
    inner class LoadFile {

        @Test
        fun verifyLoadFile() {
            val renderer = FakeRenderer()
            val handler = FakeHandler(fileInAppCache = false)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            vm.setUri(mockk())
            assertThat(vm.viewState.value).isEqualTo(ViewState.Certificate)
        }

        @Test
        fun verifyEnterPasswordDialog() = runBlockingTest {
            val renderer = FakeRenderer()
            val handler = FakeHandler(fileInAppCache = false, isPasswordProtected = true)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            coroutinesTestExtension.pauseDispatcher()
            vm.setUri(mockk())
            vm.viewEvent.test {
                coroutinesTestExtension.resumeDispatcher()
                assertThat(expectItem()).isEqualTo(ViewEvent.CloseAllDialogs)
                assertThat(expectItem()).isEqualTo(ViewEvent.ShowPasswordDialog)
                assertThat(vm.viewState.value).isEqualTo(ViewState.Empty)
            }
        }

        @Test
        fun verifyReplaceDialog() = runBlockingTest {
            val renderer = FakeRenderer()
            val handler = FakeHandler(fileInAppCache = true)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            coroutinesTestExtension.pauseDispatcher()
            vm.setUri(mockk())
            vm.viewEvent.test {
                coroutinesTestExtension.resumeDispatcher()
                assertThat(expectItem()).isEqualTo(ViewEvent.CloseAllDialogs)
                assertThat(expectItem()).isEqualTo(ViewEvent.ShowReplaceDialog)
                assertThat(vm.viewState.value).isEqualTo(ViewState.Certificate)
            }
        }

        @Test
        fun verifyErrorDuringLoading() = runBlockingTest {
            val renderer = FakeRenderer(loadSuccess = false)
            val handler = FakeHandler(fileInAppCache = false)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            coroutinesTestExtension.pauseDispatcher()
            vm.setUri(mockk())
            vm.viewEvent.test {
                coroutinesTestExtension.resumeDispatcher()
                assertThat(expectItem()).isEqualTo(ViewEvent.CloseAllDialogs)
                assertThat(expectItem()).isEqualTo(ViewEvent.ErrorParsingFile)
                assertThat(vm.viewState.value).isEqualTo(ViewState.Empty)
            }
        }

        @Test
        fun verifyErrorDuringCopying() = runBlockingTest {
            val renderer = FakeRenderer()
            val handler = FakeHandler(fileInAppCache = false, copySuccess = false)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            coroutinesTestExtension.pauseDispatcher()
            vm.setUri(mockk())
            vm.viewEvent.test {
                coroutinesTestExtension.resumeDispatcher()
                assertThat(expectItem()).isEqualTo(ViewEvent.CloseAllDialogs)
                assertThat(expectItem()).isEqualTo(ViewEvent.ErrorParsingFile)
                assertThat(vm.viewState.value).isEqualTo(ViewState.Empty)
            }
        }
    }

    @Nested
    inner class DeleteFile {

        @Test
        fun verifyDeleteLeadsToEmptyState() {
            val renderer = FakeRenderer()
            val handler = FakeHandler(fileInAppCache = true)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            assertThat(vm.viewState.value).isEqualTo(ViewState.Certificate)
            vm.onDeleteConfirmed()
            assertThat(vm.viewState.value).isEqualTo(ViewState.Empty)
        }
    }

    @Nested
    inner class ReplaceFile {

        @Test
        fun verifyReplaceFromDocumentOnlyToWithQr() {
            val renderer = FakeRenderer(hasQrCode = false)
            val handler = FakeHandler(fileInAppCache = true)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            assertThat(vm.viewState.value).isEqualTo(ViewState.Certificate)
            assertThat(vm.hasQrCode).isFalse

            renderer.overrideHasQrCode(true)
            vm.setUri(mockk())
            vm.onReplaceConfirmed()

            assertThat(vm.viewState.value).isEqualTo(ViewState.Certificate)
            assertThat(vm.hasQrCode).isTrue
        }

        @Test
        fun verifyErrorDuringReplacing() = runBlockingTest {
            val renderer = FakeRenderer()
            val handler = FakeHandler(fileInAppCache = true)
            val vm = MainViewModel(
                context = mockk(),
                pdfHandler = handler,
                pdfRenderer = renderer
            )
            assertThat(vm.viewState.value).isEqualTo(ViewState.Certificate)

            handler.overrideCopySuccess(false)
            coroutinesTestExtension.pauseDispatcher()
            vm.setUri(mockk())
            vm.onReplaceConfirmed()

            vm.viewEvent.test {
                coroutinesTestExtension.resumeDispatcher()
                assertThat(expectItem()).isEqualTo(ViewEvent.CloseAllDialogs)
                assertThat(expectItem()).isEqualTo(ViewEvent.ShowReplaceDialog)
                assertThat(expectItem()).isEqualTo(ViewEvent.ErrorParsingFile)
                assertThat(vm.viewState.value).isEqualTo(ViewState.Empty)
            }
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}
