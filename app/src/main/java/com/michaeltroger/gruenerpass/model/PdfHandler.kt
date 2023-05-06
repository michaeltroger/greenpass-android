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
import kotlinx.coroutines.CoroutineDispatcher

interface PdfHandler {
    suspend fun doesFileExist(fileName: String): Boolean
    suspend fun deleteFile(fileName: String)
    @Throws(Exception::class, OutOfMemoryError::class)
    suspend fun isPdfPasswordProtected(uri: Uri): Boolean
    @Throws(Exception::class)
    suspend fun copyPdfToApp(uri: Uri, fileName: String)
    @Throws(Exception::class)
    suspend fun decryptAndCopyPdfToApp(uri: Uri, password: String, fileName: String)
}

class PdfHandlerImpl(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): PdfHandler {

    private fun getFile(fileName: String) =  File(context.filesDir, fileName)

    override suspend fun doesFileExist(fileName: String): Boolean = withContext(dispatcher) {
        getFile(fileName).exists()
    }

    override suspend fun deleteFile(fileName: String) = withContext(dispatcher) {
        if (doesFileExist(fileName)) {
            getFile(fileName).delete()
        }
    }

    @Suppress("SwallowedException")
    @Throws(Exception::class, OutOfMemoryError::class)
    override suspend fun isPdfPasswordProtected(uri: Uri): Boolean = withContext(dispatcher) {
        try {
            getInputStream(uri).use { inputStream ->
                return@withContext PDDocument.load(inputStream).checkIfPasswordProtectedAndClose()
            }
        } catch (e: InvalidPasswordException) {
            return@withContext true
        }
    }

    @Throws(Exception::class)
    override suspend fun copyPdfToApp(uri: Uri, fileName: String): Unit = withContext(dispatcher) {
        getInputStream(uri).use { inputStream ->
            FileOutputStream(getFile(fileName)).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    @Throws(Exception::class)
    override suspend fun decryptAndCopyPdfToApp(
        uri: Uri,
        password: String,
        fileName: String
    ): Unit = withContext(Dispatchers.IO) {
        getInputStream(uri).use { inputStream ->
            with(PDDocument.load(inputStream, password)) {
                removePasswordCopyAndClose(fileName)
            }
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
