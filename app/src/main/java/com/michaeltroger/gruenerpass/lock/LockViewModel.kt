package com.michaeltroger.gruenerpass.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaeltroger.gruenerpass.pdfimporter.PdfImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LockViewModel @Inject constructor(
    private val pdfImporter: PdfImporter,
    private val lockedRepo: AppLockedRepo,
): ViewModel() {

    fun onAuthenticationSuccess() {
        viewModelScope.launch {
            lockedRepo.unlockApp()
        }
    }

    fun deletePendingFile() {
        pdfImporter.deletePendingFile()
    }
}
