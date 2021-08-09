package com.michaeltroger.gruenerpass.states

sealed class ViewState {
    data class Certificate(val documents: List<com.michaeltroger.gruenerpass.db.Certificate>) : ViewState()
    object Loading : ViewState()
}