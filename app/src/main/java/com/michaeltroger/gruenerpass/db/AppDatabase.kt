package com.michaeltroger.gruenerpass.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Certificate::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun certificateDao(): CertificateDao
}