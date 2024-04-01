package com.michaeltroger.gruenerpass.settings

import android.content.Context
import android.content.SharedPreferences
import com.michaeltroger.gruenerpass.R

interface PreferenceChangeListener {
    fun refreshUi()
}

interface PreferenceObserver {
    fun searchForBarcode(): Boolean
    fun shouldAuthenticate(): Boolean
    fun addDocumentsInFront(): Boolean
    fun showOnLockedScreen(): Boolean
    fun init(preferenceChangeListener: PreferenceChangeListener)
    fun onDestroy()
}

class PreferenceObserverImpl(
    private val context: Context,
    private val preferenceManager: SharedPreferences
        = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context),
): PreferenceObserver, SharedPreferences.OnSharedPreferenceChangeListener {

    private var preferenceChangeListener: PreferenceChangeListener? = null
    private var searchForBarcode: Boolean = true
    private var shouldAuthenticate = false
    private var addDocumentsFront: Boolean = false
    private var showOnLockedScreen: Boolean = false

    override fun init(preferenceChangeListener: PreferenceChangeListener) {
        this.preferenceChangeListener = preferenceChangeListener
        preferenceManager.registerOnSharedPreferenceChangeListener(this)
        shouldAuthenticate = preferenceManager.getBoolean(
            context.getString(R.string.key_preference_biometric),
            false
        )
        searchForBarcode = preferenceManager.getBoolean(
            context.getString(R.string.key_preference_search_for_barcode),
            true
        )
        addDocumentsFront = preferenceManager.getBoolean(
            context.getString(R.string.key_preference_add_documents_front),
            false
        )
        showOnLockedScreen = preferenceManager.getBoolean(
            context.getString(R.string.key_preference_show_on_locked_screen),
            false
        )
    }

    override fun searchForBarcode() = searchForBarcode
    override fun shouldAuthenticate() = shouldAuthenticate
    override fun addDocumentsInFront() = addDocumentsFront
    override fun showOnLockedScreen() = showOnLockedScreen

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            context.getString(R.string.key_preference_biometric) -> {
                shouldAuthenticate = sharedPreferences.getBoolean(key, false)
                preferenceChangeListener?.refreshUi()
            }
            context.getString(R.string.key_preference_search_for_barcode) -> {
                searchForBarcode = sharedPreferences.getBoolean(key, true)
                preferenceChangeListener?.refreshUi()
            }
            context.getString(R.string.key_preference_add_documents_front) -> {
                addDocumentsFront = sharedPreferences.getBoolean(key, false)
            }
            context.getString(R.string.key_preference_show_on_locked_screen) -> {
                showOnLockedScreen = sharedPreferences.getBoolean(key, false)
                preferenceChangeListener?.refreshUi()
            }
        }
    }

    override fun onDestroy() {
        preferenceManager.unregisterOnSharedPreferenceChangeListener(this)
        preferenceChangeListener = null
    }
}
