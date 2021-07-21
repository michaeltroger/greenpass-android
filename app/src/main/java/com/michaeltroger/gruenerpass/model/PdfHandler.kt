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
    suspend fun doesFileExist(): Boolean
    suspend fun deleteFile()
    suspend fun isPdfPasswordProtected(uri: Uri): Boolean
    suspend fun copyPdfToCache(uri: Uri): Boolean
    suspend fun decryptAndCopyPdfToCache(uri: Uri, password: String): Boolean
}

class PdfHandlerImpl(private val context: Context): PdfHandler {

    private val file = File(context.filesDir, PDF_FILENAME)

    override suspend fun doesFileExist(): Boolean = withContext(Dispatchers.IO) {
        file.exists()
    }

    override suspend fun deleteFile() = withContext(Dispatchers.IO) {
        if (doesFileExist()) {
            file.delete()
        }
    }

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
    override suspend fun copyPdfToCache(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            getInputStream(uri).use { inputStream ->
                deleteFile() // clear old file first if it exists
                FileOutputStream(file).use { outputStream ->
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
    override suspend fun decryptAndCopyPdfToCache(uri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            getInputStream(uri).use { inputStream ->
                with(PDDocument.load(inputStream, password)) {
                    deleteFile() // clear old file first if it exists
                    removePasswordCopyAndClose()
                }
            }
            return@withContext true
        } catch (exception: Exception) {
            return@withContext false
        }
    }

    private fun getInputStream(uri: Uri): InputStream = context.contentResolver.openInputStream(uri)!!

    private fun PDDocument.removePasswordCopyAndClose() = use {
        isAllSecurityToBeRemoved = true
        FileOutputStream(file).use { outputStream ->
            save(outputStream)
        }
    }

    private fun PDDocument.checkIfPasswordProtectedAndClose(): Boolean = use {
        return isEncrypted
    }
}