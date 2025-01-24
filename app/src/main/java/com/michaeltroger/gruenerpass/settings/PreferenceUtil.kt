package com.michaeltroger.gruenerpass.settings

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.view.WindowManager
import com.michaeltroger.gruenerpass.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PreferenceUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: SharedPreferences,
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

    suspend fun updatePreventScreenshots(activity: Activity) {
        val preventScreenshots = withContext(Dispatchers.IO) {
            preferenceManager.getBoolean(
                context.getString(R.string.key_preference_prevent_screenshots),
                true
            )
        }

        activity.window.apply {
            if (preventScreenshots) {
                addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
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
