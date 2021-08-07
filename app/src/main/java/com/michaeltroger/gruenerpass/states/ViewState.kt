package com.michaeltroger.gruenerpass.states

sealed class ViewState {
    object Empty : ViewState()
    data class Certificate(val hasQrCode: Boolean) : ViewState()
    object Loading : ViewState()
}