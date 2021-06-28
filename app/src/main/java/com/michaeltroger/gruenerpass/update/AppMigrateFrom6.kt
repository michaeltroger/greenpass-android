package com.michaeltroger.gruenerpass.update

import android.content.Context
import com.michaeltroger.gruenerpass.pdf.PDF_FILENAME
import java.io.File
import java.lang.Exception

class AppMigrateFrom6 {

    operator fun invoke(context: Context) {
        try {
            val src = File(context.cacheDir, "certificate.pdf")

            if (src.exists()) {
                val dest = File(context.filesDir, PDF_FILENAME)
                src.copyTo(dest)
                src.delete()
            }
        } catch (ignore: Exception) {}
    }
}