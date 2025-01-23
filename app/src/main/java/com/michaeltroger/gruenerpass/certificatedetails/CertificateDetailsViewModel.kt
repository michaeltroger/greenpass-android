package com.michaeltroger.gruenerpass.certificatedetails

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.certificatedetails.states.DetailsViewState
import com.michaeltroger.gruenerpass.certificates.states.ViewEvent
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.usecase.ChangeCertificateNameUseCase
import com.michaeltroger.gruenerpass.db.usecase.DeleteSingleCertificateUseCase
import com.michaeltroger.gruenerpass.db.usecase.GetSingleCertificateFlowUseCase
import com.michaeltroger.gruenerpass.settings.BarcodeSearchMode
import com.michaeltroger.gruenerpass.settings.getFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class CertificateDetailsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val deleteSingleCertificateUseCase: DeleteSingleCertificateUseCase,
    private val changeCertificateNameUseCase: ChangeCertificateNameUseCase,
    private val getSingleCertificateFlowUseCase: GetSingleCertificateFlowUseCase,
    sharedPrefs: SharedPreferences,
    savedStateHandle: SavedStateHandle,
): ViewModel() {

    private val _viewState: MutableStateFlow<DetailsViewState> = MutableStateFlow(
        DetailsViewState.Initial
    )
    val viewState: StateFlow<DetailsViewState> = _viewState

    private val id: String = CertificateDetailsFragmentArgs.fromSavedStateHandle(savedStateHandle).id

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    private val searchForBarcode =
        sharedPrefs.getFlow(
            context.getString(R.string.key_preference_extract_barcodes),
            context.getString(R.string.key_preference_barcodes_extended)
        ) { value: String ->
            BarcodeSearchMode.fromPrefValue(value)
        }

    init {
        viewModelScope.launch {
            combine(
                getSingleCertificateFlowUseCase(id),
                searchForBarcode,
                ::updateState
            ).collect()
        }
    }

    private suspend fun updateState(
        document: Certificate?,
        searchForBarcode: BarcodeSearchMode,
    ) {
        if (document == null) {
            _viewState.emit(DetailsViewState.Deleted)
        } else {
            _viewState.emit(
                DetailsViewState.Normal(
                    document = document,
                    searchBarcode = searchForBarcode,
                )
            )
        }
    }

    fun onDocumentNameChangeConfirmed(filename: String, documentName: String) = viewModelScope.launch {
         changeCertificateNameUseCase(filename, documentName)
    }

    fun onDeleteConfirmed(fileName: String) = viewModelScope.launch {
        deleteSingleCertificateUseCase(fileName)
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
