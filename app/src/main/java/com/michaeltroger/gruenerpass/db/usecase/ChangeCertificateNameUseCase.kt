package com.michaeltroger.gruenerpass.db.usecase

import com.michaeltroger.gruenerpass.db.CertificateDao
import javax.inject.Inject

class ChangeCertificateNameUseCase @Inject constructor(
    private val db: CertificateDao,
) {
    suspend operator fun invoke(id: String, documentName: String) {
        db.updateName(id = id, name = documentName)
    }
}
