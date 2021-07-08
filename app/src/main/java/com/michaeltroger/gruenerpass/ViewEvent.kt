package com.michaeltroger.gruenerpass

sealed class ViewEvent {
    object ShowPasswordDialog : ViewEvent()
    object ShowReplaceDialog : ViewEvent()
    object ShowDeleteDialog : ViewEvent()
    object CloseAllDialogs : ViewEvent()
}