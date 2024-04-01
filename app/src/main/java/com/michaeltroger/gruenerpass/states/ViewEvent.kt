package com.michaeltroger.gruenerpass.states

import com.michaeltroger.gruenerpass.db.Certificate

sealed class ViewEvent {
    data object ShowPasswordDialog : ViewEvent()
    data object CloseAllDialogs : ViewEvent()
    data object ErrorParsingFile : ViewEvent()
    data object ScrollToLastCertificate : ViewEvent()
    data object ScrollToFirstCertificate : ViewEvent()
    data class ExportAll(val list: List<Certificate>) : ViewEvent()
    data class ExportFiltered(val list: List<Certificate>) : ViewEvent()
    data object DeleteAll : ViewEvent()
    data class DeleteFiltered(val documentCount: Int) : ViewEvent()
}
