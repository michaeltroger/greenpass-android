package com.michaeltroger.gruenerpass.logger.di

import com.michaeltroger.gruenerpass.logger.Logger
import com.michaeltroger.gruenerpass.logger.LoggerDebugImpl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
public abstract class LoggerDebugModule {

    @Binds
    internal abstract fun bindLogger(
        loggerImpl: LoggerDebugImpl
    ): Logger
}
