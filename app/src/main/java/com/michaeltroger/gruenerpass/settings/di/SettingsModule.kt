package com.michaeltroger.gruenerpass.settings.di;

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
}
