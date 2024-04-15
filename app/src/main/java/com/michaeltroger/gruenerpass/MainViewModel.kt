package com.michaeltroger.gruenerpass

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
import com.michaeltroger.gruenerpass.file.FileRepo
import com.michaeltroger.gruenerpass.logger.logging.Logger
import com.michaeltroger.gruenerpass.pdfdecryptor.PdfDecryptor
import com.michaeltroger.gruenerpass.pdfrenderer.PdfRendererBuilder
import com.michaeltroger.gruenerpass.settings.PreferenceChangeListener
import com.michaeltroger.gruenerpass.settings.PreferenceObserver
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class MainViewModel @Inject constructor(
    app: Application,
    private val pdfDecryptor: PdfDecryptor,
    private val db: CertificateDao,
    private val logger: Logger,
    private val fileRepo: FileRepo,
    private val preferenceObserver: PreferenceObserver
): AndroidViewModel(app), PreferenceChangeListener {

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
                val areDocumentsFilteredOut = filteredDocs.size != docs.size
                _viewState.emit(ViewState.Normal(
                    documents = filteredDocs,
                    searchBarcode = preferenceObserver.searchForBarcode(),
                    showLockMenuItem = shouldAuthenticate,
                    showScrollToFirstMenuItem = filteredDocs.size > 1,
                    showScrollToLastMenuItem = filteredDocs.size > 1,
                    showChangeOrderMenuItem = !areDocumentsFilteredOut && docs.size > 1,
                    showSearchMenuItem = docs.size > 1,
                    filter = filter,
                    showWarningButton = preferenceObserver.showOnLockedScreen(),
                    showExportFilteredMenuItem = areDocumentsFilteredOut,
                    showDeleteFilteredMenuItem = areDocumentsFilteredOut,
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
                _viewEvent.emit(ViewEvent.ShowParsingFileError)
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
            _viewEvent.emit(ViewEvent.ShowParsingFileError)
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
            _viewEvent.emit(ViewEvent.ScrollToFirstCertificate())
        } else {
            _viewEvent.emit(ViewEvent.ScrollToLastCertificate())
        }
    }

    fun onDocumentNameChangeConfirmed(filename: String, documentName: String) {
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

    fun onDeleteAllConfirmed() = viewModelScope.launch {
        val certificates = db.getAll()
        db.deleteAll()
        updateState()
        certificates.forEach {
            fileRepo.deleteFile(it.id)
        }
    }

    fun onDeleteFilteredConfirmed() = viewModelScope.launch {
        val docs = (viewState.value as? ViewState.Normal)?.documents ?: return@launch
        docs.forEach {
            db.delete(it.id)
            fileRepo.deleteFile(it.id)
        }
        updateState()
    }

    @Suppress("SpreadOperator")
    fun onOrderChangeConfirmed(sortedIdList: List<String>) {
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

    override fun onCleared() {
        super.onCleared()
        preferenceObserver.onDestroy()
    }

    fun onExportFilteredSelected() = viewModelScope.launch {
        val docs = (viewState.value as? ViewState.Normal)?.documents ?: return@launch
        _viewEvent.emit(
            ViewEvent.ShareMultiple(docs)
        )
    }

    fun onExportAllSelected() = viewModelScope.launch {
        _viewEvent.emit(
            ViewEvent.ShareMultiple(db.getAll())
        )
    }

    fun onDeleteFilteredSelected() = viewModelScope.launch {
        val docsSize = (viewState.value as? ViewState.Normal)?.documents?.size ?: return@launch
        _viewEvent.emit(
            ViewEvent.ShowDeleteFilteredDialog(documentCountToBeDeleted = docsSize)
        )
    }

    fun onDeleteAllSelected() = viewModelScope.launch {
        _viewEvent.emit(
            ViewEvent.ShowDeleteAllDialog
        )
    }

    fun onScrollToFirstSelected() = viewModelScope.launch {
        _viewEvent.emit(
            ViewEvent.ScrollToFirstCertificate(0)
        )
    }

    fun onScrollToLastSelected() = viewModelScope.launch {
        _viewEvent.emit(
            ViewEvent.ScrollToLastCertificate(0)
        )
    }

    fun onChangeOrderSelected() = viewModelScope.launch {
        val docs = (viewState.value as? ViewState.Normal)?.documents ?: return@launch
        _viewEvent.emit(
            ViewEvent.ShowChangeDocumentOrderDialog(originalOrder = docs)
        )
    }

    fun onShowWarningDialogSelected() = viewModelScope.launch {
        _viewEvent.emit(
            ViewEvent.ShowWarningDialog
        )
    }

    fun onShowSettingsSelected() = viewModelScope.launch {
        _viewEvent.emit(
            ViewEvent.ShowSettingsScreen
        )
    }

    fun onShowMoreSelected() = viewModelScope.launch {
        _viewEvent.emit(
            ViewEvent.ShowMoreScreen
        )
    }

    fun onAddFileSelected() = viewModelScope.launch {
        _viewEvent.emit(
            ViewEvent.AddFile
        )
    }

    fun onDeleteCalled(id: String) = viewModelScope.launch {
        _viewEvent.emit(
            ViewEvent.ShowDeleteDialog(
                id = id,
            )
        )
    }

    fun onChangeDocumentNameSelected(id: String, name: String) = viewModelScope.launch {
        _viewEvent.emit(
            ViewEvent.ShowChangeDocumentNameDialog(
                id = id,
                originalName = name
            )
        )
    }

    fun onShareSelected(certificate: Certificate) = viewModelScope.launch {
        _viewEvent.emit(
            ViewEvent.Share(certificate)
        )
    }
}
