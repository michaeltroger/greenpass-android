package com.michaeltroger.gruenerpass

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.michaeltroger.gruenerpass.db.AppDatabase
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.locator.Locator
import com.michaeltroger.gruenerpass.model.*
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class MainViewModel(
    private val context: Context,
    private val pdfHandler: PdfHandler = Locator.pdfHandler(context),
    private val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "greenpass"
    ).build()
): ViewModel() {
    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(ViewState.Loading)
    val viewState: StateFlow<ViewState> = _viewState

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    private var uri: Uri? = null
    private val thread = newSingleThreadContext("RenderContext")

    init {
        viewModelScope.launch {
           _viewState.emit(ViewState.Certificate(documents = db.certificateDao().getAll().first() ))
        }
    }

    fun setUri(uri: Uri) {
        this.uri = uri
        viewModelScope.launch {
            _viewEvent.emit(ViewEvent.CloseAllDialogs)
            loadFileFromUri()
        }
    }

    private suspend fun Context.getDocumentName(uri: Uri): String = withContext(Dispatchers.IO) {
        when(uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> getDocumentNameFromDb(uri)
            else -> uri.path?.let(::File)?.name
        }?.removeSuffix(".pdf")?.removeSuffix(".PDF") ?: "Certificate"
    }

    private suspend fun Context.getDocumentNameFromDb(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
            }
        }.getOrNull()
    }

    private fun loadFileFromUri() {
        val uri = uri!!
        viewModelScope.launch {
            val documentName = context.getDocumentName(uri)
            val filename = UUID.randomUUID().toString() + ".pdf"
            if (pdfHandler.isPdfPasswordProtected(uri)) {
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
            } else {
                val renderer = PdfRendererImpl(context, fileName = filename, renderContext = thread)
                if (pdfHandler.copyPdfToCache(uri, fileName = filename) && renderer.loadFile()) {
                    withContext(Dispatchers.IO) {
                        db.certificateDao().insertAll(Certificate(id = filename, name = documentName))
                        _viewState.emit(ViewState.Certificate(documents = db.certificateDao().getAll().first() ))
                    }
                } else {
                    _viewEvent.emit(ViewEvent.ErrorParsingFile)
                }
                renderer.onCleared()
            }
        }
    }

    fun onPasswordEntered(password: String) {
        viewModelScope.launch {
            val documentName = context.getDocumentName(uri!!)
            val filename = UUID.randomUUID().toString() + ".pdf"
            if (pdfHandler.decryptAndCopyPdfToCache(uri = uri!!, password = password, filename)) {
                val renderer = PdfRendererImpl(context, fileName = filename, renderContext = thread)
                if (renderer.loadFile()) {
                    withContext(Dispatchers.IO) {
                        db.certificateDao().insertAll(Certificate(id = filename, name = documentName))
                        _viewState.emit(ViewState.Certificate(documents = db.certificateDao().getAll().first() ))
                    }
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
            withContext(Dispatchers.IO) {
                db.certificateDao().updateName(id = filename, name = documentName)
            }
        }
    }

    fun onDeleteConfirmed(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.certificateDao().delete(id)
                _viewState.emit(ViewState.Certificate(documents = db.certificateDao().getAll().first() ))
            }
            pdfHandler.deleteFile(id)
        }
    }

    fun onDragFinished(sortedIdList: List<String>) {
        viewModelScope.launch {
            val originalMap = mutableMapOf<String, String>()
            withContext(Dispatchers.IO) {
                db.certificateDao().getAll().first().forEach {
                    originalMap[it.id] = it.name
                }
            }
            val sortedMap = sortedIdList.map {
                Certificate(id = it, name = originalMap[it]!!)
            }
            withContext(Dispatchers.IO) {
                db.certificateDao().deleteAll()
                db.certificateDao().insertAll(*sortedMap.toTypedArray())
            }
        }
    }

}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(context.applicationContext) as T
    }
}