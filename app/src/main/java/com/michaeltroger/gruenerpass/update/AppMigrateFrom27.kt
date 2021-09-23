package com.michaeltroger.gruenerpass.update

import android.content.Context
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.locator.Locator
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class AppMigrateFrom27 {

    @OptIn(DelicateCoroutinesApi::class)
    operator fun invoke(context: Context) {
        try {
            val src = File(context.filesDir, "certificate.pdf")
            if (src.exists()) {
                GlobalScope.launch(Dispatchers.IO) {
                    Locator.database(context).certificateDao().insertAll(Certificate(id = "certificate.pdf", name = "Certificate"))
                }
            }
        } catch (ignore: Exception) {}
    }
}