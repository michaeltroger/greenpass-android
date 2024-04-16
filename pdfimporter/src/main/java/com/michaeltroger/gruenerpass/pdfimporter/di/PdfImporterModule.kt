package com.michaeltroger.gruenerpass.pdfimporter.di

import com.michaeltroger.gruenerpass.pdfimporter.DocumentNameRepo
import com.michaeltroger.gruenerpass.pdfimporter.DocumentNameRepoImpl
import com.michaeltroger.gruenerpass.pdfimporter.FileImportRepo
import com.michaeltroger.gruenerpass.pdfimporter.FileImportRepoImpl
import com.michaeltroger.gruenerpass.pdfimporter.PdfImporter
import com.michaeltroger.gruenerpass.pdfimporter.PdfImporterImpl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
public abstract class PdfImporterModule {

    @Binds
    @Singleton
    internal abstract fun pdfImporter(
        impl: PdfImporterImpl
    ): PdfImporter

    @Binds
    internal abstract fun fileImportRepo(
        impl: FileImportRepoImpl
    ): FileImportRepo

    @Binds
    internal abstract fun documentNameRepo(
        impl: DocumentNameRepoImpl
    ): DocumentNameRepo
}
