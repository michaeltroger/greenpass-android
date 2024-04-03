package com.michaeltroger.gruenerpass.logging.di;

import com.michaeltroger.gruenerpass.logging.Logger
import com.michaeltroger.gruenerpass.logging.LoggerImpl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LoggerModule {

    @Binds
    abstract fun bindLogger(
        loggerImpl: LoggerImpl
    ): Logger
}
