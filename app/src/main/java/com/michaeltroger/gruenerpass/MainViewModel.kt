package com.michaeltroger.gruenerpass

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination
import com.michaeltroger.gruenerpass.lock.AppLockedRepo
import com.michaeltroger.gruenerpass.pdfimporter.PdfImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val pdfImporter: PdfImporter,
    private val lockedRepo: AppLockedRepo,
    private val getMainDestinationFlowUseCase: GetMainDestinationFlowUseCase,
): ViewModel() {

    fun getMainDestination(currentDestination: NavDestination?) =
        getMainDestinationFlowUseCase(currentDestination)

    fun setPendingFile(uri: Uri) {
        viewModelScope.launch {
            pdfImporter.preparePendingFile(uri)
        }
    }

    fun onInteractionTimeout() {
        viewModelScope.launch {
            lockedRepo.lockApp()
        }
    }

    override fun onCleared() {
        pdfImporter.deletePendingFile()
    }
}
