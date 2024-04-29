package com.michaeltroger.gruenerpass.certificates.states

import com.michaeltroger.gruenerpass.db.Certificate

sealed class ViewEvent {
    data object AddFile : ViewEvent()
    data object ShowParsingFileError : ViewEvent()
    data object ShowPasswordDialog : ViewEvent()
    data object CloseAllDialogs : ViewEvent()
    data class GoToCertificate(
        val position: Int,
        val id: String,
        val isNewDocument: Boolean,
    ) : ViewEvent()
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
