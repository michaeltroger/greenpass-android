package com.michaeltroger.gruenerpass.certificate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.certificate.file.FileRepo
import com.michaeltroger.gruenerpass.certificate.mapper.toCertificate
import com.michaeltroger.gruenerpass.certificate.states.ViewEvent
import com.michaeltroger.gruenerpass.certificate.states.ViewState
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
import com.michaeltroger.gruenerpass.lock.AppLockedRepo
import com.michaeltroger.gruenerpass.pdfimporter.PdfImportResult
import com.michaeltroger.gruenerpass.pdfimporter.PdfImporter
import com.michaeltroger.gruenerpass.settings.PreferenceChangeListener
import com.michaeltroger.gruenerpass.settings.PreferenceObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class CertificateViewModel @Inject constructor(
    private val db: CertificateDao,
    private val fileRepo: FileRepo,
    private val pdfImporter: PdfImporter,
    private val lockedRepo: AppLockedRepo,
    private val preferenceObserver: PreferenceObserver
): ViewModel(), PreferenceChangeListener {

    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(
        ViewState.Initial
    )
    val viewState: StateFlow<ViewState> = _viewState
    private var filter = ""

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    init {
        viewModelScope.launch {
            preferenceObserver.init(this@CertificateViewModel)
            updateState()
        }
        viewModelScope.launch {
            pdfImporter.hasPendingFile().filter { it }.collect {
                _viewEvent.emit(ViewEvent.CloseAllDialogs)
                processPendingFile()
            }
        }
    }

    private suspend fun updateState() {
        val shouldAuthenticate = preferenceObserver.shouldAuthenticate()
        val docs = db.getAll()
        if (docs.isEmpty()) {
            _viewState.emit(
                ViewState.Empty(
                    showLockMenuItem = shouldAuthenticate,
                )
            )
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
            _viewState.emit(
                ViewState.Normal(
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
                )
            )
        }
    }

    private suspend fun processPendingFile() {
        when (val result = pdfImporter.importPdf()) {
            PdfImportResult.ParsingError -> {
                _viewEvent.emit(ViewEvent.ShowParsingFileError)
            }
            is PdfImportResult.PasswordRequired -> {
                _viewEvent.emit(ViewEvent.ShowPasswordDialog)
            }
            is PdfImportResult.Success -> {
                insertIntoDatabase(result.pendingCertificate.toCertificate())
            }
            PdfImportResult.NoFileToImport -> {
                // ignore
            }
        }
    }

    fun onPasswordEntered(password: String) {
        viewModelScope.launch {
            when (val result = pdfImporter.importPasswordProtectedPdf(password)) {
                PdfImportResult.ParsingError -> {
                    _viewEvent.emit(ViewEvent.ShowParsingFileError)
                }
                is PdfImportResult.PasswordRequired -> {
                    _viewEvent.emit(ViewEvent.ShowPasswordDialog)
                }
                is PdfImportResult.Success -> {
                    insertIntoDatabase(result.pendingCertificate.toCertificate())
                }
                PdfImportResult.NoFileToImport -> {
                    // ignore
                }
            }
        }
    }

    @Suppress("SpreadOperator")
    private suspend fun insertIntoDatabase(certificate: Certificate) {
        val addDocumentsInFront = preferenceObserver.addDocumentsInFront()
        if (addDocumentsInFront) {
            val all = listOf(certificate) + db.getAll()
            db.replaceAll(*all.toTypedArray())
        } else {
            db.insertAll(certificate)
        }
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

    fun lockApp() {
        viewModelScope.launch {
            lockedRepo.lockApp()
        }
    }

    override fun refreshUi() {
        viewModelScope.launch {
            updateState()
        }
    }

    fun onPasswordDialogAborted() {
        pdfImporter.deletePendingFile()
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
