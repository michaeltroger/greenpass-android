package com.michaeltroger.gruenerpass

sealed class ViewState {
    object Empty : ViewState()
    object Certificate : ViewState()
    object Error : ViewState()
}