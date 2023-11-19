package com.michaeltroger.gruenerpass.settings

import android.content.Context
import android.content.SharedPreferences
import com.michaeltroger.gruenerpass.R

interface PreferenceListener {
    fun refreshUi()
}

interface PreferenceManager {
    fun searchForQrCode(): Boolean
    fun shouldAuthenticate(): Boolean
    fun addDocumentsInFront(): Boolean
    fun init(preferenceListener: PreferenceListener)
}

class PreferenceManagerImpl(
    private val context: Context,
    private val preferenceManager: SharedPreferences
        = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context),
): PreferenceManager, SharedPreferences.OnSharedPreferenceChangeListener {

    private var preferenceListener: PreferenceListener? = null
    private var searchForQrCode: Boolean = true
    private var shouldAuthenticate = false
    private var addDocumentsFront: Boolean = false

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
        addDocumentsFront = preferenceManager.getBoolean(
            context.getString(R.string.key_preference_add_documents_front),
            false
        )
    }

    override fun searchForQrCode() = searchForQrCode
    override fun shouldAuthenticate() = shouldAuthenticate
    override fun addDocumentsInFront() = addDocumentsFront

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            context.getString(R.string.key_preference_biometric) -> {
                shouldAuthenticate = sharedPreferences.getBoolean(key, false)
            }
            context.getString(R.string.key_preference_search_for_qr_code) -> {
                searchForQrCode = sharedPreferences.getBoolean(key, true)
                preferenceListener?.refreshUi()
            }
            context.getString(R.string.key_preference_add_documents_front) -> {
                addDocumentsFront = sharedPreferences.getBoolean(key, false)
            }
        }
    }
}
