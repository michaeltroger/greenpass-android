package com.michaeltroger.gruenerpass.settings

import android.content.SharedPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

fun SharedPreferences.getBooleanFlow(
    prefKey: String,
    defaultValue: Boolean,
): Flow<Boolean> = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (prefKey == key) {
            trySend(getBoolean(key, defaultValue))
        }
    }
    registerOnSharedPreferenceChangeListener(listener)
    send(getBoolean(prefKey, defaultValue))
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}.buffer(Channel.UNLIMITED)

fun <T>SharedPreferences.getFlow(
    prefKey: String,
    defaultValue: String,
    transform: (String) -> T,
): Flow<T> = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (prefKey == key) {
            trySend(transform(getString(prefKey, defaultValue) ?: defaultValue))
        }
    }
    registerOnSharedPreferenceChangeListener(listener)
    send(transform(getString(prefKey, defaultValue) ?: defaultValue))
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}.buffer(Channel.UNLIMITED)
