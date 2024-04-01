package com.michaeltroger.gruenerpass.states

import com.michaeltroger.gruenerpass.db.Certificate

private const val SCROLL_TO_DELAY_MS = 1000L

sealed class ViewEvent {
    data object ShowPasswordDialog : ViewEvent()
    data object CloseAllDialogs : ViewEvent()
    data object ErrorParsingFile : ViewEvent()
    data class ScrollToLastCertificate(val delayMs: Long = SCROLL_TO_DELAY_MS) : ViewEvent()
    data class ScrollToFirstCertificate(val delayMs: Long = SCROLL_TO_DELAY_MS) : ViewEvent()
    data class ExportAll(val list: List<Certificate>) : ViewEvent()
    data class ExportFiltered(val list: List<Certificate>) : ViewEvent()
    data object DeleteAll : ViewEvent()
    data class DeleteFiltered(val documentCount: Int) : ViewEvent()
}
