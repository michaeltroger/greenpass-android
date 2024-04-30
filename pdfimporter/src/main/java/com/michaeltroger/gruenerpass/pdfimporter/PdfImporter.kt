package com.michaeltroger.gruenerpass.pdfimporter

import android.app.Application
import android.net.Uri
import com.michaeltroger.gruenerpass.logger.Logger
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
    public suspend fun preparePendingFile(uri: Uri): PdfImportResult
    public fun hasPendingFile(): Flow<Boolean>
    public fun deletePendingFile()
    public suspend fun importPendingFile(password: String?): PdfImportResult
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

    @Suppress("TooGenericExceptionCaught")
    override suspend fun preparePendingFile(uri: Uri): PdfImportResult {
        deletePendingFile()
        return try {
            _pendingFile.value = fileRepo.copyToApp(uri)
            logger.logDebug(pendingFile.value)
            PdfImportResult.Success(pendingFile.value!!)
        } catch (e: Exception) {
            logger.logError(e.toString())
            PdfImportResult.ParsingError
        }
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

    override suspend fun importPendingFile(password: String?): PdfImportResult {
        return if (password == null) {
            importRegularPdf()
        } else {
            importPasswordProtectedPdf(password)
        }
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    private suspend fun importRegularPdf(): PdfImportResult {
        val pendingFile = pendingFile.value ?: return PdfImportResult.NoFileToImport
        try {
            val file = fileRepo.getFile(pendingFile.fileName)
            return if (pdfDecryptor.isPdfPasswordProtected(file)) {
                PdfImportResult.PasswordRequired
            } else {
                if (isValidPdf(pendingFile)) {
                    PdfImportResult.Success(pendingFile)
                } else {
                    deletePendingFile()
                    PdfImportResult.ParsingError
                }
            }
        } catch (e: Throwable) {
            logger.logError(e.toString())
            deletePendingFile()
            return PdfImportResult.ParsingError
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun importPasswordProtectedPdf(password: String): PdfImportResult {
        val pendingFile = pendingFile.value ?: return PdfImportResult.NoFileToImport
        return try {
            val file = fileRepo.getFile(pendingFile.fileName)
            pdfDecryptor.decrypt(password = password, file = file)
            if (isValidPdf(pendingFile)) {
                PdfImportResult.Success(pendingFile)
            } else {
                deletePendingFile()
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
    public data object NoFileToImport : PdfImportResult()
}
