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
    data object ShowWarningDialog : ViewEvent()
    data object ShowSettings : ViewEvent()
    data object ShowMore : ViewEvent()
    data object AddFile : ViewEvent()

    data class DeleteFiltered(val documentCount: Int) : ViewEvent()
    data class ChangeDocumentOrder(val originalOrder: List<Certificate>) : ViewEvent()
    data class ShowDoYouWantToDeleteDialog(val id: String) : ViewEvent()
    data class ChangeDocumentName(val id: String, val name: String) : ViewEvent()
    data class Share(val certificate: Certificate) : ViewEvent()
}
