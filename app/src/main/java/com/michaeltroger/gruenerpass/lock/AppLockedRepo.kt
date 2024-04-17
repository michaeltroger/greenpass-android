package com.michaeltroger.gruenerpass.lock

import android.content.Context
import android.content.SharedPreferences
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.settings.getBooleanFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

interface AppLockedRepo {
    fun isAppLocked(): Flow<Boolean>
    suspend fun lockApp()
    suspend fun unlockApp()
}

class AppLockedRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    sharedPrefs: SharedPreferences,
) : AppLockedRepo {
    
    private val _isLocked: MutableStateFlow<Boolean> = MutableStateFlow(
        true
    )
    private val isLocked: StateFlow<Boolean> = _isLocked

    private val shouldAuthenticate =
        sharedPrefs.getBooleanFlow(context.getString(R.string.key_preference_biometric))

    override fun isAppLocked() = shouldAuthenticate.combine(isLocked) { shouldAuthenticate: Boolean, isLocked: Boolean ->
        shouldAuthenticate && isLocked
    }.distinctUntilChanged()

    override suspend fun lockApp() {
        _isLocked.value = true
    }
    override suspend fun unlockApp() {
        _isLocked.value = false
    }
}