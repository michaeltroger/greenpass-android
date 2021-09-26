package com.michaeltroger.gruenerpass.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.michaeltroger.gruenerpass.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
    }
}