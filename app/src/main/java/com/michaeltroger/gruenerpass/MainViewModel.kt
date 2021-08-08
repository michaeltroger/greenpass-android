package com.michaeltroger.gruenerpass

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.locator.Locator
import com.michaeltroger.gruenerpass.model.PAGE_INDEX_QR_CODE
import com.michaeltroger.gruenerpass.model.PdfHandler
import com.michaeltroger.gruenerpass.model.PdfRenderer
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class MainViewModel(
    context: Context,
    private val pdfHandler: PdfHandler = Locator.pdfHandler(context),
    val pdfRenderer: PdfRenderer = Locator.pdfRenderer(context)
): ViewModel() {
    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(ViewState.Loading)
    val viewState: StateFlow<ViewState> = _viewState

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    private var uri: Uri? = null

    init {
        viewModelScope.launch {
            if (pdfHandler.doesFileExist()) {
                if (pdfRenderer.loadFile()) {
                    _viewState.emit(ViewState.Certificate(hasQrCode = hasQrCode()))
                } else {
                    _viewState.emit(ViewState.Empty)
                    _viewEvent.emit(ViewEvent.ErrorParsingFile)
                }
            } else {
                _viewState.emit(ViewState.Empty)
            }
        }
    }

    fun setUri(uri: Uri) {
        this.uri = uri
        viewModelScope.launch {
            _viewEvent.emit(ViewEvent.CloseAllDialogs)
            loadFileFromUri()
        }
    }

    private fun loadFileFromUri() {
        val uri = uri!!
        viewModelScope.launch {
            if (pdfHandler.isPdfPasswordProtected(uri)) {
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
            } else {
                _viewState.emit(ViewState.Empty)
                if (pdfHandler.copyPdfToCache(uri) && pdfRenderer.loadFile()) {
                    _viewState.emit(ViewState.Certificate(hasQrCode = hasQrCode()))
                } else {
                    _viewEvent.emit(ViewEvent.ErrorParsingFile)
                }
            }
        }
    }

    fun onPasswordEntered(password: String) {
        viewModelScope.launch {
            if (pdfHandler.decryptAndCopyPdfToCache(uri = uri!!, password = password)) {
                _viewState.emit(ViewState.Empty)
                if (pdfRenderer.loadFile()) {
                    _viewState.emit(ViewState.Certificate(hasQrCode = hasQrCode()))
                } else {
                    _viewEvent.emit(ViewEvent.ErrorParsingFile)
                }
            } else {
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
            }
        }
    }

    fun onDeleteConfirmed() {
        viewModelScope.launch {
            pdfHandler.deleteFile()
            _viewState.emit(ViewState.Empty)
        }
    }

    private suspend fun hasQrCode() = pdfRenderer.hasQrCode(PAGE_INDEX_QR_CODE)

    override fun onCleared() {
        super.onCleared()
        pdfRenderer.onCleared()
    }

}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(context.applicationContext) as T
    }
}