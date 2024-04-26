package com.michaeltroger.gruenerpass.db.usecase

import com.michaeltroger.gruenerpass.certificates.file.FileRepo
import com.michaeltroger.gruenerpass.db.CertificateDao
import javax.inject.Inject

class DeleteSingleCertificateUseCase @Inject constructor(
    private val db: CertificateDao,
    private val fileRepo: FileRepo,
) {
    suspend operator fun invoke(fileName: String) {
        db.delete(fileName)
        fileRepo.deleteFile(fileName)
    }
}
