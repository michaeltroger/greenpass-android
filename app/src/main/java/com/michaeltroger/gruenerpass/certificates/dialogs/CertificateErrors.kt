package com.michaeltroger.gruenerpass.certificates.dialogs

import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.michaeltroger.gruenerpass.R
import javax.inject.Inject

interface CertificateErrors {
    fun showFileErrorSnackbar(view: View)
}

class CertificateErrorsImpl @Inject constructor() : CertificateErrors {
    override fun showFileErrorSnackbar(view: View) {
        Snackbar.make(view, R.string.error_reading_pdf, Snackbar.LENGTH_LONG).show()
    }
}
