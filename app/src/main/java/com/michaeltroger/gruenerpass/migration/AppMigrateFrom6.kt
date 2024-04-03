package com.michaeltroger.gruenerpass.migration

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

private const val PDF_FILENAME = "certificate.pdf"

class AppMigrateFrom6 @Inject constructor(@ApplicationContext private val context: Context) {

    operator fun invoke() {
        try {
            val src = File(context.cacheDir, PDF_FILENAME)

            if (src.exists()) {
                val dest = File(context.filesDir, PDF_FILENAME)
                src.copyTo(dest)
                src.delete()
            }
        } catch (ignore: Exception) {}
    }
}
