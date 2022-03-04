package com.michaeltroger.gruenerpass.settings

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.locator.Locator

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)

        val preference = findPreference<ValidateSwitchPreferenceCompat>(getString(R.string.key_preference_biometric)) ?: error("Preference is required")

        if (BiometricManager.from(requireContext())
                .canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS) {
            preference.isVisible = true
        }

        val biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(requireContext()),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    preference.isChecked = !preference.isChecked
                }
            })
        val promptInfo = Locator.biometricPromptInfo(requireContext())

        preference.apply {
            setOnPreferenceClickListener {
                biometricPrompt.authenticate(promptInfo)
                true
            }
        }
    }

    companion object {
        const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK// or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}