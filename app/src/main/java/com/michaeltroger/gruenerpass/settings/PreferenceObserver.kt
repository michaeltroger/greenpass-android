package com.michaeltroger.gruenerpass.settings

import android.content.Context
import android.content.SharedPreferences
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.locator.Locator

interface PreferenceChangeListener {
    fun refreshUi()
}

interface PreferenceObserver {
    fun searchForQrCode(): Boolean
    fun shouldAuthenticate(): Boolean
    fun addDocumentsInFront(): Boolean
    fun init(preferenceChangeListener: PreferenceChangeListener)
    fun onDestroy()
}

class PreferenceObserverImpl(
    private val context: Context,
    private val preferenceDataStore: EncryptedPreferenceDataStore
    = Locator.encryptedPreferenceDataStore,
): PreferenceObserver, SharedPreferences.OnSharedPreferenceChangeListener {

    private var preferenceChangeListener: PreferenceChangeListener? = null
    private var searchForQrCode: Boolean = true
    private var shouldAuthenticate = false
    private var addDocumentsFront: Boolean = false

    override fun init(preferenceChangeListener: PreferenceChangeListener) {
        this.preferenceChangeListener = preferenceChangeListener
        preferenceDataStore.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        shouldAuthenticate = preferenceDataStore.getBoolean(
            context.getString(R.string.key_preference_biometric),
            false
        )
        searchForQrCode = preferenceDataStore.getBoolean(
            context.getString(R.string.key_preference_search_for_qr_code),
            true
        )
        addDocumentsFront = preferenceDataStore.getBoolean(
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
                preferenceChangeListener?.refreshUi()
            }
            context.getString(R.string.key_preference_add_documents_front) -> {
                addDocumentsFront = sharedPreferences.getBoolean(key, false)
            }
        }
    }

    override fun onDestroy() {
        preferenceDataStore.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        preferenceChangeListener = null
    }
}
