package com.michaeltroger.gruenerpass.certificates.sharing.di;


import com.michaeltroger.gruenerpass.certificates.sharing.PdfSharing
import com.michaeltroger.gruenerpass.certificates.sharing.PdfSharingImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent

@Module
@InstallIn(FragmentComponent::class)
abstract class SharingModule {

    @Binds
    abstract fun bindPdfSharing(
        pdfSharingImpl: PdfSharingImpl
    ): PdfSharing
}
