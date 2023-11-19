package com.michaeltroger.gruenerpass.settings

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.view.WindowManager
import com.michaeltroger.gruenerpass.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PreferenceUtil(
    private val context: Context,
    private val preferenceManager: SharedPreferences
    = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context),
) {

    suspend fun updateScreenBrightness(activity: Activity) {
        val fullBrightness = withContext(Dispatchers.IO) {
            preferenceManager.getBoolean(
                context.getString(R.string.key_preference_full_brightness),
                false
            )
        }

        activity.window.apply {
            attributes.apply {
                screenBrightness = if (fullBrightness) {
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                } else {
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
            addFlags(WindowManager.LayoutParams.SCREEN_BRIGHTNESS_CHANGED)
        }
    }

    suspend fun updateShowOnLockedScreen(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val showOnLockedScreen = withContext(Dispatchers.IO) {
                preferenceManager.getBoolean(
                    context.getString(R.string.key_preference_show_on_locked_screen),
                    false
                )
            }
            activity.setShowWhenLocked(showOnLockedScreen)
        }
    }
}