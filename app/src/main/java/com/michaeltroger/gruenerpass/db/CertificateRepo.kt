package com.michaeltroger.gruenerpass.db

import android.content.Context
import com.michaeltroger.gruenerpass.locator.Locator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CertificateRepo {
    suspend fun insert(certificate: Certificate)
    suspend fun insertAll(list: List<Certificate>)
    suspend fun getAll(): List<Certificate>
    suspend fun delete(id: String)
    suspend fun updateName(id: String, name: String)
    suspend fun replaceAll(list: List<Certificate>)
}

class CertificateRepoImpl(
    context: Context,
    private val db: AppDatabase = Locator.database(context)
) : CertificateRepo {

    override suspend fun insert(certificate: Certificate) = withContext(Dispatchers.IO) {
        db.certificateDao().insertAll(certificate)
    }

    override suspend fun insertAll(list: List<Certificate>) = withContext(Dispatchers.IO) {
        db.certificateDao().insertAll(*list.toTypedArray())
    }

    override suspend fun getAll() = withContext(Dispatchers.IO) {
        db.certificateDao().getAll()
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        db.certificateDao().delete(id)
    }

    override suspend fun updateName(id: String, name: String): Unit = withContext(Dispatchers.IO)  {
        db.certificateDao().updateName(id, name)
    }

    override suspend fun replaceAll(list: List<Certificate>) = withContext(Dispatchers.IO) {
        db.certificateDao().deleteAll()
        db.certificateDao().insertAll(*list.toTypedArray())
    }
}