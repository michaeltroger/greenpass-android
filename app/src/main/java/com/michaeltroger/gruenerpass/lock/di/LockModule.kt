package com.michaeltroger.gruenerpass.lock.di

import android.content.Context
import androidx.biometric.BiometricPrompt
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.lock.AppLockedRepo
import com.michaeltroger.gruenerpass.lock.AppLockedRepoImpl
import com.michaeltroger.gruenerpass.settings.SettingsFragment

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext
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

@Module
@InstallIn(FragmentComponent::class)
object BiometricPromptModule {
    @Provides
    fun provideBiometricPromptInfo(@ApplicationContext context: Context): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.authenticate))
            .setConfirmationRequired(false)
            .setAllowedAuthenticators(SettingsFragment.AUTHENTICATORS)
            .build()
}
