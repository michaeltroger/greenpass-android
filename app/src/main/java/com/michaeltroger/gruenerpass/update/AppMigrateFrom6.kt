package com.michaeltroger.gruenerpass.update

import android.content.Context
import com.michaeltroger.gruenerpass.pdf.FILENAME
import java.io.File

class AppMigrateFrom6 {

    operator fun invoke(context: Context) {
        val src = File(context.cacheDir, "certificate.pdf")

        if (src.exists()) {
            val dest = File(context.filesDir, FILENAME)
            src.copyTo(dest)
        }

    }
}