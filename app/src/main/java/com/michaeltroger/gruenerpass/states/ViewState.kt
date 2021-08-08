package com.michaeltroger.gruenerpass.states

sealed class ViewState {
    data class Certificate(val documentCount: Int) : ViewState()
    object Loading : ViewState()
}