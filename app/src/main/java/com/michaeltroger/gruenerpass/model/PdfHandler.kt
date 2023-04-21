package com.michaeltroger.gruenerpass.model

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


const val PDF_FILENAME = "certificate.pdf"

interface PdfHandler {
    suspend fun doesFileExist(fileName: String): Boolean
    suspend fun deleteFile(fileName: String)
    suspend fun isPdfPasswordProtected(uri: Uri): Boolean
    suspend fun copyPdfToCache(uri: Uri, fileName: String): Boolean
    suspend fun decryptAndCopyPdfToCache(uri: Uri, password: String, fileName: String): Boolean
}

class PdfHandlerImpl(private val context: Context): PdfHandler {

    private fun getFile(fileName: String) =  File(context.filesDir, fileName)

    override suspend fun doesFileExist(fileName: String): Boolean = withContext(Dispatchers.IO) {
        getFile(fileName).exists()
    }

    override suspend fun deleteFile(fileName: String) = withContext(Dispatchers.IO) {
        if (doesFileExist(fileName)) {
            getFile(fileName).delete()
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun isPdfPasswordProtected(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            getInputStream(uri).use { inputStream ->
                try {
                    return@withContext PDDocument.load(inputStream).checkIfPasswordProtectedAndClose()
                } catch (exception: InvalidPasswordException) {
                    return@withContext true
                } catch(exception: OutOfMemoryError) {
                    return@withContext false
                }
            }
        } catch (exception: Exception) {
            return@withContext false
        }
    }

    /**
     * @return true if successful
     */
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun copyPdfToCache(uri: Uri, fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            getInputStream(uri).use { inputStream ->
                FileOutputStream(getFile(fileName)).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return@withContext true
        } catch (exception: Exception) {
            return@withContext false
        }
    }

    /**
     * @return true if successful
     */
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun decryptAndCopyPdfToCache(uri: Uri, password: String, fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            getInputStream(uri).use { inputStream ->
                with(PDDocument.load(inputStream, password)) {
                    removePasswordCopyAndClose(fileName)
                }
            }
            return@withContext true
        } catch (exception: Exception) {
            return@withContext false
        }
    }

    private fun getInputStream(uri: Uri): InputStream = context.contentResolver.openInputStream(uri)!!

    private fun PDDocument.removePasswordCopyAndClose(fileName: String) = use {
        isAllSecurityToBeRemoved = true
        FileOutputStream(getFile(fileName)).use { outputStream ->
            save(outputStream)
        }
    }

    private fun PDDocument.checkIfPasswordProtectedAndClose(): Boolean = use {
        return isEncrypted
    }
}