package com.michaeltroger.gruenerpass.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CertificateDao {
    @Query("SELECT * FROM certificates")
    fun getAll(): List<Certificate>

    @Insert
    fun insertAll(vararg certificates: Certificate)

    @Query("UPDATE certificates SET name = :name WHERE id = :id")
    fun updateName(id: String, name: String): Int

    @Query("DELETE FROM certificates WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM certificates")
    fun deleteAll()

}