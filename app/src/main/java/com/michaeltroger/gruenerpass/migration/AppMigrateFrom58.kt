package com.michaeltroger.gruenerpass.migration

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.michaeltroger.gruenerpass.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class AppMigrateFrom58 @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: SharedPreferences,
) {

    @OptIn(DelicateCoroutinesApi::class)
    operator fun invoke() {
        try {
            GlobalScope.launch(Dispatchers.IO) {
                val wasBarcodeSearchEnabled = preferenceManager.getBoolean("searchForQrCode", true)
                val wasExtendedBarcodeSearchEnabled = preferenceManager.getBoolean("tryHardBarcode", true)
                val prefKey = when {
                    wasBarcodeSearchEnabled && wasExtendedBarcodeSearchEnabled -> {
                        R.string.key_preference_barcodes_extended
                    }
                    wasBarcodeSearchEnabled -> R.string.key_preference_barcodes_regular
                    !wasBarcodeSearchEnabled -> R.string.key_preference_barcodes_disabled
                    else -> R.string.key_preference_barcodes_extended
                }
                preferenceManager.edit {
                    putString(context.getString(R.string.key_preference_extract_barcodes), context.getString(prefKey))
                }
            }
        } catch (ignore: Exception) {}
    }
}
