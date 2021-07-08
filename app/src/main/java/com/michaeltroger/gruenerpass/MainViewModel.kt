package com.michaeltroger.gruenerpass

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.model.Pdf
import com.michaeltroger.gruenerpass.states.BitmapState
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class MainViewModel(app: Application): AndroidViewModel(app) {
    private val _bitmapState = MutableStateFlow(BitmapState.Unready)
    val bitmapState: Flow<BitmapState> = _bitmapState.filter { it == BitmapState.Ready }

    private val _viewState = MutableStateFlow(ViewState.Empty)
    val viewState: StateFlow<ViewState> = _viewState

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    private var uri: Uri? = null
    private val pdf = Pdf(getApplication<Application>())

    init {
        viewModelScope.launch {
            if (pdf.doesFileExist()) {
                if (parsePdfIntoBitmap()) {
                    _viewState.emit(ViewState.Certificate)
                } else {
                    _viewState.emit(ViewState.Error)
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
            if (pdf.doesFileExist()) {
                _viewEvent.emit(ViewEvent.ShowReplaceDialog)
            } else {
                loadFileFromUri()
            }
        }
    }

    private fun loadFileFromUri() {
        val uri = uri!!
        viewModelScope.launch {
            if (pdf.isPdfPasswordProtected(uri)) {
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
            } else {
                _viewState.emit(ViewState.Empty)
                if (pdf.copyPdfToCache(uri) && parsePdfIntoBitmap()) {
                    _viewState.emit(ViewState.Certificate)
                } else {
                    _viewState.emit(ViewState.Error)
                }
            }
        }
    }

    private suspend fun parsePdfIntoBitmap(): Boolean {
        val success = pdf.parsePdfIntoBitmap()
        if (success) {
            _bitmapState.emit(BitmapState.Ready)
        }
        return success
    }

    fun onReplaceConfirmed() {
        loadFileFromUri()
    }

    fun onPasswordEntered(password: String) {
        viewModelScope.launch {
            if (pdf.decryptAndCopyPdfToCache(uri = uri!!, password = password)) {
                _viewState.emit(ViewState.Empty)
                if (parsePdfIntoBitmap()) {
                    _viewState.emit(ViewState.Certificate)
                } else {
                    _viewState.emit(ViewState.Error)
                }
            } else {
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
            }
        }
    }

    fun onDeleteConfirmed() {
        viewModelScope.launch {
            pdf.deleteFile()
            _viewState.emit(ViewState.Empty)
        }
    }

    fun getQrBitmap() = pdf.getQrBitmap()
    fun getPdfBitmap() = pdf.getPdfBitmap()

}