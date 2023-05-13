package com.michaeltroger.gruenerpass.states

import com.michaeltroger.gruenerpass.db.Certificate

sealed class ViewState {
    abstract val fullBrightness: Boolean
    abstract val showLockAppButton: Boolean

    data class Initial(
        override val fullBrightness: Boolean
    ) : ViewState() {
        override val showLockAppButton = false
    }

    data class Empty(
        override val fullBrightness: Boolean,
        override val showLockAppButton: Boolean,
    ) : ViewState()

    data class Normal(
        val documents: List<Certificate>,
        val searchQrCode: Boolean,
        override val fullBrightness: Boolean,
        override val showLockAppButton: Boolean,
    ) : ViewState()

    data class Locked(
        override val fullBrightness: Boolean
    ) : ViewState() {
        override val showLockAppButton = false
    }
}
