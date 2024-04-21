package com.michaeltroger.gruenerpass.certificate

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.certificate.file.FileRepo
import com.michaeltroger.gruenerpass.certificate.mapper.toCertificate
import com.michaeltroger.gruenerpass.certificate.states.ViewEvent
import com.michaeltroger.gruenerpass.certificate.states.ViewState
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
import com.michaeltroger.gruenerpass.lock.AppLockedRepo
import com.michaeltroger.gruenerpass.pdfimporter.PdfImportResult
import com.michaeltroger.gruenerpass.pdfimporter.PdfImporter
import com.michaeltroger.gruenerpass.settings.getBooleanFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

@Suppress("TooManyFunctions")
@HiltViewModel
class CertificateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: CertificateDao,
    private val fileRepo: FileRepo,
    private val pdfImporter: PdfImporter,
    private val lockedRepo: AppLockedRepo,
    sharedPrefs: SharedPreferences,
): ViewModel() {

    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(
        ViewState.Initial
    )
    val viewState: StateFlow<ViewState> = _viewState
    private var filter = ""

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    private val shouldAuthenticate =
        sharedPrefs.getBooleanFlow(
            context.getString(R.string.key_preference_biometric),
            false
        )
    private val searchForQrCode =
        sharedPrefs.getBooleanFlow(
            context.getString(R.string.key_preference_search_for_barcode),
            true
        )
    private val addDocumentsInFront =
        sharedPrefs.getBooleanFlow(
            context.getString(R.string.key_preference_add_documents_front),
            false
        )
    private val showOnLockedScreen =
        sharedPrefs.getBooleanFlow(
            context.getString(R.string.key_preference_show_on_locked_screen),
            false
        )

    init {
        viewModelScope.launch {
            combine(
                db.getAll(),
                shouldAuthenticate,
                searchForQrCode,
                showOnLockedScreen,
                ::updateState
            ).collect()
        }
        viewModelScope.launch {
            pdfImporter.hasPendingFile().filter { it }.collect {
                _viewEvent.emit(ViewEvent.CloseAllDialogs)
                processPendingFile()
            }
        }
    }

    private suspend fun updateState(
        docs: List<Certificate>,
        shouldAuthenticate: Boolean,
        searchForBarcode: Boolean,
        showOnLockedScreen: Boolean,
    ) {
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
                    searchBarcode = searchForBarcode,
                    showLockMenuItem = shouldAuthenticate,
                    showScrollToFirstMenuItem = filteredDocs.size > 1,
                    showScrollToLastMenuItem = filteredDocs.size > 1,
                    showChangeOrderMenuItem = !areDocumentsFilteredOut && docs.size > 1,
                    showSearchMenuItem = docs.size > 1,
                    filter = filter,
                    showWarningButton = showOnLockedScreen,
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
        val addDocumentsInFront = addDocumentsInFront.first()
        if (addDocumentsInFront) {
            val all = listOf(certificate) + db.getAll().first()
            db.replaceAll(*all.toTypedArray())
        } else {
            db.insertAll(certificate)
        }

        if (addDocumentsInFront) {
            _viewEvent.emit(ViewEvent.ScrollToFirstCertificate())
        } else {
            _viewEvent.emit(ViewEvent.ScrollToLastCertificate())
        }
    }

    fun onDocumentNameChangeConfirmed(filename: String, documentName: String) {
        viewModelScope.launch {
            db.updateName(id = filename, name = documentName)
        }
    }

    fun onDeleteConfirmed(fileName: String) {
        viewModelScope.launch {
            db.delete(fileName)
            fileRepo.deleteFile(fileName)
        }
    }

    fun onDeleteAllConfirmed() = viewModelScope.launch {
        val certificates = db.getAll().first()
        db.deleteAll()
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
    }

    @Suppress("SpreadOperator")
    fun onOrderChangeConfirmed(sortedIdList: List<String>) {
        viewModelScope.launch {
            val originalMap = mutableMapOf<String, String>()
            db.getAll().first().forEach {
                originalMap[it.id] = it.name
            }
            val sortedList: List<Certificate> = sortedIdList.map {
                Certificate(id = it, name = originalMap[it]!!)
            }
            db.replaceAll(*sortedList.toTypedArray())
        }
    }

    fun lockApp() {
        viewModelScope.launch {
            lockedRepo.lockApp()
        }
    }

    fun onPasswordDialogAborted() {
        pdfImporter.deletePendingFile()
    }

    fun onSearchQueryChanged(query: String) {
        viewModelScope.launch {
            filter = query
            //updateState()
        }
    }

    fun onExportFilteredSelected() = viewModelScope.launch {
        val docs = (viewState.value as? ViewState.Normal)?.documents ?: return@launch
        _viewEvent.emit(
            ViewEvent.ShareMultiple(docs)
        )
    }

    fun onExportAllSelected() = viewModelScope.launch {
        _viewEvent.emit(
            ViewEvent.ShareMultiple(db.getAll().first())
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
        _viewEvent.emit(
            ViewEvent.ShowChangeDocumentOrderDialog(originalOrder = db.getAll().first())
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
