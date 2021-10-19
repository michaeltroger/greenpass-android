package com.michaeltroger.gruenerpass

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
import com.michaeltroger.gruenerpass.locator.Locator
import com.michaeltroger.gruenerpass.model.PdfHandler
import com.michaeltroger.gruenerpass.model.PdfRendererImpl
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(
    app: Application,
    private val pdfHandler: PdfHandler = Locator.pdfHandler(app),
    private val db: CertificateDao = Locator.database(app).certificateDao(),
    private val documentNameRepo: DocumentNameRepo = Locator.documentNameRepo(app),
    private val preferenceManager: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)
): AndroidViewModel(app), SharedPreferences.OnSharedPreferenceChangeListener {
    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(ViewState.Loading)
    val viewState: StateFlow<ViewState> = _viewState

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent
    private var shouldAuthenticate = false
    private var offerAppSettings = false

    private var uri: Uri? = null

    init {
        preferenceManager.registerOnSharedPreferenceChangeListener(this)
        viewModelScope.launch {
            offerAppSettings = BiometricManager.from(app)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
            shouldAuthenticate = preferenceManager.getBoolean(app.getString(R.string.key_preference_biometric), false)
            if (shouldAuthenticate) {
                _viewState.emit(ViewState.Locked)
            } else {
                _viewState.emit(ViewState.Normal(documents = db.getAll(), offerAppSettings = offerAppSettings))
            }
        }
    }

    fun setUri(uri: Uri) {
        this.uri = uri
        viewModelScope.launch {
            val state = viewState.filter {
                it !is ViewState.Loading
            }.first() // wait for initial loading to be finished

            if (state !is ViewState.Locked) {
                _viewEvent.emit(ViewEvent.CloseAllDialogs)
                loadFileFromUri()
            }
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
            renderer.close()
            val documentName = documentNameRepo.getDocumentName(uri!!)
            db.insertAll(Certificate(id = filename, name = documentName))
            _viewState.emit(ViewState.Normal(documents = db.getAll(), offerAppSettings = offerAppSettings ))
            _viewEvent.emit(ViewEvent.ScrollToLastCertificate)
        } else {
            renderer.close()
            _viewEvent.emit(ViewEvent.ErrorParsingFile)
        }
        uri = null
    }

    fun onDocumentNameChanged(filename: String, documentName: String) {
        viewModelScope.launch {
            db.updateName(id = filename, name = documentName)
            _viewState.emit(ViewState.Normal(documents = db.getAll(), offerAppSettings = offerAppSettings ))
        }
    }

    fun onDeleteConfirmed(id: String) {
        viewModelScope.launch {
            db.delete(id)
            _viewState.emit(ViewState.Normal(documents = db.getAll(), offerAppSettings = offerAppSettings ))
            pdfHandler.deleteFile(id)
        }
    }

    fun onDragFinished(sortedIdList: List<String>) {
        viewModelScope.launch {
            val originalMap = mutableMapOf<String, String>()
            db.getAll().forEach {
                originalMap[it.id] = it.name
            }
            val sortedList: List<Certificate> = sortedIdList.map {
                Certificate(id = it, name = originalMap[it]!!)
            }
            db.replaceAll(*sortedList.toTypedArray())
            _viewState.emit(ViewState.Normal(documents = sortedList, offerAppSettings = offerAppSettings ))
        }
    }

    fun onAuthenticationSuccess() {
        viewModelScope.launch {
            if (uri == null) {
                _viewState.emit(ViewState.Normal(documents = db.getAll(), offerAppSettings = offerAppSettings ))
            } else {
                loadFileFromUri()
            }
        }
    }

    fun onInteractionTimeout() {
        if (shouldAuthenticate) {
            viewModelScope.launch {
                _viewState.emit(ViewState.Locked)
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        shouldAuthenticate = sharedPreferences.getBoolean(getApplication<Application>().getString(R.string.key_preference_biometric), false)
    }

}

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(app) as T
    }
}