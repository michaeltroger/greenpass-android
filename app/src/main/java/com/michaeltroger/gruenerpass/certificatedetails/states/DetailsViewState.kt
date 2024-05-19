package com.michaeltroger.gruenerpass.certificatedetails.states

import com.michaeltroger.gruenerpass.db.Certificate

sealed class DetailsViewState {

    data object Initial : DetailsViewState()

    data object Deleted : DetailsViewState()

    data class Normal(
        val document: Certificate,
        val searchBarcode: Boolean,
        val extraHardBarcodeSearch: Boolean,
    ) : DetailsViewState()
}
