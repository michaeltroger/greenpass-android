package com.michaeltroger.gruenerpass.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppMigrator(ctx: Context) {

    private val context = ctx.applicationContext

    private val appVersionCode = longPreferencesKey("app_version_code")
    private val appVersionFlow: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[appVersionCode] ?: 0
        }

    private val previousVersionCode: Long by lazy {
        runBlocking {
            appVersionFlow.first()
        }
    }

    private val currentVersionCode: Long by lazy {
        context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
    }

    fun performMigration(previousVersion: Long = previousVersionCode, currentVersion: Long = currentVersionCode) {
        if (previousVersion < 7) {
            AppMigrateFrom6().invoke(context)
        }

        persistCurrentAppVersionCode()
    }

    private fun persistCurrentAppVersionCode() = GlobalScope.launch(Dispatchers.IO) {
        context.dataStore.edit { settings ->
            settings[appVersionCode] = currentVersionCode
        }
    }

}