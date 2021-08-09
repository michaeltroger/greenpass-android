package com.michaeltroger.gruenerpass

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface DocumentNameRepo {
    suspend fun getDocumentName(uri: Uri): String
}

class DocumentNameRepoImpl(private val context: Context) : DocumentNameRepo {

    override suspend fun getDocumentName(uri: Uri): String {
        return context.getDocumentName(uri)
    }

    private suspend fun Context.getDocumentName(uri: Uri): String = withContext(Dispatchers.IO) {
        when(uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> getDocumentNameFromDb(uri)
            else -> uri.path?.let(::File)?.name
        }?.removeSuffix(".pdf")?.removeSuffix(".PDF") ?: "Certificate"
    }

    private suspend fun Context.getDocumentNameFromDb(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
            }
        }.getOrNull()
    }
}