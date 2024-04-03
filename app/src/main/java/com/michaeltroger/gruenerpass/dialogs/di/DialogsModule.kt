package com.michaeltroger.gruenerpass.dialogs.di;


import com.michaeltroger.gruenerpass.dialogs.CertificateDialogs
import com.michaeltroger.gruenerpass.dialogs.CertificateDialogsImpl
import com.michaeltroger.gruenerpass.sharing.PdfSharing
import com.michaeltroger.gruenerpass.sharing.PdfSharingImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent

@Module
@InstallIn(FragmentComponent::class)
abstract class DialogsModule {

    @Binds
    abstract fun bindCertificateDialogs(
        certificateDialogsImpl: CertificateDialogsImpl
    ): CertificateDialogs
}
