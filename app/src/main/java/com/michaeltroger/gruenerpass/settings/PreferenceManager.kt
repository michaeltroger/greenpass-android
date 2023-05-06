package com.michaeltroger.gruenerpass.settings

import android.content.Context
import android.content.SharedPreferences
import com.michaeltroger.gruenerpass.R

interface PreferenceListener {
    fun onPreferenceChanged()
}

interface PreferenceManager {
    fun fullScreenBrightness(): Boolean
    fun searchForQrCode(): Boolean
    fun shouldAuthenticate(): Boolean
    fun init(preferenceListener: PreferenceListener)
}

class PreferenceManagerImpl(
    private val context: Context,
    private val preferenceManager: SharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context),
): PreferenceManager, SharedPreferences.OnSharedPreferenceChangeListener {

    private var preferenceListener: PreferenceListener? = null
    private var fullScreenBrightness: Boolean = false
    private var searchForQrCode: Boolean = true
    private var shouldAuthenticate = false

    override fun init(preferenceListener: PreferenceListener) {
        this.preferenceListener = preferenceListener
        preferenceManager.registerOnSharedPreferenceChangeListener(this)
        shouldAuthenticate = preferenceManager.getBoolean(
            context.getString(R.string.key_preference_biometric),
            false
        )
        searchForQrCode = preferenceManager.getBoolean(
            context.getString(R.string.key_preference_search_for_qr_code),
            true
        )
        fullScreenBrightness = preferenceManager.getBoolean(
            context.getString(R.string.key_preference_full_brightness),
            false
        )
    }

    override fun fullScreenBrightness(): Boolean {
        return fullScreenBrightness
    }

    override fun searchForQrCode(): Boolean {
        return searchForQrCode
    }

    override fun shouldAuthenticate(): Boolean {
        return shouldAuthenticate
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            context.getString(R.string.key_preference_biometric) -> {
                shouldAuthenticate = sharedPreferences.getBoolean(key, false)
            }
            context.getString(R.string.key_preference_search_for_qr_code) -> {
                searchForQrCode = sharedPreferences.getBoolean(key, true)
            }
            context.getString(R.string.key_preference_full_brightness) -> {
                fullScreenBrightness = sharedPreferences.getBoolean(key, false)
            }
        }
        preferenceListener?.onPreferenceChanged()
    }
}