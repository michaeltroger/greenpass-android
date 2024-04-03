package com.michaeltroger.gruenerpass.pdf

import com.michaeltroger.gruenerpass.di.IoDispatcher
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface PdfDecryptor {
    @Throws(Exception::class, OutOfMemoryError::class)
    suspend fun isPdfPasswordProtected(file: File): Boolean
    @Throws(Exception::class)
    suspend fun decrypt(password: String, file: File)
}

class PdfDecryptorImpl @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher
): PdfDecryptor {

    @Suppress("SwallowedException")
    @Throws(Exception::class, OutOfMemoryError::class)
    override suspend fun isPdfPasswordProtected(file: File): Boolean = withContext(dispatcher) {
        try {
            return@withContext PDDocument.load(file).checkIfPasswordProtectedAndClose()
        } catch (e: InvalidPasswordException) {
            return@withContext true
        }
    }

    @Throws(Exception::class)
    override suspend fun decrypt(
        password: String,
        file: File,
    ): Unit = withContext(dispatcher) {
            with(PDDocument.load(file, password)) {
                removePasswordCopyAndClose(file)
            }
    }

    private fun PDDocument.removePasswordCopyAndClose(file: File) = use {
        isAllSecurityToBeRemoved = true
        FileOutputStream(file).use { outputStream ->
            save(outputStream)
        }
    }

    private fun PDDocument.checkIfPasswordProtectedAndClose(): Boolean = use {
        return isEncrypted
    }
}
