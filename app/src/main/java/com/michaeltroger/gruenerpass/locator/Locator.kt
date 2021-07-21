package com.michaeltroger.gruenerpass.locator

import android.content.Context
import com.michaeltroger.gruenerpass.model.PdfHandler
import com.michaeltroger.gruenerpass.model.PdfHandlerImpl
import com.michaeltroger.gruenerpass.model.PdfRenderer
import com.michaeltroger.gruenerpass.model.PdfRendererImpl

object Locator {
    fun pdfRenderer(context: Context): PdfRenderer = PdfRendererImpl(context)
    fun pdfHandler(context: Context): PdfHandler = PdfHandlerImpl(context)
}