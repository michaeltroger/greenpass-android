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
import com.michaeltroger.gruenerpass.logging.Logger
import com.michaeltroger.gruenerpass.logging.LoggerImpl
import com.michaeltroger.gruenerpass.model.DocumentNameRepo
import com.michaeltroger.gruenerpass.model.PdfHandler
import com.michaeltroger.gruenerpass.model.PdfRendererBuilder
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
    private val preferenceManager: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app),
    private val logger: Logger = LoggerImpl()
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
            val docs = db.getAll()
            if (docs.isEmpty()) {
                _viewState.emit(ViewState.Empty(fullBrightness = fullScreenBrightness))
            } else {
                _viewState.emit(ViewState.Normal(
                    documents = docs,
                    searchQrCode = searchForQrCode,
                    fullBrightness = fullScreenBrightness
                ))
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


    @Suppress("TooGenericExceptionCaught")
    private fun loadFileFromUri() {
        val uri = uri!!
        viewModelScope.launch {
            val filename = "${UUID.randomUUID()}.pdf"
            try {
                if (pdfHandler.isPdfPasswordProtected(uri)) {
                    _viewEvent.emit(ViewEvent.ShowPasswordDialog)
                } else {
                    pdfHandler.copyPdfToApp(uri, fileName = filename)
                    handleFileAfterCopying(filename)
                }
            } catch (e: Throwable) {
                logger.logError(e.toString())
                _viewEvent.emit(ViewEvent.ErrorParsingFile)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun onPasswordEntered(password: String) {
        viewModelScope.launch {
            val filename = "${UUID.randomUUID()}.pdf"
            try {
                pdfHandler.decryptAndCopyPdfToApp(uri = uri!!, password = password, filename)
            } catch (e: Exception) {
                logger.logError(e.toString())
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
                return@launch
            }

            handleFileAfterCopying(filename)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun handleFileAfterCopying(filename: String) {
        val renderer = PdfRendererBuilder.create(getApplication(), fileName = filename, renderContext = Dispatchers.IO)
        val loaded = try {
            renderer.loadFile()
            true
        } catch (e: Exception) {
            logger.logError(e.toString())
            _viewEvent.emit(ViewEvent.ErrorParsingFile)
            false
        } finally {
            renderer.close()
        }

        if (loaded){
            val documentName = documentNameRepo.getDocumentName(uri!!)
            db.insertAll(Certificate(id = filename, name = documentName))
            updateState()
            _viewEvent.emit(ViewEvent.ScrollToLastCertificate)
        }

        uri = null
    }

    fun onDocumentNameChanged(filename: String, documentName: String) {
        viewModelScope.launch {
            db.updateName(id = filename, name = documentName)
            updateState()
        }
    }

    fun onDeleteConfirmed(id: String) {
        viewModelScope.launch {
            db.delete(id)
            updateState()
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
            updateState()
        }
    }

    fun onAuthenticationSuccess() {
        viewModelScope.launch {
            isLocked = false
            if (uri == null) {
                updateState()
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
