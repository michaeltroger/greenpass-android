package com.michaeltroger.gruenerpass.settings

import android.os.Build
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.locator.Locator
import kotlinx.coroutines.launch

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = Locator.encryptedPreferenceDataStore

        setPreferencesFromResource(R.xml.preference, rootKey)

        setupBiometricSetting()
        setupLockscreenSetting()
        setupBrightnessSetting()
    }

    private fun setupBrightnessSetting() {
        val preference = findPreference<Preference>(
            getString(R.string.key_preference_full_brightness)
        ) ?: error("Preference is required")

        preference.setOnPreferenceClickListener {
            lifecycleScope.launch {
                PreferenceUtil(requireContext()).updateScreenBrightness(requireActivity())
            }
            true
        }
    }

    private fun setupLockscreenSetting() {
        val preference = findPreference<Preference>(
            getString(R.string.key_preference_show_on_locked_screen)
        ) ?: error("Preference is required")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            preference.isVisible = true
            preference.setOnPreferenceClickListener {
                lifecycleScope.launch {
                    PreferenceUtil(requireContext()).updateShowOnLockedScreen(requireActivity())
                }
                true
            }
        }
    }

    private fun setupBiometricSetting() {
        val preference = findPreference<ValidateSwitchPreferenceCompat>(
            getString(R.string.key_preference_biometric)
        ) ?: error("Preference is required")

        if (BiometricManager.from(requireContext())
                .canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
        ) {
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
        const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
