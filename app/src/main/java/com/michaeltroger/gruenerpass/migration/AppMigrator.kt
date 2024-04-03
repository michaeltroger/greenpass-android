package com.michaeltroger.gruenerpass.migration

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.michaeltroger.gruenerpass.extensions.getPackageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppMigrator @Inject constructor(@ApplicationContext ctx: Context) {

    private val context = ctx.applicationContext

    @Inject
    lateinit var from6: AppMigrateFrom6
    @Inject
    lateinit var from27: AppMigrateFrom27

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
            from6()
        }
        if (previousVersion < 28) {
            from27()
        }

        persistCurrentAppVersionCode()
    }

}
