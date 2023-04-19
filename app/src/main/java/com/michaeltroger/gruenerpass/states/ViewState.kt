package com.michaeltroger.gruenerpass.states

import com.michaeltroger.gruenerpass.db.Certificate

sealed class ViewState {
    abstract val fullBrightness: Boolean

    data class Loading(override val fullBrightness: Boolean) : ViewState()
    data class Normal(val documents: List<Certificate>, val searchQrCode: Boolean, override val fullBrightness: Boolean) : ViewState()
    data class Locked(override val fullBrightness: Boolean) : ViewState()
}