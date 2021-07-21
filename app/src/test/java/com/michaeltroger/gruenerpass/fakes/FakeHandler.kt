package com.michaeltroger.gruenerpass.fakes

import android.net.Uri
import com.michaeltroger.gruenerpass.model.PdfHandler


class FakeHandler(
    private val fileInAppCache: Boolean,
    private val isPasswordProtected: Boolean = false,
    private var copySuccess: Boolean = true
) : PdfHandler {

    fun overrideCopySuccess(success: Boolean) {
        copySuccess = success
    }

    override suspend fun doesFileExist(): Boolean {
        return fileInAppCache
    }

    override suspend fun deleteFile() {
        // nothing to do in test
    }

    override suspend fun isPdfPasswordProtected(uri: Uri): Boolean {
        return isPasswordProtected
    }

    override suspend fun copyPdfToCache(uri: Uri): Boolean {
        return copySuccess
    }

    override suspend fun decryptAndCopyPdfToCache(uri: Uri, password: String): Boolean {
        return copySuccess
    }

}