package com.michaeltroger.gruenerpass.migration

import android.content.Context
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class AppMigrateFrom27 @Inject constructor(@ApplicationContext private val context: Context, private val db: CertificateDao) {

    @OptIn(DelicateCoroutinesApi::class)
    operator fun invoke() {
        try {
            val src = File(context.filesDir, "certificate.pdf")
            if (src.exists()) {
                GlobalScope.launch(Dispatchers.IO) {
                    db.insertAll(
                        Certificate(
                            id = "certificate.pdf",
                            name = "Certificate"
                        )
                    )
                }
            }
        } catch (ignore: Exception) {}
    }
}
