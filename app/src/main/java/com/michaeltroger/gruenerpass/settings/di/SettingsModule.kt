package com.michaeltroger.gruenerpass.settings.di;

import android.content.Context
import android.content.SharedPreferences
import com.michaeltroger.gruenerpass.settings.PreferenceObserver
import com.michaeltroger.gruenerpass.settings.PreferenceObserverImpl

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsBindModule {

    @Binds
    abstract fun bindPreferenceObserver(
        preferenceObserverImpl: PreferenceObserverImpl
    ): PreferenceObserver
}

@Module
@InstallIn(SingletonComponent::class)
object SettingsProvideModule {

    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
}
