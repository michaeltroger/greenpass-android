package com.michaeltroger.gruenerpass.authentication

import androidx.biometric.BiometricPrompt

class BiometricAuthenticationCallback(
    private val onSuccess: () -> Unit,
    private val onError: () -> Unit
) : BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        onSuccess()
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        onError()
    }
}
