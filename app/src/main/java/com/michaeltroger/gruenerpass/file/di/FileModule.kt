package com.michaeltroger.gruenerpass.file.di;

import com.michaeltroger.gruenerpass.file.DocumentNameRepo
import com.michaeltroger.gruenerpass.file.DocumentNameRepoImpl
import com.michaeltroger.gruenerpass.file.FileRepo
import com.michaeltroger.gruenerpass.file.FileRepoImpl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class FileModule {

    @Binds
    abstract fun bindDocumentNameRepo(
        documentNameRepoImpl: DocumentNameRepoImpl
    ): DocumentNameRepo

    @Binds
    abstract fun bindLogger(
        fileRepoImpl: FileRepoImpl
    ): FileRepo
}
