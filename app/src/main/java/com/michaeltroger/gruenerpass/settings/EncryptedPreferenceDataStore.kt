package com.michaeltroger.gruenerpass.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedPreferenceDataStore(
    context: Context,
    val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
) : PreferenceDataStore() {

    override fun putString(key: String, value: String?) {
        sharedPreferences.edit {
            putString(key, value)
        }
    }

    override fun putStringSet(key: String, values: Set<String>?) {
        sharedPreferences.edit {
            putStringSet(key, values)
        }
    }

    override fun putInt(key: String, value: Int) {
        sharedPreferences.edit {
            putInt(key, value)
        }
    }

    override fun putLong(key: String, value: Long) {
        sharedPreferences.edit {
            putLong(key, value)
        }
    }

    override fun putFloat(key: String, value: Float) {
        sharedPreferences.edit {
            putFloat(key, value)
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit {
            putBoolean(key, value)
        }
    }

    override fun getString(key: String, defValue: String?): String? {
        return sharedPreferences.getString(key, defValue)
    }

    override fun getStringSet(key: String, defValues: Set<String?>?): Set<String>? {
        return sharedPreferences.getStringSet(key, defValues)
    }

    override fun getInt(key: String, defValue: Int): Int {
        return sharedPreferences.getInt(key, defValue)
    }

    override fun getLong(key: String, defValue: Long): Long {
        return sharedPreferences.getLong(key, defValue)
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return sharedPreferences.getFloat(key, defValue)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defValue)
    }
}