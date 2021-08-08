package com.michaeltroger.gruenerpass

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.locator.Locator
import com.michaeltroger.gruenerpass.model.*
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.io.File
import java.util.*

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_data")

class MainViewModel(
    private val context: Context,
    private val pdfHandler: PdfHandler = Locator.pdfHandler(context)
): ViewModel() {
    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(ViewState.Loading)
    val viewState: StateFlow<ViewState> = _viewState

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    private var uri: Uri? = null
    private val thread = newSingleThreadContext("RenderContext")

    private val certificates = stringSetPreferencesKey("certificates")
    private val certificateFlow: Flow<SortedMap<String, String>> = context.dataStore.data
        .map { settings ->
            val set = settings[certificates] ?: setOf()
            val map = sortedMapOf<String, String>()
            set.forEach {
                val list = it.split(',', ignoreCase = false, limit = 2)
                map[list[0]] = list[1]
            }
            map
        }

    private suspend fun writeCertificates(id: String, name: String) {
        context.dataStore.edit { settings ->
            val certs = certificateFlow.first()
            certs[id] = name
            settings[certificates] = certs.toList().map {
                "${it.first},${it.second}"
            }.toSet()
        }
    }

    private suspend fun deleteCertificateById(id: String) {
        context.dataStore.edit { settings ->
            val certs = certificateFlow.first()
            certs.remove(id)
            settings[certificates] = certs.toList().map {
                "${it.first},${it.second}"
            }.toSet()
        }
    }

    init {
        viewModelScope.launch {
           _viewState.emit(ViewState.Certificate(documents = certificateFlow.first()))
        }
    }

    fun setUri(uri: Uri) {
        this.uri = uri
        viewModelScope.launch {
            _viewEvent.emit(ViewEvent.CloseAllDialogs)
            loadFileFromUri()
        }
    }

    private fun Context.getFileName(uri: Uri): String? = when(uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> getContentFileName(uri)
        else -> uri.path?.let(::File)?.name
    }

    private fun Context.getContentFileName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
        }
    }.getOrNull()

    private fun loadFileFromUri() {
        val uri = uri!!
        val documentName = context.getFileName(uri) ?: "Certificate"
        val filename = UUID.randomUUID().toString() + ".pdf"
        viewModelScope.launch {
            if (pdfHandler.isPdfPasswordProtected(uri)) {
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
            } else {
                val renderer = PdfRendererImpl(context, fileName = filename, renderContext = thread)
                if (pdfHandler.copyPdfToCache(uri, fileName = filename) && renderer.loadFile()) {
                    writeCertificates(id = filename, name = documentName)
                    _viewState.emit(ViewState.Certificate(documents = certificateFlow.first()))
                } else {
                    _viewEvent.emit(ViewEvent.ErrorParsingFile)
                }
                renderer.onCleared()
            }
        }
    }

    fun onPasswordEntered(password: String) {
        viewModelScope.launch {
            val documentName = context.getFileName(uri!!) ?: "Certificate"
            val filename = UUID.randomUUID().toString() + ".pdf"
            if (pdfHandler.decryptAndCopyPdfToCache(uri = uri!!, password = password, filename)) {
                val renderer = PdfRendererImpl(context, fileName = filename, renderContext = thread)
                if (renderer.loadFile()) {
                    writeCertificates(id = filename, name = documentName)
                    _viewState.emit(ViewState.Certificate(certificateFlow.first()))
                } else {
                    _viewEvent.emit(ViewEvent.ErrorParsingFile)
                }
                renderer.onCleared()
            } else {
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
            }
        }
    }

    fun onDeleteConfirmed(id: String) {
        viewModelScope.launch {
            deleteCertificateById(id)
            pdfHandler.deleteFile(id)
            _viewState.emit(ViewState.Certificate(documents = certificateFlow.first()))
        }
    }

}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(context.applicationContext) as T
    }
}