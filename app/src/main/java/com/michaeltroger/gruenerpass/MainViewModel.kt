package com.michaeltroger.gruenerpass

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.model.PAGE_INDEX_QR_CODE
import com.michaeltroger.gruenerpass.model.PdfHandler
import com.michaeltroger.gruenerpass.model.PdfRenderer
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class MainViewModel(app: Application): AndroidViewModel(app) {
    private val _viewState = MutableStateFlow(ViewState.Loading)
    val viewState: StateFlow<ViewState> = _viewState

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    private var uri: Uri? = null
    private val pdfHandler = PdfHandler(getApplication<Application>())

    val pdfRenderer = PdfRenderer(getApplication<Application>())

    var hasQrCode = false
        private set

    init {
        viewModelScope.launch {
            if (pdfHandler.doesFileExist()) {
                if (loadFile()) {
                    _viewState.emit(ViewState.Certificate)
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
            if (pdfHandler.doesFileExist()) {
                _viewEvent.emit(ViewEvent.ShowReplaceDialog)
            } else {
                loadFileFromUri()
            }
        }
    }

    private fun loadFileFromUri() {
        val uri = uri!!
        viewModelScope.launch {
            if (pdfHandler.isPdfPasswordProtected(uri)) {
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
            } else {
                _viewState.emit(ViewState.Empty)
                if (pdfHandler.copyPdfToCache(uri) && loadFile()) {
                    _viewState.emit(ViewState.Certificate)
                } else {
                    _viewEvent.emit(ViewEvent.ErrorParsingFile)
                }
            }
        }
    }

    fun onReplaceConfirmed() {
        loadFileFromUri()
    }

    fun onPasswordEntered(password: String) {
        viewModelScope.launch {
            if (pdfHandler.decryptAndCopyPdfToCache(uri = uri!!, password = password)) {
                _viewState.emit(ViewState.Empty)
                if (loadFile()) {
                    _viewState.emit(ViewState.Certificate)
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

    private suspend fun loadFile(): Boolean {
        val success = pdfRenderer.loadFile()
        hasQrCode = pdfRenderer.getQrCodeIfPresent(PAGE_INDEX_QR_CODE) != null
        return success
    }

    override fun onCleared() {
        super.onCleared()
        pdfRenderer.onCleared()
    }

}