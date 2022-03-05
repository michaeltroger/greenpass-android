package com.michaeltroger.gruenerpass.settings

import android.content.Context
import android.util.AttributeSet
import androidx.preference.SwitchPreferenceCompat

class ValidateSwitchPreferenceCompat(context: Context, attrs: AttributeSet?) : SwitchPreferenceCompat(context, attrs) {

    override fun onClick() {
        // disable change of switch on click -> value is set programmatically after validation
    }

}