package com.michaeltroger.gruenerpass.certificates.file.di;

import com.michaeltroger.gruenerpass.certificates.file.FileRepo
import com.michaeltroger.gruenerpass.certificates.file.FileRepoImpl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class FileModule {

    @Binds
    abstract fun bindFileRepo(
        fileRepoImpl: FileRepoImpl
    ): FileRepo
}
