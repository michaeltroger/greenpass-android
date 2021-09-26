package com.michaeltroger.gruenerpass.states

import com.michaeltroger.gruenerpass.db.Certificate

sealed class ViewState {
    data class Normal(val documents: List<Certificate>, val offerAppSettings: Boolean) : ViewState()
    object Loading : ViewState()
    object Locked : ViewState()
}