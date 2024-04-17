package com.michaeltroger.gruenerpass.certificate.states

import com.michaeltroger.gruenerpass.db.Certificate

private const val SCROLL_TO_DELAY_MS = 1000L

sealed class ViewEvent {
    data object AddFile : ViewEvent()
    data object ShowParsingFileError : ViewEvent()
    data object ShowPasswordDialog : ViewEvent()
    data object CloseAllDialogs : ViewEvent()
    data class ScrollToLastCertificate(val delayMs: Long = SCROLL_TO_DELAY_MS) : ViewEvent()
    data class ScrollToFirstCertificate(val delayMs: Long = SCROLL_TO_DELAY_MS) : ViewEvent()
    data class Share(val certificate: Certificate) : ViewEvent()
    data class ShareMultiple(val list: List<Certificate>) : ViewEvent()
    data class ShowDeleteDialog(val id: String) : ViewEvent()
    data object ShowDeleteAllDialog : ViewEvent()
    data class ShowDeleteFilteredDialog(val documentCountToBeDeleted: Int) : ViewEvent()
    data object ShowWarningDialog : ViewEvent()
    data object ShowSettingsScreen : ViewEvent()
    data object ShowMoreScreen : ViewEvent()
    data class ShowChangeDocumentOrderDialog(val originalOrder: List<Certificate>) : ViewEvent()
    data class ShowChangeDocumentNameDialog(val id: String, val originalName: String) : ViewEvent()
}
