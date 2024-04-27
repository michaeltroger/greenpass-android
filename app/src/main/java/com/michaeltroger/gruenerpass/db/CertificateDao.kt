package com.michaeltroger.gruenerpass.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CertificateDao {
    @Query("SELECT * FROM certificates")
    fun getAll(): Flow<List<Certificate>>

    @Query("SELECT * FROM certificates WHERE id = :id")
    fun get(id: String): Flow<Certificate?>

    @Insert
    suspend fun insertAll(vararg certificates: Certificate)

    @Query("UPDATE certificates SET name = :name WHERE id = :id")
    suspend fun updateName(id: String, name: String): Int

    @Query("DELETE FROM certificates WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM certificates")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(vararg certificates: Certificate) {
        deleteAll()
        insertAll(*certificates)
    }
}
