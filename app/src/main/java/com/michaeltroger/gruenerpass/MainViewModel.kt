package com.michaeltroger.gruenerpass

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
import com.michaeltroger.gruenerpass.locator.Locator
import com.michaeltroger.gruenerpass.model.DocumentNameRepo
import com.michaeltroger.gruenerpass.model.PdfHandler
import com.michaeltroger.gruenerpass.model.PdfRendererImpl
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class MainViewModel(
    app: Application,
    private val pdfHandler: PdfHandler = Locator.pdfHandler(app),
    private val db: CertificateDao = Locator.database(app).certificateDao(),
    private val documentNameRepo: DocumentNameRepo = Locator.documentNameRepo(app),
    private val preferenceManager: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)
): AndroidViewModel(app), SharedPreferences.OnSharedPreferenceChangeListener {

    private var fullScreenBrightness: Boolean = false
    private var searchForQrCode: Boolean = true
    private var shouldAuthenticate = false

    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(
        ViewState.Loading(fullBrightness = fullScreenBrightness)
    )
    val viewState: StateFlow<ViewState> = _viewState

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    private var isLocked: Boolean = false

    private var uri: Uri? = null

    init {
        preferenceManager.registerOnSharedPreferenceChangeListener(this)
        viewModelScope.launch {
            shouldAuthenticate = preferenceManager.getBoolean(
                app.getString(R.string.key_preference_biometric),
                false
            )
            isLocked = shouldAuthenticate
            searchForQrCode = preferenceManager.getBoolean(
                app.getString(R.string.key_preference_search_for_qr_code),
                true
            )
            fullScreenBrightness = preferenceManager.getBoolean(
                app.getString(R.string.key_preference_full_brightness),
                false
            )
            updateState()
        }
    }

    private suspend fun updateState() {
        if (shouldAuthenticate && isLocked) {
            _viewState.emit(ViewState.Locked(fullBrightness = fullScreenBrightness))
        } else {
            _viewState.emit(ViewState.Normal(
                documents = db.getAll(),
                searchQrCode = searchForQrCode,
                fullBrightness = fullScreenBrightness
            ))
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
            _viewState.emit(ViewState.Normal(
                documents = db.getAll(),
                searchQrCode = searchForQrCode,
                fullBrightness = fullScreenBrightness
            ))
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
            _viewState.emit(ViewState.Normal(
                documents = db.getAll(),
                searchQrCode = searchForQrCode,
                fullBrightness = fullScreenBrightness
            ))
        }
    }

    fun onDeleteConfirmed(id: String) {
        viewModelScope.launch {
            db.delete(id)
            _viewState.emit(ViewState.Normal(
                documents = db.getAll(),
                searchQrCode = searchForQrCode,
                fullBrightness = fullScreenBrightness
            ))
            pdfHandler.deleteFile(id)
        }
    }

    @Suppress("SpreadOperator")
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
            _viewState.emit(ViewState.Normal(
                documents = sortedList,
                searchQrCode = searchForQrCode,
                fullBrightness = fullScreenBrightness
            ))
        }
    }

    fun onAuthenticationSuccess() {
        viewModelScope.launch {
            isLocked = false
            if (uri == null) {
                _viewState.emit(ViewState.Normal(
                    documents = db.getAll(),
                    searchQrCode = searchForQrCode,
                    fullBrightness = fullScreenBrightness
                ))
            } else {
                loadFileFromUri()
            }
        }
    }

    fun onInteractionTimeout() {
        if (shouldAuthenticate) {
            isLocked = true
            viewModelScope.launch {
                updateState()
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            getApplication<Application>().getString(R.string.key_preference_biometric) -> {
                shouldAuthenticate = sharedPreferences.getBoolean(key, false)
            }
            getApplication<Application>().getString(R.string.key_preference_search_for_qr_code) -> {
                searchForQrCode = sharedPreferences.getBoolean(key, true)
            }
            getApplication<Application>().getString(R.string.key_preference_full_brightness) -> {
                fullScreenBrightness = sharedPreferences.getBoolean(key, false)
            }
        }

        viewModelScope.launch {
            updateState()
        }
    }
}

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(app) as T
    }
}
