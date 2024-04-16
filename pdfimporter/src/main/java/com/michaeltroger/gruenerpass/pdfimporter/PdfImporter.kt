package com.michaeltroger.gruenerpass.pdfimporter

import android.app.Application
import android.net.Uri
import com.michaeltroger.gruenerpass.logger.logging.Logger
import com.michaeltroger.gruenerpass.pdfdecryptor.PdfDecryptor
import com.michaeltroger.gruenerpass.pdfrenderer.PdfRendererBuilder
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

public interface PdfImporter {
    public suspend fun preparePendingFile(uri: Uri)
    public fun hasPendingFile(): Boolean
    public fun deletePendingFile()
    public suspend fun importPdf(): PdfImportResult
    public suspend fun importPasswordProtectedPdf(password: String): PdfImportResult
}

internal class PdfImporterImpl @Inject constructor(
    private val app: Application,
    private val fileRepo: FileImportRepo,
    private val pdfDecryptor: PdfDecryptor,
    private val logger: Logger,
) : PdfImporter {

    private var pendingFile: PendingCertificate? = null

    override suspend fun preparePendingFile(uri: Uri) {
        pendingFile = fileRepo.copyToApp(uri)
        logger.logDebug(pendingFile)
    }

    override fun hasPendingFile(): Boolean {
        return pendingFile != null
    }

    override fun deletePendingFile() {
        pendingFile?.let {
            pendingFile = null
            fileRepo.deleteFile(it.fileName)
        }
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    override suspend fun importPdf(): PdfImportResult {
        val pendingFile = pendingFile ?: return PdfImportResult.ParsingError
        try {
            val file = fileRepo.getFile(pendingFile.fileName)
            return if (pdfDecryptor.isPdfPasswordProtected(file)) {
                PdfImportResult.PasswordRequired
            } else {
                if (isValidPdf(pendingFile)) {
                    PdfImportResult.Success(pendingFile)
                } else {
                    PdfImportResult.ParsingError
                }
            }
        } catch (e: Throwable) {
            logger.logError(e.toString())
            return PdfImportResult.ParsingError
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun importPasswordProtectedPdf(password: String): PdfImportResult {
        val pendingFile = pendingFile ?: return PdfImportResult.ParsingError
        return try {
            val file = fileRepo.getFile(pendingFile.fileName)
            pdfDecryptor.decrypt(password = password, file = file)
            if (isValidPdf(pendingFile)) {
                PdfImportResult.Success(pendingFile)
            } else {
                PdfImportResult.ParsingError
            }
        } catch (e: Exception) {
            logger.logError(e.toString())
            PdfImportResult.PasswordRequired
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun isValidPdf(certificate: PendingCertificate): Boolean {
        val renderer = PdfRendererBuilder.create(
            app,
            fileName = certificate.fileName,
            renderContext = Dispatchers.IO
        )
        return try {
            renderer.loadFile()
            true
        } catch (e: Exception) {
            logger.logError(e.toString())
            fileRepo.deleteFile(certificate.fileName)
            false
        } finally {
            this.pendingFile = null
            renderer.close()
        }
    }
}

public sealed class PdfImportResult {
    public data class Success(val pendingCertificate: PendingCertificate) : PdfImportResult()
    public data object PasswordRequired : PdfImportResult()
    public data object ParsingError : PdfImportResult()
}
