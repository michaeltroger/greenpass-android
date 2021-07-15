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

class PdfHandler(private val context: Context) {

    private val file = File(context.filesDir, PDF_FILENAME)

    suspend fun doesFileExist(): Boolean = withContext(Dispatchers.IO) {
        file.exists()
    }

    suspend fun deleteFile() = withContext(Dispatchers.IO) {
        if (doesFileExist()) {
            file.delete()
        }
    }

    suspend fun isPdfPasswordProtected(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            getInputStream(uri).use {
                try {
                    return@withContext PDDocument.load(it).checkIfPasswordProtectedAndClose()
                } catch (exception: InvalidPasswordException) {
                    return@withContext true
                }
            }
        } catch (exception: Exception) {
            return@withContext false
        }
    }
    /**
     * @return true if successful
     */
    suspend fun copyPdfToCache(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            getInputStream(uri).use {
                deleteFile() // clear old file first if it exists
                it.copyTo(FileOutputStream(file))
                return@withContext true
            }
        } catch (exception: Exception) {
            return@withContext false
        }
    }

    /**
     * @return true if successful
     */
    suspend fun decryptAndCopyPdfToCache(uri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            getInputStream(uri).use {
                with(PDDocument.load(it, password)) {
                    deleteFile() // clear old file first if it exists
                    removePasswordCopyAndClose()
                }
                return@withContext true
            }
        } catch (exception: Exception) {
            return@withContext false
        }
    }

    private fun getInputStream(uri: Uri): InputStream = context.contentResolver.openInputStream(uri)!!

    private fun PDDocument.removePasswordCopyAndClose() = use {
        isAllSecurityToBeRemoved = true
        save(FileOutputStream(file))
    }

    private fun PDDocument.checkIfPasswordProtectedAndClose(): Boolean = use {
        return isEncrypted
    }
}