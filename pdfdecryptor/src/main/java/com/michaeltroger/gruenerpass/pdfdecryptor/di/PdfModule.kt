package com.michaeltroger.gruenerpass.pdfdecryptor.di

import com.michaeltroger.gruenerpass.pdfdecryptor.PdfDecryptor;
import com.michaeltroger.gruenerpass.pdfdecryptor.PdfDecryptorImpl;

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
public abstract class PdfModule {

    @Binds
    internal abstract fun bindPdfDecryptor(
        pdfDecryptorImpl: PdfDecryptorImpl
    ): PdfDecryptor
}
