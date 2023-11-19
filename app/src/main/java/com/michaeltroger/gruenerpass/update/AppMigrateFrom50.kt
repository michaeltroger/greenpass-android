package com.michaeltroger.gruenerpass.update

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.michaeltroger.gruenerpass.locator.Locator

class AppMigrateFrom50 {

    fun invoke(context: Context) {
        val oldPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val newPreferences = Locator.encryptedSharedPreferences
        newPreferences.edit {
            oldPreferences.all.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Set<*> -> putStringSet(key, value.map { it.toString() }.toSet())
                    else -> throw IllegalStateException("unsupported type for shared preferences migration")
                }
            }
        }
        oldPreferences.edit {
            clear()
        }
    }
}
