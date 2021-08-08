package com.michaeltroger.gruenerpass.locator

import android.content.Context
import com.michaeltroger.gruenerpass.model.PdfHandler
import com.michaeltroger.gruenerpass.model.PdfHandlerImpl
import com.michaeltroger.gruenerpass.model.PdfRenderer
import com.michaeltroger.gruenerpass.model.PdfRendererImpl
import kotlinx.coroutines.newSingleThreadContext

object Locator {
    fun pdfHandler(context: Context): PdfHandler = PdfHandlerImpl(context)
}