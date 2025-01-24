package com.michaeltroger.gruenerpass.settings

import android.os.Build
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.lifecycleScope
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.cache.BitmapCache
import com.michaeltroger.gruenerpass.lock.AppLockedRepo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var biometricPromptInfo: BiometricPrompt.PromptInfo
    @Inject
    lateinit var preferenceUtil: PreferenceUtil
    @Inject
    lateinit var lockedRepo: AppLockedRepo

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)

        setupBiometricSetting()
        setupBarcodeSetting()
        setupLockscreenSetting()
        setupBrightnessSetting()
        setupPreventScreenshotsSetting()
    }

    private fun setupBarcodeSetting() {
        val preferenceBarcode = findPreference<DropDownPreference>(
            getString(R.string.key_preference_extract_barcodes)
        ) ?: error("Preference is required")

        preferenceBarcode.setOnPreferenceClickListener {
            BitmapCache.memoryCache.evictAll()
            true
        }
    }

    private fun setupBrightnessSetting() {
        val preference = findPreference<Preference>(
            getString(R.string.key_preference_full_brightness)
        ) ?: error("Preference is required")

        preference.setOnPreferenceClickListener {
            lifecycleScope.launch {
                preferenceUtil.updateScreenBrightness(requireActivity())
            }
            true
        }
    }

    private fun setupPreventScreenshotsSetting() {
        val preference = findPreference<Preference>(
            getString(R.string.key_preference_prevent_screenshots)
        ) ?: error("Preference is required")

        preference.setOnPreferenceClickListener {
            lifecycleScope.launch {
                preferenceUtil.updatePreventScreenshots(requireActivity())
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
                    preferenceUtil.updateShowOnLockedScreen(requireActivity())
                }
                true
            }
        }
    }

    private fun setupBiometricSetting() {
        val preference = findPreference<ValidateSwitchPreference>(
            getString(R.string.key_preference_biometric)
        ) ?: error("Preference is required")

        if (BiometricManager.from(requireContext())
                .canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
        ) {
            preference.isVisible = true
        }

        val preventScreenshotsPreference = findPreference<SwitchPreference>(
            getString(R.string.key_preference_prevent_screenshots)
        ) ?: error("Preference is required")
        if (!preference.isChecked) {
            preventScreenshotsPreference.isEnabled = true
        }

        preference.apply {
            setOnPreferenceClickListener {
                BiometricPrompt(
                    this@SettingsFragment,
                    MyAuthenticationCallback(preference)
                ).authenticate(biometricPromptInfo)
                true
            }
        }
    }

    private inner class MyAuthenticationCallback(
        private val preference: ValidateSwitchPreference
    ) : BiometricPrompt.AuthenticationCallback() {

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            lifecycleScope.launch {
                requireActivity().onUserInteraction()
                lockedRepo.unlockApp()
                preference.isChecked = !preference.isChecked

                val preventScreenshotsPreference = findPreference<SwitchPreference>(
                    getString(R.string.key_preference_prevent_screenshots)
                ) ?: error("Preference is required")

                if (preference.isChecked) {
                    preventScreenshotsPreference.isChecked = true
                    preventScreenshotsPreference.isEnabled = false
                    lifecycleScope.launch {
                        preferenceUtil.updatePreventScreenshots(requireActivity())
                    }
                } else {
                    preventScreenshotsPreference.isEnabled = true
                }
            }
        }
    }

    companion object {
        const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
