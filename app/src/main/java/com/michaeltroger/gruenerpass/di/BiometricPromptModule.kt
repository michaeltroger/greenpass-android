package com.michaeltroger.gruenerpass.di

import android.content.Context
import androidx.biometric.BiometricPrompt
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.settings.SettingsFragment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext

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
