package com.michaeltroger.gruenerpass

import android.app.Application
import android.net.Uri
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
import com.michaeltroger.gruenerpass.settings.PreferenceObserver
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
    private val preferenceObserver: PreferenceObserver = Locator.preferenceManager(app)
): AndroidViewModel(app), PreferenceListener {

    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(
        ViewState.Initial
    )
    val viewState: StateFlow<ViewState> = _viewState
    private var filter = ""

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    private var isLocked: Boolean = false

    private var pendingFile: Certificate? = null

    init {
        viewModelScope.launch {
            preferenceObserver.init(this@MainViewModel)
            isLocked = preferenceObserver.shouldAuthenticate()
            updateState()
        }
    }

    private suspend fun updateState() {
        val shouldAuthenticate = preferenceObserver.shouldAuthenticate()

        if (shouldAuthenticate && isLocked) {
            _viewState.emit(ViewState.Locked)
        } else {
            val docs = db.getAll()
            if (docs.isEmpty()) {
                _viewState.emit(ViewState.Empty(
                    showLockMenuItem = shouldAuthenticate,
                ))
            } else {
                val filter = filter
                val filteredDocs = docs.filter {
                    if (filter.isEmpty()) {
                        true
                    } else {
                        it.name.contains(filter.trim(), ignoreCase = true)
                    }
                }
                _viewState.emit(ViewState.Normal(
                    documents = filteredDocs,
                    searchQrCode = preferenceObserver.searchForQrCode(),
                    showLockMenuItem = shouldAuthenticate,
                    showScrollToFirstMenuItem = filteredDocs.size > 1,
                    showScrollToLastMenuItem = filteredDocs.size > 1,
                    showDragButtons = filteredDocs.size == docs.size && docs.size > 1,
                    showSearchMenuItem = docs.size > 1,
                    filter = filter
                ))
            }
        }
    }

    fun setPendingFile(uri: Uri) {
        viewModelScope.launch {
            val file = fileRepo.copyToApp(uri)
            logger.logDebug(file)
            pendingFile = file

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

        val addDocumentsInFront = preferenceObserver.addDocumentsInFront()
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
        if (preferenceObserver.shouldAuthenticate()) {
            lockApp()
        }
    }

    fun lockApp() {
        isLocked = true
        viewModelScope.launch {
            updateState()
        }
    }

    override fun refreshUi() {
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
