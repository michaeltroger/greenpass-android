package com.michaeltroger.gruenerpass.db.usecase

import com.michaeltroger.gruenerpass.certificates.file.FileRepo
import com.michaeltroger.gruenerpass.db.CertificateDao
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class DeleteAllCertificatesUseCase @Inject constructor(
    private val db: CertificateDao,
    private val fileRepo: FileRepo,
) {
    suspend operator fun invoke() {
        val certificates = db.getAll().first()
        db.deleteAll()
        certificates.forEach {
            fileRepo.deleteFile(it.id)
        }
    }
}
