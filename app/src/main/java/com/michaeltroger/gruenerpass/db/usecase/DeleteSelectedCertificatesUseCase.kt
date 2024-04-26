package com.michaeltroger.gruenerpass.db.usecase

import com.michaeltroger.gruenerpass.certificates.file.FileRepo
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
import javax.inject.Inject

class DeleteSelectedCertificatesUseCase @Inject constructor(
    private val db: CertificateDao,
    private val fileRepo: FileRepo,
) {
    suspend operator fun invoke(docs: List<Certificate>) {
        docs.forEach {
            db.delete(it.id)
            fileRepo.deleteFile(it.id)
        }
    }
}
