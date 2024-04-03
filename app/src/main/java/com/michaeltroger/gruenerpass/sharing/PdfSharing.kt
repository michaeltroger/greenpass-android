package com.michaeltroger.gruenerpass.sharing

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.db.Certificate
import java.io.File
import javax.inject.Inject

private const val PDF_MIME_TYPE = "application/pdf"

interface PdfSharing {

    fun openShareAllFilePicker(context: Context, certificates: List<Certificate>)
    fun openShareFilePicker(context: Context, certificate: Certificate)
}
class PdfSharingImpl @Inject constructor() : PdfSharing {

    override fun openShareAllFilePicker(context: Context, certificates: List<Certificate>) {
        val pdfUris = certificates.map { certificate ->
            FileProvider.getUriForFile(
                context,
                context.getString(R.string.pdf_file_provider_authority),
                File(context.filesDir, certificate.id),
                "${certificate.name}.pdf"
            )
        }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(pdfUris))
            type = PDF_MIME_TYPE
        }

        context.startActivity(Intent.createChooser(shareIntent, null))
    }

    override fun openShareFilePicker(context: Context, certificate: Certificate) {
        val pdfUri = FileProvider.getUriForFile(
            context,
            context.getString(R.string.pdf_file_provider_authority),
            File(context.filesDir, certificate.id),
            "${certificate.name}.pdf"
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            type = PDF_MIME_TYPE
        }

        context.startActivity(Intent.createChooser(shareIntent, null))
    }
}
