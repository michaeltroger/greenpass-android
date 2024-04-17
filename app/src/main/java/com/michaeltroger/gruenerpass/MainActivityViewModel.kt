package com.michaeltroger.gruenerpass

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.lock.AppLockedRepo
import com.michaeltroger.gruenerpass.pdfimporter.PdfImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val pdfImporter: PdfImporter,
    private val lockedRepo: AppLockedRepo,
): ViewModel() {

    val lockedState = lockedRepo.isAppLocked()

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

    fun deletePendingFile() {
        pdfImporter.deletePendingFile()
    }
}
