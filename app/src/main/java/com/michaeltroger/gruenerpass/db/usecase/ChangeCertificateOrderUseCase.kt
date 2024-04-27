package com.michaeltroger.gruenerpass.db.usecase

import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.db.CertificateDao
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class ChangeCertificateOrderUseCase @Inject constructor(
    private val db: CertificateDao,
) {
    @Suppress("SpreadOperator")
    suspend operator fun invoke(sortedIdList: List<String>) {
        val originalMap = mutableMapOf<String, String>()
        db.getAll().first().forEach {
            originalMap[it.id] = it.name
        }
        val sortedList: List<Certificate> = sortedIdList.map {
            Certificate(id = it, name = originalMap[it]!!)
        }
        db.replaceAll(*sortedList.toTypedArray())
    }
}
