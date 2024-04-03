package com.michaeltroger.gruenerpass.pdf.di;

import com.michaeltroger.gruenerpass.pdf.PdfDecryptor;
import com.michaeltroger.gruenerpass.pdf.PdfDecryptorImpl;

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfModule {

    @Binds
    abstract fun bindPdfDecryptor(
        pdfDecryptorImpl: PdfDecryptorImpl
    ): PdfDecryptor
}
