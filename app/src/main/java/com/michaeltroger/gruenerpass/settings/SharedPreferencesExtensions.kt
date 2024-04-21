package com.michaeltroger.gruenerpass.settings

import android.content.SharedPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

fun SharedPreferences.getBooleanFlow(
    prefKey: String,
    defaultValue: Boolean,
) = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (prefKey == key) {
            trySend(getBoolean(key, defaultValue))
        }
    }
    registerOnSharedPreferenceChangeListener(listener)
    send(getBoolean(prefKey, defaultValue))
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}.buffer(Channel.UNLIMITED)
