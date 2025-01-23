package com.michaeltroger.gruenerpass.settings

enum class BarcodeSearchMode { // enum representation of R.string.key_preference_barcodes_disabled etc.
    DISABLED,
    REGULAR,
    EXTENDED;

    companion object {
        fun fromPrefValue(prefValue: String) = BarcodeSearchMode.valueOf(prefValue.uppercase())
    }
}
