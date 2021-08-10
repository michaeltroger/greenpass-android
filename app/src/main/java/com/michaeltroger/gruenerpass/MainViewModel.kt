package com.michaeltroger.gruenerpass

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
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
    app: Application,
    private val pdfHandler: PdfHandler = Locator.pdfHandler(app),
    private val db: CertificateDao = Locator.database(app).certificateDao(),
    private val documentNameRepo: DocumentNameRepo = Locator.documentNameRepo(app)
): AndroidViewModel(app) {
    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(ViewState.Loading)
    val viewState: StateFlow<ViewState> = _viewState

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    private var uri: Uri? = null

    init {
        viewModelScope.launch {
            _viewState.emit(ViewState.Certificate(documents = db.getAll() ))
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
            val filename = UUID.randomUUID().toString() + ".pdf"
            if (pdfHandler.isPdfPasswordProtected(uri)) {
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
            } else {
                if (pdfHandler.copyPdfToCache(uri, fileName = filename)) {
                    handleFileAfterCopying(filename)
                } else {
                    _viewEvent.emit(ViewEvent.ErrorParsingFile)
                }
            }
        }
    }

    fun onPasswordEntered(password: String) {
        viewModelScope.launch {
            val filename = UUID.randomUUID().toString() + ".pdf"
            if (pdfHandler.decryptAndCopyPdfToCache(uri = uri!!, password = password, filename)) {
                handleFileAfterCopying(filename)
            } else {
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
            }
        }
    }

    private suspend fun handleFileAfterCopying(filename: String) {
        val renderer = PdfRendererImpl(getApplication(), fileName = filename, renderContext = Dispatchers.IO)
        if (renderer.loadFile()) {
            val documentName = documentNameRepo.getDocumentName(uri!!)
            db.insertAll(Certificate(id = filename, name = documentName))
            _viewState.emit(ViewState.Certificate(documents = db.getAll() ))
            _viewEvent.emit(ViewEvent.ScrollToLastCertificate)
        } else {
            _viewEvent.emit(ViewEvent.ErrorParsingFile)
        }
        renderer.onCleared()
    }

    fun onDocumentNameChanged(filename: String, documentName: String) {
        viewModelScope.launch {
            db.updateName(id = filename, name = documentName)
        }
    }

    fun onDeleteConfirmed(id: String) {
        viewModelScope.launch {
            db.delete(id)
            _viewState.emit(ViewState.Certificate(documents = db.getAll() ))
            pdfHandler.deleteFile(id)
        }
    }

    fun onDragFinished(sortedIdList: List<String>) {
        viewModelScope.launch {
            val originalMap = mutableMapOf<String, String>()
            db.getAll().forEach {
                originalMap[it.id] = it.name
            }
            val sortedMap = sortedIdList.map {
                Certificate(id = it, name = originalMap[it]!!)
            }
            db.deleteAll()
            db.insertAll(*sortedMap.toTypedArray())
            _viewState.emit(ViewState.Certificate(documents = db.getAll() ))
        }
    }

}

class MainViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(app) as T
    }
}