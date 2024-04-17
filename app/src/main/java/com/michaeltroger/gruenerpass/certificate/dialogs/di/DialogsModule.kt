package com.michaeltroger.gruenerpass.certificate.dialogs.di;


import com.michaeltroger.gruenerpass.certificate.dialogs.CertificateDialogs
import com.michaeltroger.gruenerpass.certificate.dialogs.CertificateDialogsImpl
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
