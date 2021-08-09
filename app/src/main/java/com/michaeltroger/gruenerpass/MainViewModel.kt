package com.michaeltroger.gruenerpass

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateRepo
import com.michaeltroger.gruenerpass.locator.Locator
import com.michaeltroger.gruenerpass.model.PdfHandler
import com.michaeltroger.gruenerpass.model.PdfRendererImpl
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(
    private val context: Context,
    private val pdfHandler: PdfHandler = Locator.pdfHandler(context),
    private val certificateRepo: CertificateRepo = Locator.certificateRepo(context),
    private val documentNameRepo: DocumentNameRepo = Locator.documentNameRepo(context)
): ViewModel() {
    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(ViewState.Loading)
    val viewState: StateFlow<ViewState> = _viewState

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    private var uri: Uri? = null

    init {
        viewModelScope.launch {
            _viewState.emit(ViewState.Certificate(documents = certificateRepo.getAll() ))
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
            val documentName = documentNameRepo.getDocumentName(uri)
            val filename = UUID.randomUUID().toString() + ".pdf"
            if (pdfHandler.isPdfPasswordProtected(uri)) {
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
            } else {
                val renderer = PdfRendererImpl(context, fileName = filename, renderContext = Dispatchers.IO)
                if (pdfHandler.copyPdfToCache(uri, fileName = filename) && renderer.loadFile()) {
                    certificateRepo.insert(Certificate(id = filename, name = documentName))
                    _viewState.emit(ViewState.Certificate(documents = certificateRepo.getAll() ))
                } else {
                    _viewEvent.emit(ViewEvent.ErrorParsingFile)
                }
                renderer.onCleared()
            }
        }
    }

    fun onPasswordEntered(password: String) {
        viewModelScope.launch {
            val documentName = documentNameRepo.getDocumentName(uri!!)
            val filename = UUID.randomUUID().toString() + ".pdf"
            if (pdfHandler.decryptAndCopyPdfToCache(uri = uri!!, password = password, filename)) {
                val renderer = PdfRendererImpl(context, fileName = filename, renderContext = Dispatchers.IO)
                if (renderer.loadFile()) {
                    certificateRepo.insert(Certificate(id = filename, name = documentName))
                    _viewState.emit(ViewState.Certificate(documents = certificateRepo.getAll() ))
                } else {
                    _viewEvent.emit(ViewEvent.ErrorParsingFile)
                }
                renderer.onCleared()
            } else {
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
            }
        }
    }

    fun onDocumentNameChanged(filename: String, documentName: String) {
        viewModelScope.launch {
            certificateRepo.updateName(id = filename, name = documentName)
        }
    }

    fun onDeleteConfirmed(id: String) {
        viewModelScope.launch {
            certificateRepo.delete(id)
            _viewState.emit(ViewState.Certificate(documents = certificateRepo.getAll() ))
            pdfHandler.deleteFile(id)
        }
    }

    fun onDragFinished(sortedIdList: List<String>) {
        viewModelScope.launch {
            val originalMap = mutableMapOf<String, String>()
            certificateRepo.getAll().forEach {
                originalMap[it.id] = it.name
            }
            val sortedMap = sortedIdList.map {
                Certificate(id = it, name = originalMap[it]!!)
            }
           certificateRepo.replaceAll(sortedMap)
        }
    }

}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(context.applicationContext) as T
    }
}