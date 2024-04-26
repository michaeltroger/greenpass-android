package com.michaeltroger.gruenerpass.db.usecase

import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class InsertIntoDatabaseUseCase @Inject constructor(
    private val db: CertificateDao,
) {
    @Suppress("SpreadOperator")
    suspend operator fun invoke(certificate: Certificate, addDocumentInFront: Boolean) {
        if (addDocumentInFront) {
            val all = listOf(certificate) + db.getAll().first()
            db.replaceAll(*all.toTypedArray())
        } else {
            db.insertAll(certificate)
        }
    }
}
