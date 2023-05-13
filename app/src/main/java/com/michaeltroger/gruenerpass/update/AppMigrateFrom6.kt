package com.michaeltroger.gruenerpass.update

import android.content.Context
import java.io.File

private const val PDF_FILENAME = "certificate.pdf"

class AppMigrateFrom6 {

    operator fun invoke(context: Context) {
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
