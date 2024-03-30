package com.michaeltroger.gruenerpass.settings

import android.content.Context
import android.util.AttributeSet
import androidx.preference.SwitchPreference

class ValidateSwitchPreference(context: Context, attrs: AttributeSet?) : SwitchPreference(context, attrs) {

    override fun onClick() {
        // disable change of switch on click -> value is set programmatically after validation
    }

}
