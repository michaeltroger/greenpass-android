package com.michaeltroger.gruenerpass.more

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.extensions.getPackageInfo

class MoreFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.more, rootKey)

        val preference = findPreference<Preference>(getString(R.string.key_preference_version)) ?: error("Preference is required")
        preference.title = getString(R.string.version, requireContext().getPackageInfo().versionName!!)
    }
}