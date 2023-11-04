package com.michaeltroger.gruenerpass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
import com.michaeltroger.gruenerpass.file.FileRepo
import com.michaeltroger.gruenerpass.locator.Locator
import com.michaeltroger.gruenerpass.logging.Logger
import com.michaeltroger.gruenerpass.pdf.PdfDecryptor
import com.michaeltroger.gruenerpass.pdf.PdfRendererBuilder
import com.michaeltroger.gruenerpass.settings.PreferenceListener
import com.michaeltroger.gruenerpass.settings.PreferenceManager
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
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
    private val pdfDecryptor: PdfDecryptor = Locator.pdfDecryptor(),
    private val db: CertificateDao = Locator.database(app).certificateDao(),
    private val logger: Logger = Locator.logger(),
    private val fileRepo: FileRepo = Locator.fileRepo(app),
    private val preferenceManager: PreferenceManager = Locator.preferenceManager(app)
): AndroidViewModel(app), PreferenceListener {

    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(
        ViewState.Initial(
            fullBrightness = preferenceManager.fullScreenBrightness(),
            showOnLockedScreen = preferenceManager.showOnLockedScreen()
        )
    )
    val viewState: StateFlow<ViewState> = _viewState
    var filter = ""

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    private var isLocked: Boolean = false

    private var pendingFile: Certificate? = null

    init {
        viewModelScope.launch {
            preferenceManager.init(this@MainViewModel)
            isLocked = preferenceManager.shouldAuthenticate()
            updateState()
        }
    }

    private suspend fun updateState() {
        val fullScreenBrightness = preferenceManager.fullScreenBrightness()
        val showOnLockedScreen = preferenceManager.showOnLockedScreen()
        val shouldAuthenticate = preferenceManager.shouldAuthenticate()

        if (shouldAuthenticate && isLocked) {
            _viewState.emit(ViewState.Locked(
                fullBrightness = fullScreenBrightness,
                showOnLockedScreen = showOnLockedScreen
            ))
        } else {
            val docs = db.getAll()
            if (docs.isEmpty()) {
                _viewState.emit(ViewState.Empty(
                    fullBrightness = fullScreenBrightness,
                    showLockMenuItem = shouldAuthenticate,
                    showOnLockedScreen = showOnLockedScreen
                ))
            } else {
                val filteredDocs = docs.filter {
                    if (filter.isEmpty()) {
                        true
                    } else {
                        it.name.contains(filter, ignoreCase = true)
                    }
                }
                _viewState.emit(ViewState.Normal(
                    documents = filteredDocs,
                    searchQrCode = preferenceManager.searchForQrCode(),
                    fullBrightness = fullScreenBrightness,
                    showLockMenuItem = shouldAuthenticate,
                    showScrollToFirstMenuItem = filteredDocs.size > 1,
                    showScrollToLastMenuItem = filteredDocs.size > 1,
                    showOnLockedScreen = showOnLockedScreen,
                    showDragButtons = filteredDocs.size == docs.size && docs.size > 1
                ))
            }
        }
    }

    fun setPendingFile(file: Certificate) {
        logger.logDebug(file)
        this.pendingFile = file
        viewModelScope.launch {
            val state = viewState.filter {
                it !is ViewState.Initial
            }.first() // wait for initial loading to be finished

            if (state !is ViewState.Locked) {
                _viewEvent.emit(ViewEvent.CloseAllDialogs)
                processPendingFile()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processPendingFile() {
        val pendingFile = pendingFile!!
        viewModelScope.launch {
            try {
                val file = fileRepo.getFile(pendingFile.id)
                if (pdfDecryptor.isPdfPasswordProtected(file)) {
                    _viewEvent.emit(ViewEvent.ShowPasswordDialog)
                } else {
                    insertIntoDatabaseIfValidPdf()
                }
            } catch (e: Throwable) {
                logger.logError(e.toString())
                _viewEvent.emit(ViewEvent.ErrorParsingFile)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun onPasswordEntered(password: String) {
        val pendingFile = pendingFile!!
        viewModelScope.launch {
            try {
                val file = fileRepo.getFile(pendingFile.id)
                pdfDecryptor.decrypt(password = password, file = file)
            } catch (e: Exception) {
                logger.logError(e.toString())
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
                return@launch
            }

            insertIntoDatabaseIfValidPdf()
        }
    }

    @Suppress("TooGenericExceptionCaught", "SpreadOperator")
    private suspend fun insertIntoDatabaseIfValidPdf() {
        val pendingFile = pendingFile!!
        val renderer = PdfRendererBuilder.create(
            getApplication(),
            fileName = pendingFile.id,
            renderContext = Dispatchers.IO
        )
        try {
            renderer.loadFile()
        } catch (e: Exception) {
            logger.logError(e.toString())
            _viewEvent.emit(ViewEvent.ErrorParsingFile)
            fileRepo.deleteFile(pendingFile.id)
            this.pendingFile = null
            return
        } finally {
            renderer.close()
        }

        val addDocumentsInFront = preferenceManager.addDocumentsInFront()
        if (addDocumentsInFront) {
            val all = listOf(pendingFile) + db.getAll()
            db.replaceAll(*all.toTypedArray())
        } else {
            db.insertAll(pendingFile)
        }
        this.pendingFile = null
        updateState()

        if (addDocumentsInFront) {
            _viewEvent.emit(ViewEvent.ScrollToFirstCertificate)
        } else {
            _viewEvent.emit(ViewEvent.ScrollToLastCertificate)
        }
    }

    fun onDocumentNameChanged(filename: String, documentName: String) {
        viewModelScope.launch {
            db.updateName(id = filename, name = documentName)
            updateState()
        }
    }

    fun onDeleteConfirmed(fileName: String) {
        viewModelScope.launch {
            db.delete(fileName)
            updateState()
            fileRepo.deleteFile(fileName)
        }
    }

    fun onDeleteAllConfirmed() {
        viewModelScope.launch {
            val certificates = db.getAll()
            db.deleteAll()
            updateState()
            certificates.forEach {
                fileRepo.deleteFile(it.id)
            }
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
            if (pendingFile == null) {
                updateState()
            } else {
                processPendingFile()
            }
        }
    }

    fun onInteractionTimeout() {
        if (preferenceManager.shouldAuthenticate()) {
            lockApp()
        }
    }

    fun lockApp() {
        isLocked = true
        viewModelScope.launch {
            updateState()
        }
    }

    override fun onPreferenceChanged() {
        viewModelScope.launch {
            updateState()
        }
    }

    fun deletePendingFileIfExists() {
        pendingFile?.let {
            pendingFile = null
            fileRepo.deleteFile(it.id)
        }
    }

    fun onSearchQueryChanged(query: String) {
        viewModelScope.launch {
            filter = query
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
