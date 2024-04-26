package com.michaeltroger.gruenerpass.db.usecase

import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GetCertificatesFlowUseCase @Inject constructor(
    private val db: CertificateDao,
) {

    operator fun invoke(): Flow<List<Certificate>> {
        return db.getAll()
    }
}
