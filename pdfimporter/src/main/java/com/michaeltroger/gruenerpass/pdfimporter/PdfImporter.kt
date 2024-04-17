package com.michaeltroger.gruenerpass.pdfimporter

import android.app.Application
import android.net.Uri
import com.michaeltroger.gruenerpass.logger.logging.Logger
import com.michaeltroger.gruenerpass.pdfdecryptor.PdfDecryptor
import com.michaeltroger.gruenerpass.pdfrenderer.PdfRendererBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

public interface PdfImporter {
    public suspend fun preparePendingFile(uri: Uri)
    public fun hasPendingFile(): Flow<Boolean>
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

    private val _pendingFile: MutableStateFlow<PendingCertificate?> = MutableStateFlow(
        null
    )
    private val pendingFile: StateFlow<PendingCertificate?> = _pendingFile

    override suspend fun preparePendingFile(uri: Uri) {
        _pendingFile.value = fileRepo.copyToApp(uri)
        logger.logDebug(pendingFile)
    }

    override fun hasPendingFile(): Flow<Boolean> = pendingFile.map {
        it != null
    }.distinctUntilChanged()

    override fun deletePendingFile() {
        pendingFile.value?.let {
            _pendingFile.value = null
            fileRepo.deleteFile(it.fileName)
        }
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    override suspend fun importPdf(): PdfImportResult {
        val pendingFile = pendingFile.value ?: return PdfImportResult.ParsingError
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
        val pendingFile = pendingFile.value ?: return PdfImportResult.ParsingError
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
            _pendingFile.value = null
            renderer.close()
        }
    }
}

public sealed class PdfImportResult {
    public data class Success(val pendingCertificate: PendingCertificate) : PdfImportResult()
    public data object PasswordRequired : PdfImportResult()
    public data object ParsingError : PdfImportResult()
}
