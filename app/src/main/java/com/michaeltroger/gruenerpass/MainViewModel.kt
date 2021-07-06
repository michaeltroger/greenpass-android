package com.michaeltroger.gruenerpass

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.michaeltroger.gruenerpass.pdf.PdfHandler
import kotlinx.coroutines.flow.MutableSharedFlow

class MainViewModel: ViewModel() {
    val updatedUri = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val pdfHandler = PdfHandler()
}