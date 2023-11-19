package com.michaeltroger.gruenerpass.update

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.michaeltroger.gruenerpass.extensions.getPackageInfo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppMigrator(ctx: Context) {

    private val context = ctx.applicationContext

    private val appVersionCode = longPreferencesKey("app_version_code")
    private val appVersionCodeFlow: Flow<Long> = context.dataStore.data
        .map { settings ->
            settings[appVersionCode] ?: 0
        }

    private val previousVersionCode: Long by lazy {
        runBlocking {
            appVersionCodeFlow.first()
        }
    }

    private val currentVersionCode: Long by lazy {
        PackageInfoCompat.getLongVersionCode(context.getPackageInfo())
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun persistCurrentAppVersionCode() = GlobalScope.launch(Dispatchers.IO) {
        context.dataStore.edit { settings ->
            settings[appVersionCode] = currentVersionCode
        }
    }

    @Suppress("MagicNumber")
    fun performMigration(previousVersion: Long = previousVersionCode, currentVersion: Long = currentVersionCode) {
        if (previousVersion == currentVersion) {
            return
        }

        if (previousVersion < 7) {
            AppMigrateFrom6().invoke(context)
        }
        if (previousVersion < 28) {
            AppMigrateFrom27().invoke(context)
        }
        if (previousVersion < 51) {
            AppMigrateFrom50().invoke(context)
        }

        persistCurrentAppVersionCode()
    }

}
