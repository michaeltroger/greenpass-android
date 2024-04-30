package com.michaeltroger.gruenerpass

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.michaeltroger.gruenerpass.lock.AppLockedRepo
import com.michaeltroger.gruenerpass.navigation.GetAutoRedirectDestinationUseCase
import com.michaeltroger.gruenerpass.pdfimporter.PdfImportResult
import com.michaeltroger.gruenerpass.pdfimporter.PdfImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val pdfImporter: PdfImporter,
    private val lockedRepo: AppLockedRepo,
    private val getAutoRedirectDestinationUseCase: GetAutoRedirectDestinationUseCase
): ViewModel() {

    private val _viewEvent = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 1)
    val viewEvent: SharedFlow<ViewEvent> = _viewEvent

    fun getAutoRedirectDestination(navController: NavController)
        = getAutoRedirectDestinationUseCase(navController)

    fun setPendingFile(uri: Uri) = viewModelScope.launch {
        when (pdfImporter.preparePendingFile(uri)) {
            is PdfImportResult.ParsingError -> _viewEvent.emit(ViewEvent.ShowParsingFileError)
            else -> Unit
        }
    }

    fun onInteractionTimeout() = viewModelScope.launch {
        lockedRepo.lockApp()
    }

    override fun onCleared() {
        pdfImporter.deletePendingFile()
    }
}

sealed class ViewEvent {
    data object ShowParsingFileError : ViewEvent()
}
