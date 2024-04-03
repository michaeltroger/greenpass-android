package com.michaeltroger.gruenerpass.db.di

import android.content.Context
import androidx.room.Room
import com.michaeltroger.gruenerpass.db.AppDatabase
import com.michaeltroger.gruenerpass.db.CertificateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun provideCertificateDao(appDatabase: AppDatabase): CertificateDao {
        return appDatabase.certificateDao()
    }

    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "greenpass"
        ).build()
    }
}
