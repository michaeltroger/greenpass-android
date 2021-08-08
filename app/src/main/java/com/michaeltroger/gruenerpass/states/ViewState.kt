package com.michaeltroger.gruenerpass.states

import java.util.*

sealed class ViewState {
    data class Certificate(val documents: SortedMap<String, String>) : ViewState()
    object Loading : ViewState()
}