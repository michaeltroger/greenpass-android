package com.michaeltroger.gruenerpass.certificates

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.certificates.mapper.toCertificate
import com.michaeltroger.gruenerpass.certificates.states.ViewEvent
import com.michaeltroger.gruenerpass.certificates.states.ViewState
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.usecase.ChangeCertificateNameUseCase
import com.michaeltroger.gruenerpass.db.usecase.ChangeCertificateOrderUseCase
import com.michaeltroger.gruenerpass.db.usecase.DeleteAllCertificatesUseCase
import com.michaeltroger.gruenerpass.db.usecase.DeleteSelectedCertificatesUseCase
import com.michaeltroger.gruenerpass.db.usecase.DeleteSingleCertificateUseCase
import com.michaeltroger.gruenerpass.db.usecase.GetCertificatesFlowUseCase
import com.michaeltroger.gruenerpass.db.usecase.InsertIntoDatabaseUseCase
import com.michaeltroger.gruenerpass.lock.AppLockedRepo
import com.michaeltroger.gruenerpass.pdfimporter.PdfImportResult
import com.michaeltroger.gruenerpass.pdfimporter.PdfImporter
import com.michaeltroger.gruenerpass.settings.getBooleanFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
@HiltViewModel
class CertificatesViewModel @Inject constructor(
    app: Application,
    private val pdfImporter: PdfImporter,
    private val insertIntoDatabaseUseCase: InsertIntoDatabaseUseCase,
    private val deleteAllCertificatesUseCase: DeleteAllCertificatesUseCase,
    private val deleteSingleCertificateUseCase: DeleteSingleCertificateUseCase,
    private val deleteSelectedCertificatesUseCase: DeleteSelectedCertificatesUseCase,
    private val changeCertificateNameUseCase: ChangeCertificateNameUseCase,
    private val changeCertificateOrderUseCase: ChangeCertificateOrderUseCase,
    private val getCertificatesFlowUseCase: GetCertificatesFlowUseCase,
    private val lockedRepo: AppLockedRepo,
    private val sharedPrefs: SharedPreferences,
): AndroidViewModel(app) {

    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(
        ViewState.Initial
    )
    val viewState: StateFlow<ViewState> = _viewState

    private val filter = MutableStateFlow("")

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    private val shouldAuthenticate =
        sharedPrefs.getBooleanFlow(
            app.getString(R.string.key_preference_biometric),
            false
        )
    private val searchForQrCode =
        sharedPrefs.getBooleanFlow(
            app.getString(R.string.key_preference_search_for_barcode),
            true
        )
    private val addDocumentsInFront =
        sharedPrefs.getBooleanFlow(
            app.getString(R.string.key_preference_add_documents_front),
            false
        )
    private val showOnLockedScreen =
        sharedPrefs.getBooleanFlow(
            app.getString(R.string.key_preference_show_on_locked_screen),
            false
        )

    init {
        viewModelScope.launch {
            combine(
                getCertificatesFlowUseCase(),
                filter,
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
        filter: String,
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
            val filteredDocs = docs.filter {
                if (filter.isEmpty()) {
                    true
                } else {
                    it.name.contains(filter, ignoreCase = true)
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

    private suspend fun processPendingFile(password: String? = null) {
        when (val result = pdfImporter.importPendingFile(password = password)) {
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

    fun onPasswordEntered(password: String) = viewModelScope.launch {
        processPendingFile(password = password)
    }

    private suspend fun insertIntoDatabase(certificate: Certificate) {
        val addDocumentsInFront = addDocumentsInFront.first()
        insertIntoDatabaseUseCase(certificate, addDocumentsInFront)
        if (addDocumentsInFront) {
            _viewEvent.emit(ViewEvent.ScrollToFirstCertificate())
        } else {
            _viewEvent.emit(ViewEvent.ScrollToLastCertificate())
        }
    }

    fun onDocumentNameChangeConfirmed(filename: String, documentName: String) = viewModelScope.launch {
         changeCertificateNameUseCase(filename, documentName)
    }

    fun onDeleteConfirmed(fileName: String) = viewModelScope.launch {
        deleteSingleCertificateUseCase(fileName)
    }

    fun onDeleteAllConfirmed() = viewModelScope.launch {
        deleteAllCertificatesUseCase()
    }

    fun onDeleteFilteredConfirmed() = viewModelScope.launch {
        val docs = (viewState.value as? ViewState.Normal)?.documents ?: return@launch
        deleteSelectedCertificatesUseCase(docs)
    }

    @Suppress("SpreadOperator")
    fun onOrderChangeConfirmed(sortedIdList: List<String>) = viewModelScope.launch {
        changeCertificateOrderUseCase(sortedIdList)
    }

    fun lockApp() = viewModelScope.launch {
        lockedRepo.lockApp()
    }

    fun onPasswordDialogAborted() {
        pdfImporter.deletePendingFile()
    }

    fun onSearchQueryChanged(query: String) = viewModelScope.launch {
        filter.value = query.trim()
    }

    fun onExportFilteredSelected() = viewModelScope.launch {
        val docs = (viewState.value as? ViewState.Normal)?.documents ?: return@launch
        _viewEvent.emit(
            ViewEvent.ShareMultiple(docs)
        )
    }

    fun onExportAllSelected() = viewModelScope.launch {
        _viewEvent.emit(
            ViewEvent.ShareMultiple(getCertificatesFlowUseCase().first())
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
            ViewEvent.ShowChangeDocumentOrderDialog(
                originalOrder = getCertificatesFlowUseCase().first()
            )
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

    fun onSwitchLayoutSelected() {
        sharedPrefs.edit {
            putBoolean(
                getApplication<Application>().getString(R.string.key_preference_show_list_layout),
                !(sharedPrefs.getBoolean(
                    getApplication<Application>().getString(R.string.key_preference_show_list_layout),
                    false
                ))
            )
        }
    }
}
