package com.michaeltroger.gruenerpass.states

sealed class ViewState {
    data class Certificate(val documents: List<Pair<String, String>>) : ViewState()
    object Loading : ViewState()
}