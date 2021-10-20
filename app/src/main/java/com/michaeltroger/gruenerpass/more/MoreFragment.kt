package com.michaeltroger.gruenerpass.more

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.michaeltroger.gruenerpass.R

class MoreFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.more, rootKey)
    }
}