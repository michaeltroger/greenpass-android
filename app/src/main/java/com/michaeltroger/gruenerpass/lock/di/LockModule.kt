package com.michaeltroger.gruenerpass.lock.di

import com.michaeltroger.gruenerpass.lock.AppLockedRepo
import com.michaeltroger.gruenerpass.lock.AppLockedRepoImpl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LockModule {

    @Singleton
    @Binds
    abstract fun bindLockRepo(
        impl: AppLockedRepoImpl
    ): AppLockedRepo
}
