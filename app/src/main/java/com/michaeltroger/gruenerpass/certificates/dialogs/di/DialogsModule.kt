package com.michaeltroger.gruenerpass.certificates.dialogs.di;


import com.michaeltroger.gruenerpass.certificates.dialogs.CertificateDialogs
import com.michaeltroger.gruenerpass.certificates.dialogs.CertificateDialogsImpl
import com.michaeltroger.gruenerpass.certificates.dialogs.CertificateErrors
import com.michaeltroger.gruenerpass.certificates.dialogs.CertificateErrorsImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
abstract class DialogsModule {

    @Binds
    abstract fun bindCertificateDialogs(
        impl: CertificateDialogsImpl
    ): CertificateDialogs

    @Binds
    abstract fun bindCertificateErrors(
        impl: CertificateErrorsImpl
    ): CertificateErrors
}
