package com.michaeltroger.gruenerpass.logger.logging.di

import com.michaeltroger.gruenerpass.logger.logging.Logger
import com.michaeltroger.gruenerpass.logger.logging.LoggerImpl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
public abstract class LoggerModule {

    @Binds
    internal abstract fun bindLogger(
        loggerImpl: LoggerImpl
    ): Logger
}
