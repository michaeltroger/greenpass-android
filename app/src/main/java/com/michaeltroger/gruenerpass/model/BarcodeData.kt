package com.michaeltroger.gruenerpass.model

sealed class BarcodeData {
    abstract val text: String?
    data class QrCode(
        override val text: String?
    ) : BarcodeData()
    data class AztecCode(
        override val text: String?
    ) : BarcodeData()
}