package com.michaeltroger.gruenerpass.more

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.extensions.getInstallerPackageName
import com.michaeltroger.gruenerpass.extensions.getPackageInfo
import com.michaeltroger.gruenerpass.extensions.getSignature
import javax.security.cert.X509Certificate

class MoreFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.more, rootKey)

        setVersionAndInstaller()
    }

    private fun setVersionAndInstaller() {
        val preference = findPreference<Preference>(
            getString(R.string.key_preference_version)
        ) ?: error("Preference is required")

        preference.title = getString(R.string.version, requireContext().getPackageInfo().versionName!!)

        preference.summary = when (requireContext().getInstallerPackageName()) {
            "com.android.vending" -> "Google Play Store"
            "com.amazon.venezia" -> "Amazon Appstore"
            "com.huawei.appmarket" -> "Huawei AppGallery"
            else -> {
                val signature = requireContext().getSignature() ?: return
                val certificate = X509Certificate.getInstance(signature.toByteArray())
                if (certificate.subjectDN.name.contains("FDroid")) {
                    "F-Droid"
                } else null
            }
        }
    }
}
