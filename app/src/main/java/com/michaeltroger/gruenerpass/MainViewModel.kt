package com.michaeltroger.gruenerpass

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.pdf.Pdf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class MainViewModel(app: Application): AndroidViewModel(app) {
    val updatedUri = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val areBitmapsReady = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    val viewState = MutableStateFlow<ViewState>(ViewState.Empty)
    val viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)

    private var runtimeUriReceived: Uri? = null

    private val pdf = Pdf(getApplication<Application>())

    init {
        viewModelScope.launch {
            if (pdf.doesFileExist()) {
                if (parsePdfIntoBitmap()) {
                    viewState.emit(ViewState.Certificate)
                } else {
                    viewState.emit(ViewState.Error)
                }
            } else {
                viewState.emit(ViewState.Empty)
            }
        }

        viewModelScope.launch {
            updatedUri.collect {
                runtimeUriReceived = it
                viewEvent.emit(ViewEvent.CloseAllDialogs)
                if (pdf.doesFileExist()) {
                    viewEvent.emit(ViewEvent.ShowReplaceDialog)
                } else {
                    handleFileFromUri(it)
                }
            }
        }
    }

    private fun handleFileFromUri(uri: Uri) {
        viewModelScope.launch {
            if (pdf.isPdfPasswordProtected(uri)) {
                viewEvent.emit(ViewEvent.ShowPasswordDialog)
            } else {
                viewState.emit(ViewState.Empty)
                if (pdf.copyPdfToCache(uri) && parsePdfIntoBitmap()) {
                    viewState.emit(ViewState.Certificate)
                } else {
                    viewState.emit(ViewState.Error)
                }
            }
        }
    }

    private suspend fun parsePdfIntoBitmap(): Boolean {
        val success = pdf.parsePdfIntoBitmap()
        if (success) {
            areBitmapsReady.emit(true)
        }
        return success
    }

    fun onReplaceConfirmed() {
        handleFileFromUri(runtimeUriReceived!!)
    }

    fun onPasswordEntered(password: String) {
        viewModelScope.launch {
            if (pdf.decryptAndCopyPdfToCache(uri = runtimeUriReceived!!, password = password)) {
                viewState.emit(ViewState.Empty)
                if (parsePdfIntoBitmap()) {
                    viewState.emit(ViewState.Certificate)
                } else {
                    viewState.emit(ViewState.Error)
                }
            } else {
                viewEvent.emit(ViewEvent.ShowPasswordDialog)
            }
        }
    }

    fun onDeleteConfirmed() {
        viewModelScope.launch {
            pdf.deleteFile()
            viewState.emit(ViewState.Empty)
        }
    }

    fun getQrBitmap() = pdf.getQrBitmap()
    fun getPdfBitmap() = pdf.getPdfBitmap()

}