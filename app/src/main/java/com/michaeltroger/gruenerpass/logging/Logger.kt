package com.michaeltroger.gruenerpass.logging

interface Logger {
    fun logDebug(value: String?)
    fun logError(value: String?)
}
