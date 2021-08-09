package com.michaeltroger.gruenerpass.locator

import android.content.Context
import com.michaeltroger.gruenerpass.model.PdfHandler
import com.michaeltroger.gruenerpass.model.PdfHandlerImpl

object Locator {
    fun pdfHandler(context: Context): PdfHandler = PdfHandlerImpl(context)
}